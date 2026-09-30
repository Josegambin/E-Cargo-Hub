# Dossier Técnico — E-Cargo Hub

**Sistema de Simulación Logística en Tiempo Real**

---

## Portada

**Proyecto:** E-Cargo Hub — Sistema de Simulación Logística en Tiempo Real

**Autor:** Jose Manuel Gambin Manresa

**Director del proyecto:** Alejandro Brugarolas

**Centro:** EFA El Campico (Jacarilla, Alicante)

**Ciclo:** Desarrollo de Aplicaciones Web (DAW) — 2º Curso

**Curso académico:** 2025-2026

**Fecha de entrega:** Diciembre 2025

---

## Índice

1. [Resumen ejecutivo](#1-resumen-ejecutivo)
2. [Introducción](#2-introducción)
3. [Objetivos](#3-objetivos)
4. [Estado del arte](#4-estado-del-arte)
5. [Análisis de requisitos](#5-análisis-de-requisitos)
6. [Diseño arquitectónico](#6-diseño-arquitectónico)
7. [Diseño detallado](#7-diseño-detallado)
8. [Implementación](#8-implementación)
9. [Pruebas](#9-pruebas)
10. [Despliegue](#10-despliegue)
11. [Conclusiones y trabajo futuro](#11-conclusiones-y-trabajo-futuro)
12. [Bibliografía](#12-bibliografía)

---

## 1. Resumen ejecutivo

**E-Cargo Hub** es un sistema de información web para la simulación y monitorización en tiempo real de flotas de vehículos logísticos. El sistema permite enviar comandos de control a vehículos simulados, calcular rutas reales por carretera, visualizar el movimiento sobre un mapa interactivo y consultar el histórico de telemetría.

**Características principales:**

- **Arquitectura orientada a eventos** con Apache Kafka.
- **Comunicación en tiempo real** vía WebSocket/STOMP.
- **Idempotencia** de comandos con Redis.
- **Cálculo de rutas reales** con GraphHopper, con caché y fallback.
- **Frontend SPA** en Angular.
- **Despliegue containerizado** con Docker Compose.

**Stack tecnológico:** Java 21, Spring Boot 4, Angular 18, PostgreSQL 17, Apache Kafka 4.1, Redis 7, Docker.

**Resultado:** un sistema completo, resiliente y desplegable, que demuestra el dominio de tecnologías modernas de desarrollo web full-stack.

---

## 2. Introducción

### 2.1. Contexto

El sector logístico está experimentando una transformación digital sin precedentes. Las empresas necesitan herramientas para monitorizar sus flotas en tiempo real, optimizar rutas y reaccionar rápidamente ante imprevistos.

**E-Cargo Hub** nace como respuesta a esa necesidad, ofreciendo una plataforma web que simula el comportamiento de una flota de vehículos logísticos y permite monitorizarla en tiempo real.

### 2.2. Motivación

Este proyecto es el **Trabajo de Fin de Grado** del Ciclo Formativo de Grado Superior en Desarrollo de Aplicaciones Web (DAW). La motivación principal es:

- **Demostrar** el dominio de tecnologías modernas de desarrollo web.
- **Aplicar** patrones de arquitectura empresarial (Event-Driven, Pub/Sub).
- **Integrar** múltiples servicios (Kafka, Redis, PostgreSQL, GraphHopper).
- **Construir** un sistema completo, desde el backend hasta el frontend.

### 2.3. Alcance

El sistema **incluye**:

- Gestión de vehículos (CRUD).
- Envío de comandos de control (START, PAUSE, RESUME, STOP).
- Simulación de movimiento con rutas reales.
- Monitorización en tiempo real sobre mapa.
- Histórico de telemetría.
- Eventos globales de flota.
- Autenticación JWT con roles (en desarrollo).
- Documentación completa.

El sistema **no incluye**:

- Gestión real de flotas físicas.
- Facturación ni contabilidad.
- Integración con ERPs externos.

---

## 3. Objetivos

### 3.1. Objetivo general

Diseñar e implementar un sistema de información web para la simulación y monitorización en tiempo real de flotas de vehículos logísticos, aplicando tecnologías y patrones de arquitectura modernos.

### 3.2. Objetivos específicos

1. **Backend robusto** con Spring Boot 4, arquitectura MVC en capas, manejo de errores uniforme y documentación OpenAPI.
2. **Arquitectura orientada a eventos** con Apache Kafka para desacoplar el envío de comandos de su procesamiento.
3. **Comunicación en tiempo real** con WebSocket/STOMP, con canales separados por vehículo.
4. **Persistencia** en PostgreSQL con JPA/Hibernate, con modelo de datos bien diseñado.
5. **Idempotencia** de comandos con Redis.
6. **Integración con APIs externas** (GraphHopper) con caché y fallback.
7. **Frontend SPA** en Angular con Angular Material y Leaflet.
8. **Despliegue containerizado** con Docker Compose.
9. **Documentación completa** (ERS, arquitectura, modelo de datos, API, despliegue).
10. **Tests** unitarios e integración con cobertura mínima del 60%.

---

## 4. Estado del arte

### 4.1. Sistemas de gestión de flotas

Los sistemas de gestión de flotas (Fleet Management Systems) son plataformas que permiten a las empresas monitorizar y gestionar sus vehículos en tiempo real. Ejemplos comerciales:

- **Samsara**: plataforma completa con IoT, cámaras y telemetría.
- **Verizon Connect**: gestión de flotas con GPS y análisis.
- **Geotab**: telemática y análisis de datos.

Estos sistemas suelen ser **propietarios, caros y cerrados**. E-Cargo Hub propone una **alternativa académica, abierta y educativa**.

### 4.2. Tecnologías de mensajería

Apache Kafka se ha convertido en el **estándar de facto** para arquitecturas orientadas a eventos. Alternativas:

- **RabbitMQ**: más tradicional, colas punto a punto.
- **ActiveMQ**: JMS, más empresarial.
- **Pulsar**: más moderno, multi-tenant.

Kafka se elige por su **escalabilidad, persistencia y ecosistema**.

### 4.3. Comunicación en tiempo real

Opciones para comunicación en tiempo real:

- **WebSocket nativo**: bidireccional, bajo nivel.
- **STOMP sobre WebSocket**: protocolo de mensajería, pub/sub.
- **Server-Sent Events (SSE)**: unidireccional servidor→cliente.
- **Long polling**: más antiguo, menos eficiente.

Se elige **STOMP sobre WebSocket** por su **soporte nativo de pub/sub y su integración con Spring**.

### 4.4. Cálculo de rutas

Alternativas a GraphHopper:

- **Google Maps Directions API**: excelente pero de pago.
- **OpenRouteService**: open source, buena calidad.
- **OSRM**: open source, self-hosted.
- **GraphHopper**: buena calidad, plan gratuito.

Se elige **GraphHopper** por su **plan gratuito, calidad de rutas y facilidad de uso**.

---

## 5. Análisis de requisitos

Ver documento completo: **[ERS.md](ERS.md)**.

### 5.1. Resumen de requisitos funcionales

| ID | Nombre | Estado |
|---|---|---|
| RF01 | Autenticación de usuarios | Planificado |
| RF02 | Login con OAuth2 | Planificado |
| RF03 | Roles y permisos | Planificado |
| RF04 | CRUD de vehículos | Implementado |
| RF05 | CRUD de usuarios | Planificado |
| RF06 | Envío de comandos | Implementado |
| RF07 | Cálculo de rutas | Implementado |
| RF08 | Simulación de movimiento | Implementado |
| RF09 | Idempotencia | Implementado |
| RF10 | Telemetría en tiempo real | Implementado |
| RF11 | Eventos globales | Implementado |
| RF12 | Histórico de telemetría | Implementado |
| RF13 | Paginación | Planificado |
| RF14 | Visualización en mapa | En desarrollo |
| RF15 | Swagger | Implementado |

### 5.2. Resumen de requisitos no funcionales

| ID | Nombre | Prioridad |
|---|---|---|
| RNF01 | Interfaz intuitiva | Alta |
| RNF02 | Rendimiento | Alta |
| RNF03 | Escalabilidad | Media |
| RNF04 | Seguridad | Alta |
| RNF05 | Disponibilidad | Media |
| RNF06 | Mantenibilidad | Alta |
| RNF07 | Portabilidad | Alta |
| RNF08 | Usabilidad | Alta |

---

## 6. Diseño arquitectónico

Ver documento completo: **[ARCHITECTURE.md](../ARCHITECTURE.md)**.

### 6.1. Visión general

```
Frontend Angular ──REST────► Spring Boot API ──► PostgreSQL
        │                          │
        │                          ▼
        └──WebSocket STOMP────► Apache Kafka ──► Kafka Consumer
                                       │              │
                                       │              ├──► GraphHopper
                                       │              ├──► PostgreSQL
                                       │              └──► Redis
                                       │
                                       └──────────────► WebSocket (eventos)
```

### 6.2. Decisiones de diseño clave

1. **Kafka** para desacoplar comandos de su procesamiento.
2. **Redis** para idempotencia (SET NX con TTL).
3. **WebSocket/STOMP** para comunicación en tiempo real.
4. **GraphHopper** con caché LRU y fallback lineal.
5. **Jackson 3** alineado con Spring Boot 4.
6. **MapStruct** para mapeo Entity ↔ DTO.
7. **Docker Compose** para despliegue reproducible.

---

## 7. Diseño detallado

### 7.1. Modelo de datos

Ver documento completo: **[DATA_MODEL.md](DATA_MODEL.md)**.

**Tablas:**

- `vehicles`: catálogo de vehículos.
- `vehicle_commands`: historial de comandos.
- `vehicle_current_status`: última posición conocida.
- `vehicle_telemetry`: histórico completo de telemetría.

### 7.2. Capas del backend

| Capa | Paquete | Responsabilidad |
|---|---|---|
| Controller | `com.ecargohub.backend.controller` | Endpoints REST |
| Service | `com.ecargohub.backend.service` | Lógica de negocio |
| Repository | `com.ecargohub.backend.repository` | Acceso a datos |
| Entity | `com.ecargohub.backend.entity` | Modelo JPA |
| DTO | `com.ecargohub.backend.dto` | Transferencia |
| Mapper | `com.ecargohub.backend.mapper` | Conversión |
| Kafka | `com.ecargohub.backend.kafka` | Producer/Consumer |
| Config | `com.ecargohub.backend.config` | Configuración |
| Exception | `com.ecargohub.backend.exception` | Manejo de errores |

### 7.3. Flujos principales

1. **Envío de comando:** Angular → Controller → Service → BBDD → Kafka.
2. **Procesamiento:** Kafka → Consumer → GraphHopper → Simulación → WebSocket.
3. **Telemetría en vivo:** WebSocket → Angular → Mapa.

Ver diagramas de secuencia en **[ARCHITECTURE.md](../ARCHITECTURE.md)**.

---

## 8. Implementación

### 8.1. Backend

**Tecnologías:**

- Java 21
- Spring Boot 4.0.8
- Spring Kafka 4.0.x
- Spring Data JPA
- Hibernate 7.2
- Jackson 3
- Lombok 1.18.42
- MapStruct 1.6.3
- SpringDoc OpenAPI 2.8.5

**Estructura de paquetes:**

```
com.ecargohub.backend
├── config/         → Configuración
├── controller/     → Endpoints REST
├── domain/enums/   → Enumerados
├── dto/            → DTOs
├── entity/         → Entidades JPA
├── exception/      → Excepciones
├── interfaces/     → Interfaces de servicios
├── kafka/          → Kafka
├── mapper/         → Mappers MapStruct
├── repository/     → Repositorios
└── service/        → Servicios
```

### 8.2. Frontend (en desarrollo)

**Tecnologías:**

- Angular 18+
- TypeScript 5.x
- Angular Material
- Leaflet 1.9
- @stomp/stompjs 7.x

**Estructura:**

```
src/app
├── core/
│   ├── models/       → Interfaces TS
│   ├── services/     → Servicios HTTP/WS
│   └── interceptors/ → Interceptores
├── features/
│   ├── dashboard/
│   ├── fleet-map/
│   └── vehicle-list/
└── shared/
    └── components/
```

### 8.3. Infraestructura

**Docker Compose** levanta:

- PostgreSQL 17
- Apache Kafka 4.1 (KRaft)
- Kafka UI
- Redis 7
- Backend Spring Boot
- Frontend Angular

**Configuración:**

```bash
docker compose up -d
```

### 8.4. Métricas del proyecto

| Métrica | Valor |
|---|---|
| Líneas de código Java | ~3000 |
| Líneas de código TypeScript | ~2000 |
| Líneas de SQL | ~200 |
| Clases Java | ~50 |
| Componentes Angular | ~10 |
| Endpoints REST | ~12 |
| Canales WebSocket | 3 |
| Tests unitarios | pendiente |
| Cobertura de tests | pendiente |

---

## 9. Pruebas

### 9.1. Estrategia de pruebas

| Nivel | Herramienta | Cobertura |
|---|---|---|
| Unitarias | JUnit 5 + Mockito | Servicios |
| Integración | Spring Boot Test + Testcontainers | Repositorios + Kafka |
| E2E | Postman / Newman | API completa |
| Frontend | Jasmine + Karma | Componentes |

### 9.2. Casos de prueba principales

1. **Envío de comando válido** → 200 OK, comando en BBDD, mensaje en Kafka.
2. **Envío de comando duplicado** → descartado por Redis.
3. **START en vehículo ya corriendo** → 409 Conflict.
4. **PAUSE sin vehículo corriendo** → 409 Conflict.
5. **Comando con coordenadas inválidas** → 400 Bad Request.
6. **Vehículo inexistente** → 404 Not Found.
7. **WebSocket recibe telemetría** → mensaje en `/topic/vehicle-status/1`.
8. **Eventos globales** → mensaje en `/topic/fleet-status`.

### 9.3. Resultados

**Estado actual:** pruebas manuales exhaustivas mediante Postman. Tests automatizados pendientes.

---

## 10. Despliegue

Ver documento completo: **[DEPLOYMENT.md](DEPLOYMENT.md)**.

### 10.1. Entornos

- **Local (dev)**: para desarrollo.
- **Producción**: VPS con Docker Compose + Nginx + Let's Encrypt.
- **Frontend**: Vercel (plan gratuito).

### 10.2. Pipeline de despliegue

```bash
git push origin main     # → CI/CD (pendiente)
docker compose build     # → Build
docker compose up -d     # → Deploy
```

### 10.3. Monitorización

- **Health checks**: `/actuator/health`.
- **Métricas**: `/actuator/metrics` (pendiente Prometheus + Grafana).
- **Logs**: rotación automática en producción.

---

## 11. Conclusiones y trabajo futuro

### 11.1. Conclusiones

**E-Cargo Hub** demuestra el dominio de tecnologías modernas de desarrollo web full-stack:

- **Backend**: Spring Boot 4 con arquitectura MVC, Kafka, Redis, JPA.
- **Frontend**: Angular con Leaflet y WebSocket.
- **Infraestructura**: Docker Compose con 6 servicios.
- **Documentación**: ERS, arquitectura, modelo de datos, API, despliegue.
- **Patrones**: Event-Driven, Pub/Sub, MVC, Repository, DTO, Mapper.

**Objetivos cumplidos:**

- ✅ Backend robusto con Spring Boot 4.
- ✅ Arquitectura orientada a eventos con Kafka.
- ✅ Comunicación en tiempo real con WebSocket.
- ✅ Persistencia con PostgreSQL.
- ✅ Idempotencia con Redis.
- ✅ Integración con GraphHopper.
- ✅ Documentación completa.
- ⏳ Frontend Angular (en desarrollo).
- ⏳ Tests automatizados (pendiente).
- ⏳ Autenticación JWT (pendiente).

### 11.2. Trabajo futuro

**Corto plazo:**

- Completar frontend Angular.
- Autenticación JWT con roles.
- Login con OAuth2 (Google).
- CRUD completo de usuarios.
- Paginación en listados.
- Tests automatizados.

**Medio plazo:**

- Persistencia de rutas calculadas.
- Multi-tenant.
- Panel de administración.
- WebSocket con autenticación JWT.

**Largo plazo:**

- Soporte multi-vehículo.
- Integración con tráfico real.
- Machine Learning para predicción de tiempos.
- Kubernetes.

### 11.3. Reflexión personal

El desarrollo de **E-Cargo Hub** ha sido un reto técnico y personal. He aprendido:

- A diseñar **arquitecturas orientadas a eventos** con Kafka.
- A integrar **múltiples servicios** (Kafka, Redis, PostgreSQL).
- A manejar **comunicación en tiempo real** con WebSocket.
- A aplicar **patrones de diseño** empresariales.
- A **documentar** un proyecto completo siguiendo estándares.
- A **resolver problemas** complejos de forma autónoma.

El resultado es un sistema **sólido, resiliente y desplegable**, que demuestra mi preparación como desarrollador web full-stack.

---

## 12. Bibliografía

### Documentación oficial

1. Spring Boot. https://spring.io/projects/spring-boot
2. Apache Kafka. https://kafka.apache.org/documentation/
3. Angular. https://angular.io/docs
4. PostgreSQL. https://www.postgresql.org/docs/
5. Redis. https://redis.io/docs/
6. GraphHopper. https://docs.graphhopper.com/
7. Leaflet. https://leafletjs.com/reference.html
8. Docker. https://docs.docker.com/

### Estándares

9. IEEE Std 830-1998. *IEEE Recommended Practice for Software Requirements Specifications*.

### Libros

10. *Spring in Action*, Craig Walls.
11. *Kafka: The Definitive Guide*, Neha Narkhede et al.
12. *Designing Data-Intensive Applications*, Martin Kleppmann.

### Artículos

13. *Event-Driven Architecture*, Martin Fowler.
14. *Microservices Patterns*, Chris Richardson.

---

**Fin del dossier técnico.**