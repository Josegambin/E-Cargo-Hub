# Modelo de Datos — E-Cargo Hub

Este documento describe el modelo de datos del sistema **E-Cargo Hub**, incluyendo el diagrama entidad-relación, la descripción detallada de cada tabla y las decisiones de diseño adoptadas.

---

## 1. Diagrama Entidad-Relación

```
┌──────────────────────────┐
│        vehicles          │
│──────────────────────────│
│ id          BIGSERIAL PK │
│ name        VARCHAR(255) │
│ plate       VARCHAR(32)  │
│ created_at  TIMESTAMPTZ  │
└────────────┬─────────────┘
             │
             │ 1:N
             │
    ┌────────┴──────────┬──────────────────┬─────────────────────┐
    │                   │                  │                     │
    ▼                   ▼                  ▼                     ▼
┌─────────────────────┐ ┌──────────────────────┐ ┌─────────────────────┐
│  vehicle_commands   │ │ vehicle_current_     │ │  vehicle_telemetry  │
│                     │ │ status               │ │                     │
│─────────────────────│ │──────────────────────│ │─────────────────────│
│ id          PK      │ │ vehicle_id      PK   │ │ id          PK      │
│ command_id  UUID    │ │ latitude        FLOAT│ │ vehicle_id  FK      │
│ vehicle_id  FK      │ │ longitude       FLOAT│ │ latitude    FLOAT   │
│ command     VARCHAR │ │ progress_pct    INT  │ │ longitude   FLOAT   │
│ command_value FLOAT │ │ speed_kmh       FLOAT│ │ progress_pct INT    │
│ origin_lat  FLOAT   │ │ status          VARCHAR│ │ speed_kmh  FLOAT   │
│ origin_lon  FLOAT   │ │ updated_at      TS   │ │ status      VARCHAR │
│ dest_lat    FLOAT   │ │                      │ │ recorded_at TS      │
│ dest_lon    FLOAT   │ │                      │ │                     │
│ created_at  TS      │ │                      │ │                     │
└─────────────────────┘ └──────────────────────┘ └─────────────────────┘
```

**Leyenda:**
- `PK` = Primary Key
- `FK` = Foreign Key
- `TS` = TIMESTAMPTZ (timestamp con zona horaria)
- `FLOAT` = DOUBLE PRECISION
- `1:N` = Relación uno a muchos

---

## 2. Descripción de tablas

### 2.1. Tabla `vehicles`

Almacena los vehículos (camiones) registrados en el sistema.

| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `id` | BIGSERIAL | PK, NOT NULL | Identificador único del vehículo |
| `name` | VARCHAR(255) | NOT NULL | Nombre del vehículo (ej. "Camión 1") |
| `plate` | VARCHAR(32) | NULL | Matrícula del vehículo |
| `created_at` | TIMESTAMPTZ | NOT NULL, DEFAULT NOW() | Fecha de alta del vehículo |

**Ejemplo:**

```sql
INSERT INTO vehicles (name, plate) VALUES
    ('Vehículo 1', '1234-ABC'),
    ('Vehículo 2', '5678-DEF');
```

---

### 2.2. Tabla `vehicle_commands`

Almacena el historial de comandos enviados a los vehículos.

| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `id` | BIGSERIAL | PK, NOT NULL | Identificador único del comando |
| `command_id` | UUID | NOT NULL, UNIQUE | UUID único para idempotencia |
| `vehicle_id` | BIGINT | FK → vehicles(id), NOT NULL | Vehículo destinatario |
| `command` | VARCHAR(32) | NOT NULL | Tipo: START, PAUSE, RESUME, STOP |
| `command_value` | DOUBLE PRECISION | NULL | Valor opcional del comando |
| `origin_lat` | DOUBLE PRECISION | NULL | Latitud de origen (opcional) |
| `origin_lon` | DOUBLE PRECISION | NULL | Longitud de origen (opcional) |
| `dest_lat` | DOUBLE PRECISION | NULL | Latitud de destino (opcional) |
| `dest_lon` | DOUBLE PRECISION | NULL | Longitud de destino (opcional) |
| `created_at` | TIMESTAMPTZ | NOT NULL | Fecha de envío del comando |

**Índices:**

```sql
CREATE UNIQUE INDEX uk_command_id ON vehicle_commands (command_id);
CREATE INDEX idx_commands_vehicle_time ON vehicle_commands (vehicle_id, created_at DESC);
```

**Restricciones:**

- `uk_command_id`: el `command_id` es único en toda la tabla (garantiza idempotencia).
- `fk_vehicle_id`: el vehículo debe existir.

**Ejemplo:**

