# API Reference — E-Cargo Hub

Referencia completa de la API REST y WebSocket del sistema **E-Cargo Hub**.

---

## 1. Convenciones generales

### 1.1. Base URL

| Entorno | URL |
|---|---|
| Desarrollo | `http://localhost:8081` |
| Producción | `https://api.ecargohub.example` |

### 1.2. Prefijo de la API

Todos los endpoints REST comienzan con `/api/v1`.

### 1.3. Formato de datos

- **Peticiones**: `application/json`
- **Respuestas**: `application/json`
- **Fechas**: ISO-8601 (`2026-09-30T14:30:00Z`)
- **UUIDs**: formato estándar (`47a1c31b-f21b-433e-ba85-ed2e38db4a5f`)

### 1.4. Códigos de estado HTTP

| Código | Significado |
|---|---|
| `200 OK` | Petición exitosa |
| `201 Created` | Recurso creado |
| `204 No Content` | Eliminación exitosa |
| `400 Bad Request` | Datos inválidos (validación) |
| `401 Unauthorized` | No autenticado |
| `403 Forbidden` | Sin permisos |
| `404 Not Found` | Recurso no encontrado |
| `409 Conflict` | Conflicto de estado (ej. vehículo ya en ruta) |
| `500 Internal Server Error` | Error del servidor |

### 1.5. Formato de errores

Todos los errores devuelven el mismo formato JSON:

```json
{
  "timestamp": "2026-09-30T14:30:00.123Z",
  "status": 404,
  "error": "NOT_FOUND",
  "message": "Vehicle not found: 9999",
  "path": "/api/v1/vehicles/9999"
}
```

### 1.6. Autenticación

Las rutas de control requieren `X-Control-Token`, configurado mediante `ECARGO_CONTROL_API_TOKEN`.
Las pantallas vinculadas usan una credencial independiente, limitada a consultar su propio estado.
No incluyas ninguno de estos secretos en el panel administrativo ni en código público.

### Modelos y entidades

Los cuerpos REST están formados por DTO, no por entidades JPA. Las entidades son internas del backend y no deben consumirse desde Angular. El contrato OpenAPI mantenido en [`openapi/openapi.yaml`](../openapi/openapi.yaml) es la fuente única para los DTO HTTP y genera los modelos y servicios Angular mediante `npm run generate:api`.

La referencia OpenAPI contiene el listado completo de endpoints, campos, tipos, validaciones y respuestas. Esta página resume los flujos principales.

---

## 2. Endpoints REST

### 2.1. Vehículos

#### `GET /api/v1/vehicles`

Devuelve la lista completa de vehículos.

**Parámetros:** ninguno.

**Respuesta 200:**

```json
[
  {
    "id": 1,
    "name": "TRUCK-001",
    "type": "TRUCK",
    "maxSpeed": 120,
    "status": "IDLE",
    "currentPosition": null
  },
  {
    "id": 2,
    "name": "VAN-001",
    "type": "VAN",
    "maxSpeed": 100,
    "status": "IDLE",
    "currentPosition": null
  }
]
```

**Ejemplo:**

```bash
curl http://localhost:8081/api/v1/vehicles
```

---

#### `GET /api/v1/vehicles/{vehicleId}`

Devuelve un vehículo por su ID.

**Parámetros de ruta:**
- `id` (Long) — ID del vehículo.

**Respuesta 200:**

```json
{
  "id": 1,
  "name": "TRUCK-001",
  "type": "TRUCK",
  "maxSpeed": 120,
  "status": "IDLE",
  "currentPosition": null
}
```

**Respuesta 404:**

```json
{
  "timestamp": "2026-09-30T14:30:00Z",
  "status": 404,
  "error": "NOT_FOUND",
  "message": "Vehicle not found: 9999",
  "path": "/api/v1/vehicles/9999"
}
```

---

#### `POST /api/v1/vehicles`

Crea un vehículo nuevo.

Para iniciar, crea primero una ruta con `POST /api/v1/routes`, guarda el `id` devuelto y úsalo
en el comando. Por ejemplo:

```json
{
  "name": "Cox - Murcia",
  "origin": { "latitude": 38.015, "longitude": -0.746 },
  "destination": { "latitude": 37.983, "longitude": -1.129 }
}
```

