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

**Estado actual:** sin autenticación (solo HTTP Basic opcional con `admin/admin123`).

**Estado planificado:** JWT en header `Authorization: Bearer <token>`.

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
    "name": "Vehículo 1",
    "plate": "1234-ABC"
  },
  {
    "id": 2,
    "name": "Vehículo 2",
    "plate": "5678-DEF"
  }
]
```

**Ejemplo:**

```bash
curl http://localhost:8081/api/v1/vehicles
```

---

#### `GET /api/v1/vehicles/{id}`

Devuelve un vehículo por su ID.

**Parámetros de ruta:**
- `id` (Long) — ID del vehículo.

**Respuesta 200:**

```json
{
  "id": 1,
  "name": "Vehículo 1",
  "plate": "1234-ABC"
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

**Body:**

```json
{
  "name": "Vehículo 3",
  "plate": "9012-GHI"
}
```

**Respuesta 201:**

```json
{
  "id": 3,
  "name": "Vehículo 3",
  "plate": "9012-GHI"
}
```

---

#### `PUT /api/v1/vehicles/{id}`

Actualiza un vehículo existente.

**Body:**

```json
{
  "name": "Vehículo 3 (renombrado)",
  "plate": "9012-GHI"
}
```

**Respuesta 200:**

```json
{
  "id": 3,
  "name": "Vehículo 3 (renombrado)",
  "plate": "9012-GHI"
}
```

---

#### `DELETE /api/v1/vehicles/{id}`

Elimina un vehículo.

**Respuesta 204:** sin body.

---

### 2.2. Estado del vehículo

#### `GET /api/v1/vehicles/{id}/status`

Devuelve el estado actual del vehículo (última posición conocida).

**Respuesta 200:**

```json
{
  "vehicleId": 1,
  "latitude": 38.1408,
  "longitude": -0.8844,
  "progress": 45,
  "speedKmh": 87.5,
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
  "status": "IDLE",
  "updatedAt": null
}
```

---

### 2.3. Telemetría

#### `GET /api/v1/vehicles/{id}/telemetry`

Devuelve el histórico completo de telemetría del vehículo.

**Query params opcionales:**

| Param | Tipo | Descripción |
|---|---|---|
| `from` | ISO-8601 | Fecha de inicio (inclusive) |
| `to` | ISO-8601 | Fecha de fin (inclusive) |

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

#### `GET /api/v1/vehicles/{id}/telemetry/latest`

Devuelve los últimos N puntos de telemetría.

**Query params:**

| Param | Tipo | Default | Descripción |
|---|---|---|---|
| `limit` | int | 50 | Número máximo de puntos (max 500) |

**Respuesta 200:** igual que el endpoint anterior.

---

#### `DELETE /api/v1/vehicles/{id}/telemetry`

Borra todo el histórico de telemetría del vehículo.

**Respuesta 200:**

```json
{
  "vehicleId": 1,
  "deleted": 21
}
```

---

### 2.4. Comandos

#### `POST /api/v1/vehicle-commands/vehicle/{vehicleId}`

Envía un comando a un vehículo.

**Parámetros de ruta:**
- `vehicleId` (Long) — ID del vehículo destinatario.

**Body:**

```json
{
  "command": "START",
  "value": null,
  "originLat": 38.1408,
  "originLon": -0.8844,
  "destLat": 37.9922,
  "destLon": -1.1307
}
```

**Campos del body:**

| Campo | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| `command` | Enum | Sí | `START`, `PAUSE`, `RESUME`, `STOP` |
| `value` | Double | No | Valor asociado (opcional) |
| `originLat` | Double | No | Latitud de origen (opcional) |
| `originLon` | Double | No | Longitud de origen (opcional) |
| `destLat` | Double | No | Latitud de destino (opcional) |
| `destLon` | Double | No | Longitud de destino (opcional) |

**Si no se envían coordenadas**, se usa el trayecto por defecto Cox → Murcia.

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

#### `GET /api/v1/vehicle-commands/vehicle/{vehicleId}`

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
npx @openapitools/openapi-generator-cli generate \
  -i http://localhost:8081/v3/api-docs \
  -g typescript-angular \
  -o ./src/app/api
```

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