```sql
INSERT INTO vehicle_commands
    (command_id, vehicle_id, command, created_at)
VALUES
    ('47a1c31b-f21b-433e-ba85-ed2e38db4a5f', 1, 'START', NOW());
```

---

### 2.3. Tabla `vehicle_current_status`

Almacena la **última posición conocida** de cada vehículo (una fila por vehículo).

| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `vehicle_id` | BIGINT | PK, FK → vehicles(id), NOT NULL | Vehículo (PK porque es 1:1) |
| `latitude` | DOUBLE PRECISION | NOT NULL | Última latitud conocida |
| `longitude` | DOUBLE PRECISION | NOT NULL | Última longitud conocida |
| `progress_pct` | INTEGER | NOT NULL | Progreso del trayecto (0-100) |
| `speed_kmh` | DOUBLE PRECISION | NOT NULL | Velocidad actual en km/h |
| `status` | VARCHAR(32) | NOT NULL | IDLE, EN_RUTA, PAUSADO, COMPLETADO, STOPPED |
| `updated_at` | TIMESTAMPTZ | NOT NULL | Última actualización |

**Operación típica:** UPSERT (insert si no existe, update si existe).

**Ejemplo:**

```sql
INSERT INTO vehicle_current_status
    (vehicle_id, latitude, longitude, progress_pct, speed_kmh, status, updated_at)
VALUES
    (1, 38.1408, -0.8844, 25, 87.5, 'EN_RUTA', NOW())
ON CONFLICT (vehicle_id) DO UPDATE SET
    latitude = EXCLUDED.latitude,
    longitude = EXCLUDED.longitude,
    progress_pct = EXCLUDED.progress_pct,
    speed_kmh = EXCLUDED.speed_kmh,
    status = EXCLUDED.status,
    updated_at = EXCLUDED.updated_at;
```

**¿Por qué una tabla separada y no una columna en `vehicles`?**

- Separación de responsabilidades: `vehicles` es catálogo, `vehicle_current_status` es estado.
- Si un vehículo no tiene estado (nunca se ha movido), no hay fila → se interpreta como IDLE.
- Evita `UPDATE` constantes en la tabla principal.

---

### 2.4. Tabla `vehicle_telemetry`

Almacena el **histórico completo de telemetría** (cada punto emitido durante una simulación).

| Columna | Tipo | Restricciones | Descripción |
|---|---|---|---|
| `id` | BIGSERIAL | PK, NOT NULL | Identificador único del punto |
| `vehicle_id` | BIGINT | FK → vehicles(id), NOT NULL | Vehículo emisor |
| `latitude` | DOUBLE PRECISION | NOT NULL | Latitud del punto |
| `longitude` | DOUBLE PRECISION | NOT NULL | Longitud del punto |
| `progress_pct` | INTEGER | NOT NULL | Progreso (0-100) |
| `speed_kmh` | DOUBLE PRECISION | NOT NULL | Velocidad en ese punto |
| `status` | VARCHAR(32) | NOT NULL | Estado en ese punto |
| `recorded_at` | TIMESTAMPTZ | NOT NULL | Momento de la emisión |

**Índices:**

```sql
CREATE INDEX idx_telemetry_vehicle_time
    ON vehicle_telemetry (vehicle_id, recorded_at DESC);
```

**¿Por qué el índice `(vehicle_id, recorded_at DESC)`?**

- Las consultas típicas son "todos los puntos del vehículo X ordenados por fecha".
- El índice compuesto permite responder esas consultas en O(log n) en lugar de O(n).

**Ejemplo:**

```sql
INSERT INTO vehicle_telemetry
    (vehicle_id, latitude, longitude, progress_pct, speed_kmh, status, recorded_at)
VALUES
    (1, 38.1408, -0.8844, 5, 4.5, 'EN_RUTA', NOW());
```

---

## 3. Índices y restricciones

### 3.1. Resumen de índices

| Tabla | Índice | Tipo | Columnas | Propósito |
|---|---|---|---|---|
| `vehicle_commands` | `uk_command_id` | UNIQUE | `command_id` | Idempotencia |
| `vehicle_commands` | `idx_commands_vehicle_time` | BTREE | `(vehicle_id, created_at DESC)` | Consultas por vehículo |
| `vehicle_current_status` | `pk_vehicle_id` | PRIMARY | `vehicle_id` | 1:1 con vehículos |
| `vehicle_telemetry` | `idx_telemetry_vehicle_time` | BTREE | `(vehicle_id, recorded_at DESC)` | Consultas de histórico |

### 3.2. Resumen de FKs