El backend obtiene origen y destino de esa ruta y usa los límites `max_speed` de GraphHopper.
Cuando GraphHopper devuelve `null` para un tramo, se aplica únicamente a ese tramo la velocidad
máxima configurada para el vehículo; los valores numéricos recibidos se conservan y los datos
malformados o las rutas sin geometría válida siguen rechazándose.

**Body START:**

```json
{
  "name": "TRUCK-003",
  "type": "TRUCK",
  "maxSpeed": 120,
  "energyType": "FUEL",
  "energyCapacity": 300,
  "energyConsumptionPer100": 28
}
```

**Respuesta 201:**

```json
{
  "id": 3,
  "name": "TRUCK-003",
  "type": "TRUCK",
  "maxSpeed": 120,
  "energyType": "FUEL",
  "energyCapacity": 300,
  "energyConsumptionPer100": 28,
  "energyPercent": 100,
  "status": "IDLE",
  "currentPosition": null
}
```

---

#### `PUT /api/v1/vehicles/{vehicleId}`

Actualiza un vehículo existente.

**Body:**

```json
{
  "name": "TRUCK-003",
  "type": "TRUCK",
  "maxSpeed": 110,
  "energyType": "FUEL",
  "energyCapacity": 300,
  "energyConsumptionPer100": 28,
  "remainingEnergy": 300
}
```

Para repostar o recargar desde el backend, establece `remainingEnergy` entre `0` y
`energyCapacity` en la misma petición de actualización.

**Respuesta 200:**

```json
{
  "id": 3,
  "name": "TRUCK-003",
  "type": "TRUCK",
  "maxSpeed": 110,
  "energyType": "FUEL",
  "energyCapacity": 300,
  "energyConsumptionPer100": 28,
  "energyPercent": 100,
  "status": "IDLE",
  "currentPosition": null
}
```

---

#### `DELETE /api/v1/vehicles/{vehicleId}`

Elimina un vehículo.

**Respuesta 204:** sin body.

---

### 2.2. Estado del vehículo

#### `GET /api/v1/vehicles/{vehicleId}/status`

Devuelve el estado actual del vehículo (última posición conocida).

**Respuesta 200:**

```json
{
  "vehicleId": 1,
  "latitude": 38.1408,
  "longitude": -0.8844,
  "progress": 45,
  "speedKmh": 87.5,
  "energyPercent": 72.4,
  "energyType": "FUEL",
  "gearPosition": "D4",
  "status": "EN_RUTA",
  "updatedAt": "2026-09-30T14:30:00Z"
}
```

**Si el vehículo no tiene estado guardado:**

```json
{
  "vehicleId": 1,
  "latitude": null,
  "longitude": null,
  "progress": 0,
  "speedKmh": 0.0,
  "energyPercent": 100,
  "energyType": "FUEL",
  "gearPosition": "P",
  "status": "IDLE",
  "updatedAt": null
}
```

---

### 2.3. Vincular pantallas de solo lectura

El teléfono no puede configurar el vehículo ni emitir comandos. El operador inicia la vinculación
desde el backend; el código dura diez minutos y solo puede reclamarse una vez.

```http
POST /api/v1/vehicles/1/device-enrollments
X-Control-Token: <ECARGO_CONTROL_API_TOKEN>
```

La respuesta `201` incluye `pairingUrl` y `qrSvg`. Muestra el SVG en un lugar seguro y escanéalo
con la cámara del teléfono; para AAOS introduce el `pairingCode` en **Vincular pantalla**. La PWA
`/vehicle-display` reclama el código y guarda una credencial
que solo puede consultar `GET /api/v1/device/state` usando `X-Vehicle-Device-Token`. Nunca envíes
`X-Control-Token` a la PWA ni al dispositivo del vehículo.

El enrolamiento devuelve una credencial una sola vez; el backend almacena únicamente su hash.
El QR también contiene un secreto temporal: no lo compartas y genera otro si no se usa antes de
su caducidad.

Configura `ECARGO_MOBILE_APP_URL` como la URL HTTPS accesible desde el teléfono (por ejemplo,
`https://fleet.example/vehicle-display`) y configura `environment.prod.ts` con la URL HTTPS
pública del backend antes de compilar la PWA para producción.

