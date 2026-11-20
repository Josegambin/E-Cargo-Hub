(() => {
    'use strict';

    const API_ORIGIN = `${location.protocol}//${location.hostname}:8081`;
    const KEYCLOAK_ORIGIN = `${location.protocol}//${location.hostname}:8180`;
    const REALM = 'e-cargo-hub';
    const CLIENT_ID = 'ecargo-dashboard';
    const OAUTH_KEY = 'ecargo.test-console.pkce';
    const NULL_CHAR = String.fromCharCode(0);
    const state = {
        accessToken: null,
        refreshToken: null,
        idToken: null,
        expiresAt: 0,
        refreshTimer: null,
        openApi: null,
        operations: [],
        vehicles: [],
        routes: [],
        map: null,
        markers: new Map(),
        polylines: new Map(),
        socket: null,
        stompBuffer: '',
        stompHeartbeat: null
    };

    const byId = id => document.getElementById(id);

    function showFeedback(message, isError = false) {
        const element = byId('feedback');
        element.textContent = message;
        element.classList.toggle('error', isError);
    }

    function apiBase() {
        return API_ORIGIN;
    }

    function identityBase() {
        return `${KEYCLOAK_ORIGIN}/realms/${REALM}/protocol/openid-connect`;
    }

    function redirectUri() {
        const path = location.pathname.replace(/%[0-9a-f]{2}/gi, escape => escape.toUpperCase());
        return `${location.origin}${path}`;
    }

    function updateLinks() {
        byId('keycloak-users-link').href = `${KEYCLOAK_ORIGIN}/admin/master/console/#/${encodeURIComponent(REALM)}/users`;
        byId('swagger-link').href = `${apiBase()}/swagger-ui.html`;
    }

    async function apiRequest(path, options = {}) {
        const headers = { Accept: 'application/json', ...(options.headers || {}) };
        if (state.accessToken && options.auth !== false) {
            headers.Authorization = `Bearer ${state.accessToken}`;
        }
        const request = { method: options.method || 'GET', headers };
        if (options.body !== undefined) {
            headers['Content-Type'] = 'application/json';
            request.body = JSON.stringify(options.body);
        }
        const controller = new AbortController();
        const timeout = setTimeout(() => controller.abort(), 15000);
        let response;
        try {
            response = await fetch(`${apiBase()}${path}`, { ...request, signal: controller.signal });
        } catch (error) {
            if (error.name === 'AbortError') throw new Error('El backend no respondió en 15 segundos.');
            throw new Error(`No se pudo conectar con el backend: ${error.message}`);
        } finally {
            clearTimeout(timeout);
        }
        const text = await response.text();
        let data = null;
        if (text) {
            try {
                data = JSON.parse(text);
            } catch {
                data = text;
            }
        }
        if (!response.ok) {
            const detail = typeof data === 'string'
                ? data
                : data?.message || data?.detail || data?.error || response.statusText;
            const error = new Error(`HTTP ${response.status}: ${detail}`);
            error.status = response.status;
            error.response = data;
            throw error;
        }
        return { data, status: response.status };
    }

    function setApiStatus(label, kind) {
        const element = byId('api-status');
        element.textContent = label;
        element.className = `status-pill status-${kind}`;
    }

    function base64Url(bytes) {
        let binary = '';
        bytes.forEach(byte => binary += String.fromCharCode(byte));
        return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
    }

    function randomString() {
        const bytes = crypto.getRandomValues(new Uint8Array(32));
        return base64Url(bytes);
    }

    async function sha256(value) {
        const bytes = new TextEncoder().encode(value);
        return base64Url(new Uint8Array(await crypto.subtle.digest('SHA-256', bytes)));
    }

    async function beginLogin() {
        try {
            if (!crypto?.subtle) throw new Error('PKCE requiere HTTPS o localhost con Web Crypto habilitado.');
            const endpoint = identityBase();
            const verifier = randomString();
            const stateValue = randomString();
            const callbackUri = redirectUri();
            sessionStorage.setItem(OAUTH_KEY, JSON.stringify({ verifier, state: stateValue, redirectUri: callbackUri }));
            const url = new URL(`${endpoint}/auth`);
            url.search = new URLSearchParams({
                client_id: CLIENT_ID,
                redirect_uri: callbackUri,
                response_type: 'code',
                scope: 'openid profile email',
                code_challenge_method: 'S256',
                code_challenge: await sha256(verifier),
                state: stateValue
            }).toString();
            location.assign(url);
        } catch (error) {
            showFeedback(error.message, true);
        }
    }

    async function exchangeCode(code, oauthState) {
        let pending;
        try {
            pending = JSON.parse(sessionStorage.getItem(OAUTH_KEY) || 'null');
        } catch {
            pending = null;
        }
        sessionStorage.removeItem(OAUTH_KEY);
        if (!pending || pending.state !== oauthState) {
            throw new Error('La respuesta de inicio de sesión no coincide. Vuelve a iniciar sesión.');
        }
        const form = new URLSearchParams({
            grant_type: 'authorization_code',
            client_id: CLIENT_ID,
            code,
            redirect_uri: pending.redirectUri,
            code_verifier: pending.verifier
        });
        const response = await fetch(`${identityBase()}/token`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: form.toString()
        });
        const tokens = await response.json();
        if (!response.ok || !tokens.access_token) {
            throw new Error(tokens.error_description || tokens.error || `Keycloak respondió HTTP ${response.status}.`);
        }
        acceptTokens(tokens);
        history.replaceState({}, document.title, location.pathname);
        showFeedback('Sesión iniciada correctamente con Keycloak.');
    }

    function decodeJwt(token) {
        const part = token.split('.')[1];
        if (!part) throw new Error('Keycloak devolvió un JWT no válido.');
        const encoded = part.replace(/-/g, '+').replace(/_/g, '/');
        const binary = atob(encoded.padEnd(Math.ceil(encoded.length / 4) * 4, '='));
        const bytes = Uint8Array.from(binary, char => char.charCodeAt(0));
        return JSON.parse(new TextDecoder().decode(bytes));
    }

    function acceptTokens(tokens) {
        state.accessToken = tokens.access_token;
        state.refreshToken = tokens.refresh_token || null;
        state.idToken = tokens.id_token || state.idToken;
        state.expiresAt = Date.now() + Number(tokens.expires_in || 300) * 1000;
        renderSession();
        loadFleet();
        scheduleRefresh();
        connectSocket();
    }

    function scheduleRefresh() {
        clearTimeout(state.refreshTimer);
        if (!state.refreshToken) return;
        const delay = Math.max(5000, state.expiresAt - Date.now() - 45000);
        state.refreshTimer = setTimeout(refreshAccessToken, delay);
    }

    async function refreshAccessToken() {
        if (!state.refreshToken) return;
        try {
            const form = new URLSearchParams({
                grant_type: 'refresh_token',
                client_id: CLIENT_ID,
                refresh_token: state.refreshToken
            });
            const response = await fetch(`${identityBase()}/token`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: form.toString()
            });
            const tokens = await response.json();
            if (!response.ok || !tokens.access_token) {
                throw new Error(tokens.error_description || 'La sesión Keycloak ha caducado.');
            }
            acceptTokens(tokens);
        } catch (error) {
            clearSession();
            showFeedback(`La sesión terminó: ${error.message}. Inicia sesión de nuevo.`, true);
        }
    }

    async function logout() {
        const endpoint = identityBase();
        const logoutUrl = new URL(`${endpoint}/logout`);
        logoutUrl.search = new URLSearchParams({
            client_id: CLIENT_ID,
            post_logout_redirect_uri: redirectUri(),
            ...(state.idToken ? { id_token_hint: state.idToken } : {})
        }).toString();
        clearSession();
        location.assign(logoutUrl);
    }

    function clearSession() {
        clearTimeout(state.refreshTimer);
        state.accessToken = null;
        state.refreshToken = null;
        state.idToken = null;
        state.expiresAt = 0;
        disconnectSocket();
        renderSession();
        byId('user-name').textContent = '—';
        byId('vehicle-count').textContent = '—';
        byId('active-count').textContent = '—';
        byId('route-count').textContent = '—';
        byId('vehicle-rows').innerHTML = '<tr><td colspan="8" class="empty-row">Inicia sesión y actualiza la flota.</td></tr>';
        setApiStatus('Esperando inicio de sesión', 'neutral');
    }

    function renderSession() {
        const button = byId('login-button');
        const logoutButton = byId('logout-button');
        const rolesElement = byId('role-badges');
        rolesElement.replaceChildren();
        if (!state.accessToken) {
            byId('session-label').textContent = 'Sin iniciar sesión';
            byId('session-summary').querySelector('.status-dot').classList.remove('online');
            byId('user-name').textContent = '—';
            setApiStatus('Esperando inicio de sesión', 'neutral');
            button.hidden = false;
            byId('copy-token-button').hidden = true;
            logoutButton.hidden = true;
            document.querySelectorAll('.admin-only').forEach(control => control.disabled = true);
            return;
        }
        let claims;
        try {
            claims = decodeJwt(state.accessToken);
        } catch (error) {
            clearSession();
            showFeedback(error.message, true);
            return;
        }
        const username = claims.preferred_username || claims.name || claims.sub || 'Usuario';
        const roles = claims.realm_access?.roles || [];
        byId('session-label').textContent = username;
        byId('user-name').textContent = username;
        byId('session-summary').querySelector('.status-dot').classList.add('online');
        setApiStatus('Validando token…', 'neutral');
        roles.filter(role => ['ADMIN', 'VIEWER'].includes(role)).forEach(role => {
            const badge = document.createElement('span');
            badge.className = 'role-badge';
            badge.textContent = role;
            rolesElement.appendChild(badge);
        });
        button.hidden = true;
        byId('copy-token-button').hidden = false;
        logoutButton.hidden = false;
        const isAdmin = roles.includes('ADMIN');
        document.querySelectorAll('.admin-only').forEach(control => control.disabled = !isAdmin);
    }

    async function processCallback() {
        const params = new URLSearchParams(location.search);
        if (params.has('error')) {
            history.replaceState({}, document.title, location.pathname);
            sessionStorage.removeItem(OAUTH_KEY);
            throw new Error(params.get('error_description') || params.get('error'));
        }
        if (params.has('code')) {
            await exchangeCode(params.get('code'), params.get('state'));
        }
    }

    function initMap() {
        if (!window.L) return;
        try {
            state.map = L.map('map').setView([38.07, -1.0], 8);
            L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                maxZoom: 19,
                attribution: '© OpenStreetMap contributors'
            }).addTo(state.map);
            setTimeout(() => state.map?.invalidateSize(), 100);
        } catch (error) {
            showFeedback(`No se pudo cargar el mapa: ${error.message}`, true);
        }
    }

    async function loadFleet() {
        if (!state.accessToken) {
            setApiStatus('Esperando inicio de sesión', 'neutral');
            showFeedback('Paso 1: inicia sesión con tu usuario de Keycloak.');
            return;
        }
        try {
            setApiStatus('Validando token…', 'neutral');
            showFeedback('Paso 2: validando el access token en el backend…');
            const vehiclesResponse = await apiRequest('/api/v1/vehicles');
            setApiStatus('Token aceptado', 'ok');
            const routesResponse = await apiRequest('/api/v1/routes');
            state.vehicles = Array.isArray(vehiclesResponse.data) ? vehiclesResponse.data : [];
            state.routes = Array.isArray(routesResponse.data) ? routesResponse.data : [];
            renderSelectors();
            const statuses = await Promise.allSettled(state.vehicles.map(async vehicle => {
                const result = await apiRequest(`/api/v1/vehicles/${encodeURIComponent(vehicle.id)}/status`);
                return { ...vehicle, ...result.data };
            }));
            state.vehicles = state.vehicles.map((vehicle, index) => {
                const result = statuses[index];
                return result.status === 'fulfilled' ? { ...vehicle, ...result.value } : vehicle;
            });
            renderFleet();
            connectSocket();
            const failedStatuses = statuses.filter(result => result.status === 'rejected').length;
            showFeedback(`Backend conectado: ${state.vehicles.length} vehículos y ${state.routes.length} rutas.${failedStatuses ? ` No se pudo cargar el estado de ${failedStatuses} vehículos.` : ''}`);
        } catch (error) {
            const status = error.status === 401
                ? 'Token rechazado'
                : error.status === 403 ? 'Sin permisos' : 'Backend no disponible';
            setApiStatus(status, 'error');
            showFeedback(`No se pudo validar la conexión con el token: ${error.message}`, true);
        }
    }

    function renderSelectors() {
        const vehicleSelects = [byId('command-vehicle'), byId('device-vehicle')];
        vehicleSelects.forEach(select => {
            const previous = select.value;
            select.replaceChildren();
            state.vehicles.forEach(vehicle => {
                const option = document.createElement('option');
                option.value = vehicle.id;
                option.textContent = `${vehicle.name || `Vehículo ${vehicle.id}`} (#${vehicle.id})`;
                select.appendChild(option);
            });
            if (previous && [...select.options].some(option => option.value === previous)) select.value = previous;
        });
        const routeSelect = byId('command-route');
        const previousRoute = routeSelect.value;
        routeSelect.replaceChildren();
        const placeholder = document.createElement('option');
        placeholder.value = '';
        placeholder.textContent = state.routes.length ? 'Selecciona ruta' : 'No hay rutas';
        routeSelect.appendChild(placeholder);
        state.routes.forEach(route => {
            const option = document.createElement('option');
            option.value = route.id;
            option.textContent = `${route.name || `Ruta ${route.id}`} (#${route.id})`;
            routeSelect.appendChild(option);
        });
        if (previousRoute && [...routeSelect.options].some(option => option.value === previousRoute)) routeSelect.value = previousRoute;
    }

    function renderFleet() {
        byId('vehicle-count').textContent = String(state.vehicles.length);
        byId('route-count').textContent = String(state.routes.length);
        const active = state.vehicles.filter(vehicle => ['RUNNING', 'EN_RUTA'].includes(String(vehicle.status || '').toUpperCase())).length;
        byId('active-count').textContent = String(active);
        const tbody = byId('vehicle-rows');
        if (!state.vehicles.length) {
            tbody.innerHTML = '<tr><td colspan="8" class="empty-row">No hay vehículos registrados.</td></tr>';
            return;
        }
        const rows = state.vehicles.map(vehicle => {
            const row = document.createElement('tr');
            const values = [
                vehicle.id,
                vehicle.name || `Vehículo ${vehicle.id}`,
                formatNumber(vehicle.latitude, 5),
                formatNumber(vehicle.longitude, 5),
                `${formatNumber(vehicle.speedKmh, 1)} km/h`,
                `${vehicle.progress ?? 0}%`,
                vehicle.status || 'IDLE',
                formatTime(vehicle.updatedAt || vehicle.timestamp)
            ];
            values.forEach((value, index) => {
                const cell = document.createElement('td');
                cell.textContent = value;
                if (index === 6) {
                    const badge = document.createElement('span');
                    badge.className = `vehicle-state ${String(value).toLowerCase()}`;
                    badge.textContent = value;
                    cell.replaceChildren(badge);
                }
                row.appendChild(cell);
            });
            return row;
        });
        tbody.replaceChildren(...rows);
        state.vehicles.forEach(updateMap);
    }

    function formatNumber(value, decimals) {
        const number = Number(value);
        return value !== null && value !== undefined && Number.isFinite(number) ? number.toFixed(decimals) : '—';
    }

    function formatTime(value) {
        if (!value) return '—';
        const date = typeof value === 'number' ? new Date(value) : new Date(value);
        return Number.isNaN(date.getTime()) ? '—' : date.toLocaleTimeString();
    }

    function updateMap(vehicle) {
        if (!state.map || vehicle.latitude == null || vehicle.longitude == null) return;
        const position = [Number(vehicle.latitude), Number(vehicle.longitude)];
        if (!position.every(Number.isFinite)) return;
        const id = String(vehicle.vehicleId || vehicle.id);
        if (!state.markers.has(id)) {
            state.markers.set(id, L.marker(position).addTo(state.map).bindPopup(
                `${vehicle.name || `Vehículo ${id}`} (#${id})`
            ));
        } else {
            state.markers.get(id).setLatLng(position);
        }
        if (!state.polylines.has(id)) {
            const color = Number(id) % 2 ? '#2457d6' : '#d04747';
            state.polylines.set(id, L.polyline([], { color, weight: 3, opacity: .7 }).addTo(state.map));
        }
        state.polylines.get(id).addLatLng(position);
        const points = [...state.markers.values()].map(marker => marker.getLatLng());
        if (points.length) state.map.fitBounds(points, { padding: [35, 35], maxZoom: 12 });
    }

    async function sendVehicleCommand(command) {
        const vehicleId = byId('command-vehicle').value;
        if (!vehicleId) {
            showFeedback('No hay ningún vehículo seleccionado.', true);
            return;
        }
        const body = { command };
        if (command === 'START') {
            const routeId = byId('command-route').value;
            if (!routeId) {
                showFeedback('START requiere seleccionar una ruta existente.', true);
                return;
            }
            body.routeId = Number(routeId);
        }
        try {
            const result = await apiRequest(`/api/v1/vehicles/${encodeURIComponent(vehicleId)}/commands`, {
                method: 'POST',
                body
            });
            showResponse(result.data, result.status, 'Comando aceptado');
            await loadFleet();
        } catch (error) {
            showFeedback(error.message, true);
        }
    }

    function showResponse(data, status, label = 'Respuesta') {
        byId('api-response').textContent = typeof data === 'string' ? data : JSON.stringify(data, null, 2);
        byId('response-meta').textContent = `${label} · HTTP ${status}`;
    }

    async function loadCommandHistory() {
        const vehicleId = byId('command-vehicle').value;
        if (!vehicleId) return showFeedback('Selecciona un vehículo.', true);
        try {
            const result = await apiRequest(`/api/v1/vehicles/${encodeURIComponent(vehicleId)}/commands`);
            showResponse(result.data, result.status, 'Historial de comandos');
        } catch (error) {
            showFeedback(error.message, true);
        }
    }

    async function createRoute(event) {
        event.preventDefault();
        const form = new FormData(event.currentTarget);
        const body = {
            name: String(form.get('name')).trim(),
            origin: { latitude: Number(form.get('originLat')), longitude: Number(form.get('originLon')) },
            destination: { latitude: Number(form.get('destinationLat')), longitude: Number(form.get('destinationLon')) }
        };
        try {
            const result = await apiRequest('/api/v1/routes', { method: 'POST', body });
            showResponse(result.data, result.status, 'Ruta creada');
            await loadFleet();
        } catch (error) {
            showFeedback(error.message, true);
        }
    }

    async function createEnrollment() {
        const vehicleId = byId('device-vehicle').value;
        if (!vehicleId) return showFeedback('Selecciona un vehículo.', true);
        try {
            const result = await apiRequest(`/api/v1/vehicles/${encodeURIComponent(vehicleId)}/device-enrollments`, { method: 'POST' });
            byId('device-result').textContent = JSON.stringify(result.data, null, 2);
            showFeedback('Código temporal creado. Compártelo solo con el dispositivo que vas a vincular.');
        } catch (error) {
            byId('device-result').textContent = error.message;
            showFeedback(error.message, true);
        }
    }

    async function listDevices() {
        const vehicleId = byId('device-vehicle').value;
        if (!vehicleId) return showFeedback('Selecciona un vehículo.', true);
        try {
            const result = await apiRequest(`/api/v1/vehicles/${encodeURIComponent(vehicleId)}/devices`);
            byId('device-result').textContent = JSON.stringify(result.data, null, 2);
        } catch (error) {
            byId('device-result').textContent = error.message;
            showFeedback(error.message, true);
        }
    }

    async function claimDevice() {
        const pairingCode = byId('pairing-code').value.trim();
        const deviceName = byId('claim-device-name').value.trim();
        if (!pairingCode || !deviceName) return showFeedback('Introduce el código temporal y el nombre del dispositivo.', true);
        try {
            const result = await apiRequest('/api/v1/device-enrollments/claim', {
                method: 'POST',
                auth: false,
                body: { pairingCode, deviceName }
            });
            byId('device-token').value = result.data.deviceToken || '';
            byId('device-result').textContent = JSON.stringify(result.data, null, 2);
            showFeedback('Dispositivo vinculado. El token mostrado se entrega una sola vez; guárdalo de forma segura en el dispositivo.');
        } catch (error) {
            byId('device-result').textContent = error.message;
            showFeedback(error.message, true);
        }
    }

    async function loadDeviceState() {
        const token = byId('device-token').value.trim();
        if (!token) return showFeedback('Introduce el token propio del dispositivo.', true);
        try {
            const result = await apiRequest('/api/v1/device/state', {
                auth: false,
                headers: { 'X-Vehicle-Device-Token': token }
            });
            byId('device-result').textContent = JSON.stringify(result.data, null, 2);
            showFeedback('Estado de dispositivo consultado correctamente.');
        } catch (error) {
            byId('device-result').textContent = error.message;
            showFeedback(error.message, true);
        }
    }

    async function copyAccessToken() {
        if (!state.accessToken) return;
        try {
            await navigator.clipboard.writeText(state.accessToken);
            showFeedback('Access token copiado al portapapeles. Es temporal; no lo compartas ni lo guardes en archivos.');
        } catch (error) {
            showFeedback(`No se pudo copiar el token: ${error.message}`, true);
        }
    }

    async function loadOpenApi() {
        try {
            const response = await apiRequest('/v3/api-docs', { auth: false });
            state.openApi = response.data;
            state.operations = [];
            Object.entries(state.openApi.paths || {}).forEach(([path, pathItem]) => {
                Object.entries(pathItem).forEach(([method, operation]) => {
                    if (!['get', 'post', 'put', 'delete'].includes(method.toLowerCase())) return;
                    state.operations.push({ path, method: method.toUpperCase(), operation, pathItem });
                });
            });
            state.operations.sort((a, b) => {
                const tagA = a.operation.tags?.[0] || 'Otros';
                const tagB = b.operation.tags?.[0] || 'Otros';
                return tagA.localeCompare(tagB) || a.path.localeCompare(b.path) || a.method.localeCompare(b.method);
            });
            const select = byId('operation-select');
            select.replaceChildren(new Option('Selecciona una operación…', ''));
            state.operations.forEach((item, index) => {
                const option = document.createElement('option');
                const tag = item.operation.tags?.[0] || 'Otros';
                option.value = String(index);
                option.textContent = `${tag} · ${item.method} ${item.path} — ${item.operation.summary || item.operation.operationId || ''}`;
                select.appendChild(option);
            });
            showFeedback(`OpenAPI cargado: ${state.operations.length} operaciones disponibles.`);
        } catch (error) {
            showFeedback(`No se pudo cargar OpenAPI: ${error.message}`, true);
        }
    }

    function dereference(schema, seen = new Set()) {
        if (!schema?.$ref) return schema || {};
        if (seen.has(schema.$ref)) return {};
        seen.add(schema.$ref);
        const pointer = schema.$ref.replace(/^#\//, '').split('/');
        let resolved = state.openApi;
        pointer.forEach(part => { resolved = resolved?.[part.replace(/~1/g, '/').replace(/~0/g, '~')]; });
        return dereference(resolved, seen);
    }

    function sampleValue(schema, seen = new Set()) {
        const resolved = dereference(schema, seen);
        if (resolved.example !== undefined) return resolved.example;
        if (resolved.default !== undefined) return resolved.default;
        if (Array.isArray(resolved.enum) && resolved.enum.length) return resolved.enum[0];
        if (resolved.const !== undefined) return resolved.const;
        if (resolved.oneOf?.length) return sampleValue(resolved.oneOf[0], new Set(seen));
        if (resolved.anyOf?.length) return sampleValue(resolved.anyOf[0], new Set(seen));
        if (resolved.allOf?.length) {
            return resolved.allOf.reduce((result, part) => Object.assign(result, sampleValue(part, new Set(seen))), {});
        }
        if (resolved.type === 'array') return [];
        if (resolved.type === 'object' || resolved.properties) {
            const value = {};
            Object.entries(resolved.properties || {}).forEach(([key, property]) => {
                value[key] = sampleValue(property, new Set(seen));
            });
            return value;
        }
        if (resolved.type === 'integer' || resolved.type === 'number') return 1;
        if (resolved.type === 'boolean') return false;
        if (resolved.format === 'date-time') return new Date().toISOString();
        if (resolved.format === 'date') return new Date().toISOString().slice(0, 10);
        return resolved.type === 'string' ? 'string' : null;
    }

    function resolveParameter(parameter) {
        return parameter?.$ref ? dereference(parameter) : parameter;
    }

    function selectedOperation() {
        const index = Number(byId('operation-select').value);
        return Number.isInteger(index) ? state.operations[index] : null;
    }

    function renderOperation() {
        const selected = selectedOperation();
        const fields = byId('parameter-fields');
        fields.replaceChildren();
        byId('request-method').value = selected?.method || '';
        byId('request-path').value = selected?.path || '';
        byId('operation-hint').textContent = selected?.operation.description || '';
        const parameters = [
            ...(selected?.pathItem.parameters || []),
            ...(selected?.operation.parameters || [])
        ].map(resolveParameter);
        parameters.forEach(parameter => {
            const label = document.createElement('label');
            label.textContent = `${parameter.name} (${parameter.in})`;
            const input = document.createElement('input');
            input.dataset.parameterName = parameter.name;
            input.dataset.parameterIn = parameter.in;
            input.required = Boolean(parameter.required);
            const schema = dereference(parameter.schema || {});
            const sample = parameter.example ?? schema.example ?? schema.default;
            input.value = sample === undefined ? (parameter.in === 'path' ? '1' : '') : String(sample);
            if (schema.type === 'integer' || schema.type === 'number') input.type = 'number';
            input.title = parameter.description || '';
            label.appendChild(input);
            fields.appendChild(label);
        });
        const requestBody = selected?.operation.requestBody;
        const bodySchema = requestBody?.content?.['application/json']?.schema;
        byId('body-label').hidden = !bodySchema;
        if (bodySchema) {
            const example = requestBody.content['application/json'].example;
            byId('request-body').value = JSON.stringify(example ?? sampleValue(bodySchema), null, 2);
        } else {
            byId('request-body').value = '';
        }
        byId('send-request').disabled = !selected || !state.accessToken;
    }

    async function sendExploredRequest(event) {
        event.preventDefault();
        const selected = selectedOperation();
        if (!selected) return;
        const parameters = [...byId('parameter-fields').querySelectorAll('input')];
        let path = byId('request-path').value.trim();
        const query = new URLSearchParams();
        for (const input of parameters) {
            if (!input.value && input.required) {
                showFeedback(`El parámetro ${input.dataset.parameterName} es obligatorio.`, true);
                return;
            }
            if (input.dataset.parameterIn === 'path') {
                path = path.replace(`{${input.dataset.parameterName}}`, encodeURIComponent(input.value));
            } else if (input.dataset.parameterIn === 'query' && input.value) {
                query.set(input.dataset.parameterName, input.value);
            }
        }
        if (/\{[^}]+\}/.test(path)) {
            showFeedback('Completa todos los parámetros de ruta.', true);
            return;
        }
        if (query.size) path += `${path.includes('?') ? '&' : '?'}${query.toString()}`;
        const options = { method: selected.method };
        if (!byId('body-label').hidden) {
            try {
                options.body = JSON.parse(byId('request-body').value);
            } catch {
                showFeedback('El cuerpo debe ser JSON válido.', true);
                return;
            }
        }
        if (selected.method === 'DELETE' && !confirm(`Vas a ejecutar ${selected.method} ${path}. ¿Continuar?`)) return;
        const startedAt = performance.now();
        byId('send-request').disabled = true;
        byId('response-meta').textContent = 'Enviando…';
        try {
            const result = await apiRequest(path, options);
            showResponse(result.data, result.status, `${selected.method} ${path}`);
            byId('response-meta').textContent += ` · ${Math.round(performance.now() - startedAt)} ms`;
            showFeedback('Petición completada.');
            if (selected.path === '/api/v1/vehicles' || selected.path === '/api/v1/routes') await loadFleet();
        } catch (error) {
            byId('api-response').textContent = typeof error.response === 'string'
                ? error.response
                : error.response ? JSON.stringify(error.response, null, 2) : error.message;
            byId('response-meta').textContent = `${selected.method} ${path}${error.status ? ` · HTTP ${error.status}` : ''}`;
            showFeedback(error.message, true);
        } finally {
            byId('send-request').disabled = !state.accessToken;
        }
    }

    function setLiveStatus(label, kind) {
        const element = byId('live-status');
        element.textContent = label;
        element.className = `status-pill status-${kind}`;
    }

    function disconnectSocket() {
        clearInterval(state.stompHeartbeat);
        state.stompHeartbeat = null;
        if (state.socket) {
            state.socket.onclose = null;
            state.socket.close();
            state.socket = null;
        }
        state.stompBuffer = '';
        setLiveStatus('Desconectado', 'neutral');
    }

    function connectSocket() {
        if (!state.accessToken || !state.vehicles.length || !window.WebSocket) return;
        disconnectSocket();
        let wsUrl;
        try {
            wsUrl = apiBase().replace(/^http/i, 'ws') + '/ws-native';
        } catch (error) {
            showFeedback(error.message, true);
            return;
        }
        try {
            const socket = new WebSocket(wsUrl);
            state.socket = socket;
            socket.onopen = () => {
                socket.send([
                    'CONNECT',
                    'accept-version:1.2,1.1,1.0',
                    'heart-beat:10000,10000',
                    `Authorization:Bearer ${state.accessToken}`,
                    '',
                    NULL_CHAR
                ].join('\n'));
            };
            socket.onmessage = event => {
                state.stompBuffer += String(event.data);
                let end;
                while ((end = state.stompBuffer.indexOf(NULL_CHAR)) !== -1) {
                    handleStompFrame(state.stompBuffer.slice(0, end));
                    state.stompBuffer = state.stompBuffer.slice(end + 1);
                }
            };
            socket.onerror = () => setLiveStatus('Error', 'error');
            socket.onclose = () => {
                if (state.socket === socket) {
                    clearInterval(state.stompHeartbeat);
                    setLiveStatus('Desconectado', 'neutral');
                }
            };
        } catch (error) {
            setLiveStatus('Error', 'error');
            showFeedback(`No se pudo abrir WebSocket: ${error.message}`, true);
        }
    }

    function handleStompFrame(frame) {
        const normalized = frame.replace(/\r/g, '');
        if (normalized.startsWith('CONNECTED')) {
            setLiveStatus('En vivo', 'ok');
            state.vehicles.forEach(vehicle => {
                state.socket?.send(`SUBSCRIBE\nid:vehicle-${vehicle.id}\ndestination:/topic/vehicle-status/${vehicle.id}\nack:auto\n\n${NULL_CHAR}`);
            });
            state.socket?.send(`SUBSCRIBE\nid:fleet\ndestination:/topic/fleet-status\nack:auto\n\n${NULL_CHAR}`);
            state.stompHeartbeat = setInterval(() => {
                if (state.socket?.readyState === WebSocket.OPEN) state.socket.send('\n');
            }, 10000);
            return;
        }
        if (normalized.startsWith('ERROR')) {
            setLiveStatus('No autorizado', 'error');
            showFeedback('El backend rechazó la conexión WebSocket. Confirma los roles y el token.', true);
            return;
        }
        if (!normalized.startsWith('MESSAGE')) return;
        const bodyStart = normalized.indexOf('\n\n');
        if (bodyStart < 0) return;
        try {
            const payload = JSON.parse(normalized.slice(bodyStart + 2));
            if (payload.eventType) appendFleetEvent(payload);
            const vehicleId = Number(payload.vehicleId);
            if (!Number.isFinite(vehicleId)) return;
            const current = state.vehicles.find(vehicle => Number(vehicle.id) === vehicleId) || {};
            const updated = { ...current, ...payload, id: vehicleId };
            state.vehicles = state.vehicles.map(vehicle => Number(vehicle.id) === vehicleId ? updated : vehicle);
            renderFleet();
        } catch (error) {
            showFeedback(`No se pudo procesar un mensaje STOMP: ${error.message}`, true);
        }
    }

    function appendFleetEvent(event) {
        const container = byId('fleet-events');
        const empty = container.querySelector('.empty-row');
        if (empty) empty.remove();
        const row = document.createElement('div');
        row.className = 'event-item';
        const time = document.createElement('span');
        time.className = 'event-time';
        time.textContent = formatTime(event.timestamp);
        const type = document.createElement('span');
        type.className = 'event-name';
        type.textContent = String(event.eventType || 'EVENT');
        const message = document.createElement('span');
        message.textContent = String(event.message || '');
        row.append(time, type, message);
        container.prepend(row);
        while (container.children.length > 50) container.lastElementChild.remove();
    }

    function clearEvents() {
        byId('fleet-events').innerHTML = '<p class="empty-row">La vista de eventos se ha limpiado; no se han borrado datos del backend.</p>';
    }

    async function runAction(action) {
        try {
            await action();
        } catch (error) {
            showFeedback(error.message, true);
        }
    }

    function bindEvents() {
        byId('login-button').addEventListener('click', beginLogin);
        byId('logout-button').addEventListener('click', logout);
        byId('copy-token-button').addEventListener('click', () => runAction(copyAccessToken));
        byId('refresh-fleet').addEventListener('click', () => runAction(loadFleet));
        byId('route-form').addEventListener('submit', event => runAction(() => createRoute(event)));
        byId('create-enrollment').addEventListener('click', () => runAction(createEnrollment));
        byId('list-devices').addEventListener('click', () => runAction(listDevices));
        byId('claim-device').addEventListener('click', () => runAction(claimDevice));
        byId('check-device-state').addEventListener('click', () => runAction(loadDeviceState));
        byId('command-history').addEventListener('click', () => runAction(loadCommandHistory));
        byId('clear-events').addEventListener('click', clearEvents);
        byId('load-openapi').addEventListener('click', () => runAction(loadOpenApi));
        byId('operation-select').addEventListener('change', renderOperation);
        byId('api-form').addEventListener('submit', event => runAction(() => sendExploredRequest(event)));
        document.querySelectorAll('[data-command]').forEach(button => {
            button.addEventListener('click', () => runAction(() => sendVehicleCommand(button.dataset.command)));
        });
    }

    async function initialize() {
        updateLinks();
        bindEvents();
        initMap();
        renderSession();
        try {
            await processCallback();
        } catch (error) {
            setApiStatus('Error de inicio de sesión', 'error');
            showFeedback(`Inicio de sesión fallido: ${error.message}`, true);
        }
    }

    document.addEventListener('DOMContentLoaded', initialize);
})();