| Tabla | FK | Referencia | On Delete |
|---|---|---|---|
| `vehicle_commands` | `vehicle_id` | `vehicles(id)` | CASCADE |
| `vehicle_current_status` | `vehicle_id` | `vehicles(id)` | CASCADE |
| `vehicle_telemetry` | `vehicle_id` | `vehicles(id)` | CASCADE |

**`ON DELETE CASCADE`:** si se elimina un vehículo, se eliminan todos sus comandos, estado y telemetría.

---

## 4. Consultas típicas

### 4.1. Obtener el estado actual de un vehículo

```sql
SELECT *
FROM vehicle_current_status
WHERE vehicle_id = 1;
```

### 4.2. Obtener el histórico de telemetría de un vehículo

```sql
SELECT *
FROM vehicle_telemetry
WHERE vehicle_id = 1
ORDER BY recorded_at ASC;
```

### 4.3. Obtener los últimos N puntos de un vehículo

```sql
SELECT *
FROM vehicle_telemetry
WHERE vehicle_id = 1
ORDER BY recorded_at DESC
LIMIT 50;
```

### 4.4. Obtener los comandos de un vehículo

```sql
SELECT *
FROM vehicle_commands
WHERE vehicle_id = 1
ORDER BY created_at DESC;
```

### 4.5. Contar comandos por tipo

```sql
SELECT command, COUNT(*)
FROM vehicle_commands
GROUP BY command;
```

### 4.6. Limpiar telemetría antigua (job futuro)

```sql
DELETE FROM vehicle_telemetry
WHERE recorded_at < NOW() - INTERVAL '30 days';
```

---

## 5. Estrategia de migración

**Fase actual (desarrollo):** Hibernate genera el esquema con `ddl-auto: update`.

**Fase futura (producción):** Flyway gestionará las migraciones.

### 5.1. Esquema de Flyway (planificado)

```
src/main/resources/db/migration/
├── V1__initial_schema.sql
├── V2__add_jwt_users.sql
├── V3__add_pagination_indexes.sql
└── V4__...
```

**Convención:**
- Prefijo `V` (versión).
- Número consecutivo.
- Doble guion bajo `__`.
- Descripción en snake_case.

### 5.2. Regla de oro

> **Nunca modificar una migración ya aplicada en producción.**
> Si te equivocas, crea una nueva migración que lo corrija.

---

## 6. Convenciones de nombres

| Elemento | Convención | Ejemplo |
|---|---|---|
| Tablas | `snake_case`, plural | `vehicles`, `vehicle_commands` |
| Columnas | `snake_case` | `vehicle_id`, `created_at` |
| Claves primarias | `id` (o `vehicle_id` en tablas 1:1) | `id`, `vehicle_id` |
| Claves foráneas | `<tabla_singular>_id` | `vehicle_id` |
| Índices | `idx_<tabla>_<columnas>` | `idx_telemetry_vehicle_time` |
| Unique constraints | `uk_<columnas>` | `uk_command_id` |
| Foreign keys | `fk_<tabla>_<columna>` | `fk_commands_vehicle` |

---

## 7. Consideraciones de rendimiento

### 7.1. Volumen esperado

Con el alcance del TFG:
- **Vehículos**: 2-10.
- **Comandos**: ~20/día.
- **Telemetría**: ~21 puntos por simulación × 20 simulaciones/día = 420 puntos/día.

**Volumen anual:** ~150.000 filas en `vehicle_telemetry`. Manejable sin particionado.

### 7.2. Cuellos de botella conocidos

- **Escritura de telemetría**: cada punto es un `INSERT`. Con 4 vehículos en paralelo → 4 inserts/segundo. Aceptable.
- **Consultas de histórico completo**: si un vehículo tiene millones de puntos, un `SELECT *` es lento. Mitigado con `LIMIT` y filtro por fecha.

### 7.3. Optimizaciones futuras

- **Batch inserts** en telemetría (`saveAll` o `JdbcTemplate.batchUpdate`).
- **Particionado** de `vehicle_telemetry` por mes.
- **Job de limpieza** con TTL.
- **Read replicas** para consultas pesadas.

---

## 8. Seguridad de datos

- **Contraseñas**: nunca en claro, siempre con BCrypt.
- **Datos personales (RGPD)**: nombre, email de usuarios → cifrado en reposo en producción.
- **Backups**: `pg_dump` diario en producción.
- **Acceso**: usuario de BBDD con permisos mínimos (no `superuser`).

---

## 9. Historial de cambios del modelo

| Versión | Fecha | Cambio |
|---|---|---|
| 1.0 | Septiembre 2025 | Esquema inicial con 4 tablas |
| 1.1 | Octubre 2025 | Añadidas coordenadas a `vehicle_commands` |
| 1.2 | Noviembre 2025 | Pendiente: tabla `users` para JWT |