### 2.4. Telemetría

#### `GET /api/v1/vehicles/{vehicleId}/telemetry`

Devuelve el histórico completo de telemetría del vehículo.

**Query params opcionales:**

| Param | Tipo | Descripción |
|---|---|---|
| `from` | ISO-8601 | Fecha inicial inclusive; se puede enviar sin `to` |
| `to` | ISO-8601 | Fecha final inclusive; se puede enviar sin `from` |

**Respuesta 200:**

```json
[
  {
    "id": 1,
    "vehicleId": 1,
    "latitude": 38.1408,
    "longitude": -0.8844,
    "progress": 5,
    "speedKmh": 4.5,
    "status": "EN_RUTA",
    "recordedAt": "2026-09-30T14:30:00Z"
  },
  {
    "id": 2,
    "vehicleId": 1,
    "latitude": 38.1395,
    "longitude": -0.8912,
    "progress": 10,
    "speedKmh": 15.2,
    "status": "EN_RUTA",
    "recordedAt": "2026-09-30T14:30:01Z"
  }
]
```

**Ejemplo con filtro de fechas:**

```bash
curl "http://localhost:8081/api/v1/vehicles/1/telemetry?from=2026-09-30T00:00:00Z&to=2026-09-30T23:59:59Z"
```

---

#### `GET /api/v1/vehicles/{vehicleId}/telemetry/latest`

Devuelve los últimos N puntos de telemetría.

**Query params:**

| Param | Tipo | Default | Descripción |
|---|---|---|---|
| `limit` | int | 50 | Número de puntos, entre 1 y 500 |

**Respuesta 200:** igual que el endpoint anterior.

---

#### `DELETE /api/v1/vehicles/{vehicleId}/telemetry`

Borra todo el histórico de telemetría del vehículo.

**Respuesta 200:**

```json
{
  "vehicleId": 1,
  "deleted": 21
}
```

---

### 2.5. Integración Android Automotive OS (AAOS)

La app puente `android-aaos-bridge` detecta y lee las propiedades que el VHAL y los permisos de AAOS ponen a disposición, y envía cada instantánea al backend. Se guardan las propiedades como un mapa flexible para no perder sensores que varíen entre vehículos/emuladores.

#### `POST /api/v1/vehicles/{vehicleId}/aaos/telemetry`

```json
{
  "source": "android-automotive",
  "capturedAt": "2026-10-06T17:30:00Z",
  "values": {
    "PERF_VEHICLE_SPEED": 12.5,
    "SEAT_BELT_BUCKLED@1": true,
    "FUEL_LEVEL": 68.0
  },
  "availableProperties": ["PERF_VEHICLE_SPEED", "SEAT_BELT_BUCKLED", "FUEL_LEVEL"],
  "readErrors": []
}
```

`values` admite hasta 200 propiedades por instantánea. Los nombres y la disponibilidad dependen del VHAL del vehículo y de los permisos concedidos; el ejemplo no garantiza que cada vehículo exponga esos sensores.

#### `GET /api/v1/vehicles/{vehicleId}/aaos/telemetry/latest?limit=1`

Devuelve hasta 500 instantáneas recientes, incluida la fecha de captura, recepción, propiedades leídas, propiedades disponibles y errores de lectura.

#### `POST /api/v1/vehicles/{vehicleId}/aaos/notifications`

Publica una notificación para la app del vehículo, que se suscribe a `/topic/vehicles/{vehicleId}/aaos/notifications`:

```json
{
  "title": "Cinturón",
  "message": "Comprueba el cinturón del conductor.",
  "severity": "WARNING"
}
```

#### `POST /api/v1/vehicles/{vehicleId}/aaos/simulated-commands`

Envía un comando al vehículo simulado asignado, no a los actuadores físicos del coche. Requiere la cabecera `X-Control-Token` configurada mediante `ECARGO_CONTROL_API_TOKEN` con al menos 32 bytes UTF-8. Consulta los tipos y las restricciones en la sección de comandos.

### 2.6. Comandos

#### `POST /api/v1/vehicles/{vehicleId}/commands`

