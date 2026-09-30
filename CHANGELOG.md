# Changelog

Todos los cambios notables de este proyecto se documentan en este archivo.

El formato está basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.0.0/),
y este proyecto sigue [Versionado Semántico](https://semver.org/lang/es/).

---

## [Unreleased]

### Added
- Autenticación JWT con roles (ADMIN, OPERADOR, VIEWER).
- Login con OAuth2 (Google).
- CRUD completo de usuarios.
- Paginación en endpoints de listado.
- Frontend Angular completo.
- Docker Compose con todos los servicios.
- Migraciones Flyway.
- Perfiles dev/prod.
- Tests unitarios y de integración.

---

## [0.3.0] - 2025-09-30

### Added
- **Bloque 1**: Multi-vehículo con canales WebSocket por vehículo.
- **Bloque 2**: Persistencia de última posición del vehículo + endpoint `GET /status`.
- **Bloque 3**: Comandos PAUSE, RESUME, STOP con máquina de estados.
- **Bloque 4**: Telemetría histórica completa + endpoints `GET /telemetry`.
- **Bloque 5**: Rutas parametrizables con coordenadas de origen/destino.
- **Bloque 5**: Caché LRU de rutas en GraphHopper para evitar rate limits.
- **Bloque 6**: Notificaciones globales de flota en `/topic/fleet-status`.
- **Sprint A**: CORS configurado, `GlobalExceptionHandler` completo, Actuator con health checks.
- **Sprint B**: Swagger UI con documentación completa de endpoints.
- **Sprint B**: Generación automática de cliente TypeScript para Angular.
- Redis para idempotencia de comandos.
- Fallback lineal si GraphHopper falla.

### Changed
- Migración de Jackson 2 a Jackson 3.
- Lombok actualizado a 1.18.42 para compatibilidad con JDK 21.
- Eliminado `commandId` del `VehicleCommandRequest` (ahora generado por el backend).

### Fixed
- NPE en `VehicleCommandServiceImpl` por inyección incorrecta de `kafkaProducerService`.
- `ClassFormatError` causado por `@Builder` en records con Lombok antiguo.
- Error `No static resource` por rutas incorrectas sin `/v1`.
- Fechas serializadas como timestamps numéricos (ahora ISO-8601).
- Configuración incorrecta de `spring.data.redis` (indentación en YAML).

---

## [0.2.0] - 2025-09-15

### Added
- Persistencia de comandos en PostgreSQL.
- Integración con Apache Kafka para desacoplar el envío de comandos.
- Consumer de Kafka que procesa comandos y arranca simulaciones.
- Simulación de rutas Cox → Murcia con 20 puntos intermedios.
- WebSocket STOMP para telemetría en tiempo real.
- Frontend HTML vanilla con Leaflet para visualización.

### Changed
- Reestructuración del proyecto siguiendo el patrón MVC.

### Fixed
- Configuración de `@EnableKafka` para que el consumer arrancara correctamente.

---

## [0.1.0] - 2025-09-01

### Added
- Estructura inicial del proyecto Spring Boot.
- Entidad `VehicleEntity` y repositorio.
- Endpoint `POST /api/v1/vehicle-commands/vehicle/{id}` para enviar comandos.
- CRUD básico de vehículos.
- Docker Compose con PostgreSQL, Kafka, Kafka UI.

---

[Unreleased]: https://github.com/jmgambin/E-Cargo-Hub/compare/v0.3.0...HEAD
[0.3.0]: https://github.com/jmgambin/E-Cargo-Hub/compare/v0.2.0...v0.3.0
[0.2.0]: https://github.com/jmgambin/E-Cargo-Hub/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/jmgambin/E-Cargo-Hub/releases/tag/v0.1.0