Envía un comando a un vehículo. Requiere la cabecera `X-Control-Token` configurada mediante `ECARGO_CONTROL_API_TOKEN` con al menos 32 bytes UTF-8. La misma protección se aplica a `POST /api/v1/fleet/commands` y a las rutas de inicio, pausa, reanudación y parada de simulaciones. Si el token no está configurado o es demasiado corto, el control responde `503`; si falta o no coincide, responde `401`. No incluyas este secreto en clientes de navegador o apps del vehículo.

**Parámetros de ruta:**
- `vehicleId` (Long) — ID del vehículo destinatario.

**Body:**

```json
{
  "command": "START",
  "routeId": 1
}
```

**Campos del body:**

| Campo | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| `command` | Enum | Sí | `START`, `PAUSE`, `RESUME`, `STOP`, `SET_SPEED`, `INCREASE_SPEED`, `DECREASE_SPEED`, `EMERGENCY_BRAKE`, `SET_IGNITION`, `SET_HEADLIGHTS`, `SET_HIGH_BEAMS`, `SET_HAZARD_LIGHTS`, `SET_TURN_SIGNAL`, `SET_WIPERS`, `SET_CLIMATE_POWER`, `SET_CLIMATE_TEMPERATURE`, `SET_CLIMATE_FAN_SPEED`, `SET_CLIMATE_MODE` |
| `value` | Double | Según el comando | Velocidad en km/h; controles binarios `0/1`; `SET_TURN_SIGNAL`: `0` apagado, `1` izquierda y `2` derecha; temperatura: `16-30` °C; ventilador: entero `0-7`; climatizador: modo `0` AUTO, `1` COOL, `2` HEAT |
| `routeId` | Long | Solo para `START` | ID de una ruta previamente creada; obligatorio al iniciar |
| `originLat`, `originLon`, `destLat`, `destLon` | Double | No | Se derivan de la ruta seleccionada; no se deben enviar manualmente |

La velocidad simula aceleración y frenado graduales y no supera el menor valor entre el máximo
configurado del vehículo y el límite de la vía para cada segmento. La energía se consume según
la distancia y el consumo configurado; al cruzar el 10% se genera una alerta `LOW_ENERGY`.

**Respuesta 200:**

```json
{
  "id": 42,
  "commandId": "47a1c31b-f21b-433e-ba85-ed2e38db4a5f",
  "vehicleId": 1,
  "command": "START",
  "value": null,
  "originLat": 38.1408,
  "originLon": -0.8844,
  "destLat": 37.9922,
  "destLon": -1.1307,
  "createdAt": "2026-09-30T14:30:00Z"
}
```

**Errores:**

| Código | Causa |
|---|---|
| `400` | Comando nulo o coordenadas fuera de rango |
| `404` | Vehículo no encontrado |
| `409` | Comando no válido para el estado actual del vehículo |

**Ejemplos por comando:**

**START:**
```json
{ "command": "START", "value": null }
```

**PAUSE:**
```json
{ "command": "PAUSE", "value": null }
```

**RESUME:**
```json
{ "command": "RESUME", "value": null }
```

**STOP:**
```json
{ "command": "STOP", "value": null }
```

**Control de velocidad de la simulación** (respetando el límite `maxSpeed` configurado):
```json
{ "command": "SET_SPEED", "value": 60 }
```
`INCREASE_SPEED` y `DECREASE_SPEED` aceptan el tamaño del paso en km/h (`10` por defecto). `SET_SPEED` con valor `0` detiene temporalmente el movimiento; un nuevo valor positivo lo reanuda.

**Frenada de emergencia simulada** (detiene la simulación de inmediato):
```json
{ "command": "EMERGENCY_BRAKE", "value": null }
```

**Controles simulados del vehículo** (solo cambian el estado del vehículo virtual):
```json
{ "command": "SET_IGNITION", "value": 1 }
{ "command": "SET_HEADLIGHTS", "value": 1 }
{ "command": "SET_HIGH_BEAMS", "value": 1 }
{ "command": "SET_HAZARD_LIGHTS", "value": 1 }
{ "command": "SET_TURN_SIGNAL", "value": 2 }
{ "command": "SET_WIPERS", "value": 1 }
{ "command": "SET_CLIMATE_POWER", "value": 1 }
{ "command": "SET_CLIMATE_TEMPERATURE", "value": 20 }
{ "command": "SET_CLIMATE_FAN_SPEED", "value": 4 }
{ "command": "SET_CLIMATE_MODE", "value": 1 }
```
El modo acepta `0` AUTO, `1` COOL y `2` HEAT; la temperatura debe estar entre `16` y `30` °C y el ventilador es un nivel entero de `0` a `7`.
Apagar el encendido detiene la simulación en marcha; para iniciarla de nuevo hay que volver a encenderlo. El estado actual, incluido `climateOn`, `climateTemperatureC`, `climateFanSpeed` y `climateMode`, se consulta en `GET /api/v1/vehicles/{vehicleId}/aaos/control-state` y se publica en `/topic/vehicles/{vehicleId}/control-state`. Este estado vive en memoria y vuelve a los valores iniciales al reiniciar el backend.

#### `POST /api/v1/fleet/commands`

Envía una acción a todos los vehículos registrados. Cada vehículo se valida individualmente; una petición puede tener éxito parcial. La respuesta es `202` si se aceptó para toda la flota o `207` si algunos vehículos la rechazaron, e indica el motivo para cada uno.

```json
{ "command": "START" }
```

#### Canales WebSocket de AAOS

- `/topic/vehicles/{vehicleId}/aaos/telemetry` — instantáneas recién recibidas.
- `/topic/vehicles/{vehicleId}/aaos/notifications` — avisos para la pantalla del vehículo.
- `/topic/vehicles/{vehicleId}/commands` — órdenes dirigidas a la simulación y etiquetadas `SIMULATOR_ONLY`.
- `/topic/vehicles/{vehicleId}/control-state` — estado actualizado de los controles simulados.

**START con coordenadas personalizadas:**
```json
{
  "command": "START",
  "value": null,
  "originLat": 37.9922,
  "originLon": -1.1307,
  "destLat": 37.6056,
  "destLon": -0.9912
}
```

---

#### `GET /api/v1/vehicles/{vehicleId}/commands`

Devuelve el historial de comandos del vehículo.

**Respuesta 200:**

```json
[
  {
    "id": 42,
    "commandId": "47a1c31b-f21b-433e-ba85-ed2e38db4a5f",
    "vehicleId": 1,
    "command": "START",
    "value": null,
    "originLat": 38.1408,
    "originLon": -0.8844,
    "destLat": 37.9922,
    "destLon": -1.1307,
    "createdAt": "2026-09-30T14:30:00Z"
  }
]
```

---

### 2.6. Resto de recursos REST

Todos estos endpoints están descritos con sus DTO, parámetros, validaciones y errores en el contrato OpenAPI.

| Recurso | Operaciones |
|---|---|
| Almacenes | `GET/POST /api/v1/warehouses`, `GET/PUT/DELETE /api/v1/warehouses/{warehouseId}` |
| Rutas | `GET/POST /api/v1/routes`, `GET/DELETE /api/v1/routes/{routeId}` |
| Simulaciones | `GET/POST /api/v1/simulations`, `GET /api/v1/simulations/{simulationId}`, y `POST /api/v1/simulations/{simulationId}/{start|pause|resume|stop}` |
| Posición | `GET /api/v1/vehicles/{vehicleId}/position` |
| Alertas | `GET /api/v1/alerts?vehicleId={id}&simulationId={id}&severity={INFO|WARNING|CRITICAL}` |

Las tres condiciones de `GET /api/v1/alerts` se pueden combinar; si se envían varias, se aplican conjuntamente.

---

## 3. WebSocket / STOMP

### 3.1. Endpoint

| Entorno | URL |
|---|---|
| Desarrollo | `ws://localhost:8081/ws-native` |
| Desarrollo (SockJS) | `http://localhost:8081/ws` |
| Producción | `wss://api.ecargohub.example/ws` |

### 3.2. Conexión STOMP

**Frames STOMP nativos:**

```
CONNECT
accept-version:1.1,1.0
heart-beat:10000,10000

\x00
```

**Respuesta:**

```
CONNECTED
version:1.1
heart-beat:10000,10000

\x00
```

### 3.3. Canales de suscripción

#### `/topic/vehicle-status/{vehicleId}`

Telemetría en tiempo real de un vehículo concreto.

**Suscribirse:**

```
SUBSCRIBE
id:sub-1
destination:/topic/vehicle-status/1
ack:auto

\x00
```

**Mensaje recibido:**

```json
{
  "vehicleId": 1,
  "latitude": 38.1408,
  "longitude": -0.8844,
  "progress": 45,
  "speedKmh": 87.5,
  "status": "EN_RUTA",
  "timestamp": 1790756789000
}
```

**Estados posibles en `status`:**
- `EN_RUTA` — vehículo en movimiento.
- `PAUSADO` — simulación pausada.
- `COMPLETADO` — llegó a destino.
- `STOPPED` — detenido por comando.

---

#### `/topic/fleet-status`

Eventos globales de flota (afectan a todos los vehículos).

**Suscribirse:**

```
SUBSCRIBE
id:sub-fleet
destination:/topic/fleet-status
ack:auto

\x00
```

**Mensaje recibido:**

```json
{
  "eventType": "VEHICLE_STARTED",
  "vehicleId": 1,
  "status": "EN_RUTA",
  "message": "Vehículo 1 ha iniciado ruta",
  "timestamp": 1790756789000
}
```

**Tipos de evento (`eventType`):**
- `VEHICLE_STARTED` — vehículo arrancado.
- `VEHICLE_PAUSED` — vehículo pausado.
- `VEHICLE_RESUMED` — vehículo reanudado.
- `VEHICLE_STOPPED` — vehículo detenido.
- `VEHICLE_COMPLETED` — vehículo llegado a destino.

---

### 3.4. Ejemplo cliente JavaScript

```javascript
import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

const client = new Client({
  webSocketFactory: () => new SockJS('http://localhost:8081/ws'),
  reconnectDelay: 5000,
  onConnect: () => {
    console.log('Conectado');

    client.subscribe('/topic/vehicle-status/1', (msg) => {
      const telemetry = JSON.parse(msg.body);
      console.log('Telemetría V1:', telemetry);
    });

    client.subscribe('/topic/fleet-status', (msg) => {
      const event = JSON.parse(msg.body);
      console.log('Evento de flota:', event);
    });
  },
  onStompError: (frame) => {
    console.error('Error STOMP:', frame);
  }
});

client.activate();
```

---

## 4. Documentación interactiva

**Swagger UI:** http://localhost:8081/swagger-ui.html

**OpenAPI JSON:** http://localhost:8081/v3/api-docs

**Generación de cliente TypeScript:**

```bash
cd frontend-angular/simulador-logistico-web
npm run generate:api
```

El comando usa la especificación versionada `openapi/openapi.yaml`; no depende de que el backend esté ejecutándose.

---

## 5. Rate limiting

**Estado actual:** sin límites.

**Estado planificado:** rate limit por IP (ej. 100 req/min) en producción.

---

## 6. Health checks

| Endpoint | Propósito |
|---|---|
| `GET /actuator/health` | Estado general + componentes (DB, Kafka, Redis) |
| `GET /actuator/health/liveness` | ¿Está vivo el proceso? |
| `GET /actuator/health/readiness` | ¿Puede recibir tráfico? |
| `GET /actuator/info` | Info del proyecto |
| `GET /actuator/metrics` | Métricas |
| `GET /actuator/mappings` | Rutas registradas |

---

## 7. Versionado

La API sigue un versionado por URL (`/api/v1`, `/api/v2`).

**Política de cambios:**
- **Minor**: añadir endpoints o campos opcionales (compatible).
- **Major**: cambiar contratos existentes (rompe compatibilidad).

---

## 8. Roadmap de la API

### Corto plazo

- Añadir paginación a los endpoints de listado.
- Añadir filtros por fecha a comandos.
- Endpoint `GET /api/v1/vehicles/{id}/route` con la ruta planificada.

### Medio plazo

- Autenticación JWT en todos los endpoints.
- Rate limiting.
- Endpoint `POST /api/v1/vehicles/{id}/redirect` para cambiar destino en marcha.

### Largo plazo

- API GraphQL como alternativa a REST.
- Webhooks para notificar a sistemas externos.