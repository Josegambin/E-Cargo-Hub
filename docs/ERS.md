# Especificación de Requisitos de Software (ERS)

**Proyecto:** Sistema de Simulación Logística E-Cargo Hub

**Revisión:** 1.0

**Fecha:** Septiembre 2025

**Autor:** Jose Manuel Gambin Manresa

**Director del proyecto:** Alejandro Brugarolas

**Centro:** EFA El Campico (Jacarilla, Alicante)

**Ciclo:** Desarrollo de Aplicaciones Web (DAW) — 2º Curso

---

## Contenido

1. [Introducción](#1-introducción)
2. [Descripción general](#2-descripción-general)
3. [Requisitos específicos](#3-requisitos-específicos)
4. [Modelo de negocio](#4-modelo-de-negocio)
5. [Estudio económico del proyecto](#5-estudio-económico-del-proyecto)
6. [Bibliografía](#6-bibliografía)

---

## 1. Introducción

Este documento es una Especificación de Requisitos Software (ERS) para el sistema **E-Cargo Hub**. Está basado y es conforme con el estándar **IEEE Std 830-1998** (IEEE Recommended Practice for Software Requirements Specifications).

### 1.1. Propósito

El propósito de este documento es definir las especificaciones funcionales y no funcionales del sistema **E-Cargo Hub**, un sistema de información web para la simulación y monitorización en tiempo real de flotas de vehículos logísticos.

**Audiencia a la que va dirigido:**

- **Director del proyecto (Alejandro Brugarolas)**: para validar el alcance y la calidad de los requisitos.
- **Tribunal evaluador**: para evaluar la completitud y coherencia del proyecto.
- **Desarrollador (Jose Manuel Gambin Manresa)**: como referencia durante la implementación.
- **Futuros mantenedores**: para comprender el sistema sin necesidad de leer el código.

### 1.2. Alcance

El sistema **E-Cargo Hub** es un producto software independiente, accesible vía web, que permite:

- **Enviar comandos de control** a vehículos logísticos simulados (START, PAUSE, RESUME, STOP).
- **Simular el movimiento** de vehículos sobre rutas reales calculadas con GraphHopper.
- **Visualizar en tiempo real** el estado y posición de los vehículos sobre un mapa interactivo.
- **Persistir el histórico de telemetría** para consulta posterior.
- **Emitir notificaciones globales** de eventos de flota.

El sistema **no incluye**:

- Gestión real de flotas físicas (es una simulación).
- Facturación ni contabilidad.
- Integración con sistemas ERP externos.

### 1.3. Personal involucrado

| Nombre | Rol | Categoría profesional | Responsabilidades | Contacto |
|---|---|---|---|---|
| Jose Manuel Gambin Manresa | Desarrollador principal | Estudiante de DAW | Análisis, diseño, implementación y documentación | — |
| Alejandro Brugarolas | Director del proyecto | Profesor de DAW | Supervisión, validación y evaluación | — |

### 1.4. Definiciones, acrónimos y abreviaturas

| Término | Descripción |
|---|---|
| **API** | Application Programming Interface |
| **CORS** | Cross-Origin Resource Sharing |
| **CRUD** | Create, Read, Update, Delete |
| **DAW** | Desarrollo de Aplicaciones Web |
| **DTO** | Data Transfer Object |
| **EFA** | Escuela Familiar Agraria |
| **ERS** | Especificación de Requisitos Software |
| **IEEE** | Institute of Electrical and Electronics Engineers |
| **JWT** | JSON Web Token |
| **LRU** | Least Recently Used (algoritmo de caché) |
| **ORM** | Object-Relational Mapping |
| **REST** | Representational State Transfer |
| **RF** | Requisito Funcional |
| **RNF** | Requisito No Funcional |
| **SPA** | Single Page Application |
| **STOMP** | Simple Text Oriented Messaging Protocol |
| **TFG** | Trabajo de Fin de Grado |
| **UPSERT** | Insert or Update (operación) |
| **WebSocket** | Protocolo de comunicación bidireccional en tiempo real |

### 1.5. Referencias

| Referencia | Título | Ruta | Fecha | Autor |
|---|---|---|---|---|
| [Ref. 1] | IEEE Std 830-1998 | IEEE Recommended Practice for Software Requirements Specifications | 1998 | IEEE |
| [Ref. 2] | Documentación Spring Boot | https://spring.io/projects/spring-boot | 2025 | VMware |
| [Ref. 3] | Documentación Apache Kafka | https://kafka.apache.org/documentation/ | 2025 | Apache Foundation |
| [Ref. 4] | Documentación Angular | https://angular.io/docs | 2025 | Google |
| [Ref. 5] | Documentación GraphHopper | https://docs.graphhopper.com/ | 2025 | GraphHopper GmbH |

### 1.6. Resumen

Este documento está organizado en cinco secciones:

1. **Introducción**: propósito, alcance, personal involucrado y definiciones.
2. **Descripción general**: perspectiva del producto, funcionalidades, usuarios, restricciones y evolución prevista.
3. **Requisitos específicos**: requisitos funcionales (RF) y no funcionales (RNF) detallados.
4. **Modelo de negocio**: modelo de explotación del sistema.
5. **Estudio económico**: recursos humanos, materiales, temporalización y presupuesto.

---

## 2. Descripción general

### 2.1. Perspectiva del producto

**E-Cargo Hub** es un producto **independiente**, aunque se apoya en las siguientes APIs y servicios externos:

- **GraphHopper**: cálculo de rutas por carretera.
- **OpenStreetMap**: tiles del mapa base.

El producto está pensado para ser **auto-contenido** (backend + frontend + BBDD + Kafka + Redis) y desplegable en cualquier entorno con Docker.

### 2.2. Funcionalidad del producto

Las funcionalidades principales son:

1. **Gestión de vehículos**: CRUD de vehículos con nombre, matrícula y otros datos.
2. **Envío de comandos**: START, PAUSE, RESUME, STOP con coordenadas opcionales.
3. **Simulación de movimiento**: cálculo de ruta + emisión de telemetría.
4. **Monitorización en tiempo real**: mapa Leaflet con marcadores y estelas.
5. **Histórico de telemetría**: persistencia y consulta de puntos.
6. **Eventos de flota**: notificaciones globales en vivo.
7. **Autenticación y autorización**: JWT con roles (pendiente de implementación).
8. **Paginación y filtros**: en endpoints de listado (pendiente de implementación).

### 2.3. Características de los usuarios

| Tipo de usuario | Formación | Habilidades | Actividades |
|---|---|---|---|
| **Administrador** | Técnico superior en informática | Manejo de sistemas web, gestión de usuarios | Control total del sistema: usuarios, vehículos, monitorización |
| **Operador** | Técnico en logística | Manejo de aplicaciones web | Enviar comandos, monitorizar vehículos, consultar histórico |
| **Visualizador** | Usuario general | Manejo básico de aplicaciones web | Solo consulta: ver vehículos, telemetría, eventos |

### 2.4. Restricciones

- **Interfaz web**: debe funcionar en navegadores modernos (Chrome, Firefox, Edge).
- **Hardware mínimo del servidor**: 4 GB RAM, 2 núcleos, 20 GB disco.
- **Hardware mínimo del cliente**: cualquier equipo con navegador moderno.
- **Stack tecnológico obligatorio**: Java + Spring Boot (backend), Angular (frontend), PostgreSQL (BBDD).
- **Restricción de rate limit**: GraphHopper plan gratuito (500 créditos/día).
- **Restricción de red**: el sistema depende de conexión a Internet para tiles de OpenStreetMap y GraphHopper.

### 2.5. Suposiciones y dependencias

- Se asume que los requisitos aquí descritos son **estables** durante el desarrollo.
- Se asume que el entorno de despliegue tiene **Docker** instalado.
- Se asume que el usuario tiene un **navegador moderno**.
- Dependencia externa: **GraphHopper API** (con fallback si falla).
- Dependencia externa: **OpenStreetMap tiles** (con fallback a consola offline).

### 2.6. Evolución previsible del sistema

- **Corto plazo** (TFG): autenticación JWT, CRUD de usuarios, paginación, frontend Angular completo.
- **Medio plazo**: persistencia de rutas, multi-tenant, panel de administración.
- **Largo plazo**: soporte multi-vehículo, integración con tráfico real, ML para predicción.

---

## 3. Requisitos específicos

### 3.1. Requisitos comunes de los interfaces

#### 3.1.1. Interfaces de usuario

- **Dashboard principal**: mapa interactivo con marcadores de vehículos, tabla de estado, panel de eventos.
- **Formulario de comandos**: botones START/PAUSE/RESUME/STOP por vehículo.
- **Página de login**: usuario/contraseña y botón "Entrar con Google".
- **Panel de administración**: gestión de usuarios y vehículos.

#### 3.1.2. Interfaces de hardware

- **Servidor**: cualquier máquina con Java 21 + Docker.
- **Cliente**: cualquier dispositivo con navegador moderno (móvil, tablet, PC).

#### 3.1.3. Interfaces de software

- **Backend ↔ Frontend**: REST (JSON) + WebSocket (STOMP).
- **Backend ↔ PostgreSQL**: JDBC vía Hibernate.
- **Backend ↔ Kafka**: cliente Java.
- **Backend ↔ Redis**: cliente Lettuce.
- **Backend ↔ GraphHopper**: REST vía `RestClient`.

#### 3.1.4. Interfaces de comunicación

- **HTTP/HTTPS**: para REST.
- **WebSocket (WS/WSS)**: para STOMP.
- **TCP**: para PostgreSQL, Kafka, Redis.

---

### 3.2. Requisitos funcionales

#### RF01 — Autenticación de usuarios

| Campo | Valor |
|---|---|
| **Número de requisito** | RF01 |
| **Nombre** | Autenticación de usuarios |
| **Tipo** | Requisito |
| **Fuente** | Mínimos del centro |
| **Prioridad** | Alta / Esencial |
| **Descripción** | Los usuarios deberán autenticarse con usuario y contraseña para acceder al sistema. La sesión se gestionará mediante JWT. |
| **Estado** | Planificado |

#### RF02 — Login con OAuth2

| Campo | Valor |
|---|---|
| **Número de requisito** | RF02 |
| **Nombre** | Login con OAuth2 (Google) |
| **Tipo** | Requisito |
| **Fuente** | Mínimos del centro |
| **Prioridad** | Alta / Esencial |
| **Descripción** | Los usuarios podrán autenticarse usando su cuenta de Google (OAuth2). |
| **Estado** | Planificado |

#### RF03 — Roles de usuario

| Campo | Valor |
|---|---|
| **Número de requisito** | RF03 |
| **Nombre** | Roles y permisos |
| **Tipo** | Requisito |
| **Fuente** | Mínimos del centro |
| **Prioridad** | Alta / Esencial |
| **Descripción** | El sistema tendrá tres roles: ADMIN, OPERADOR, VIEWER. Cada rol tendrá restricciones de acceso según sus permisos. |
| **Estado** | Planificado |

#### RF04 — CRUD de vehículos

| Campo | Valor |
|---|---|
| **Número de requisito** | RF04 |
| **Nombre** | CRUD de vehículos |
| **Tipo** | Requisito |
| **Fuente** | Mínimos del centro |
| **Prioridad** | Alta / Esencial |
| **Descripción** | El sistema permitirá crear, leer, actualizar y eliminar vehículos. |
| **Estado** | Implementado |

#### RF05 — CRUD de usuarios

| Campo | Valor |
|---|---|
| **Número de requisito** | RF05 |
| **Nombre** | CRUD de usuarios |
| **Tipo** | Requisito |
| **Fuente** | Mínimos del centro |
| **Prioridad** | Alta / Esencial |
| **Descripción** | El sistema permitirá crear, leer, actualizar y eliminar usuarios. |
| **Estado** | Planificado |

#### RF06 — Envío de comandos

| Campo | Valor |
|---|---|
| **Número de requisito** | RF06 |
| **Nombre** | Envío de comandos a vehículos |
| **Tipo** | Requisito |
| **Fuente** | Proyecto |
| **Prioridad** | Alta / Esencial |
| **Descripción** | El sistema permitirá enviar comandos START, PAUSE, RESUME y STOP a un vehículo concreto. |
| **Estado** | Implementado |

#### RF07 — Cálculo de rutas con GraphHopper

| Campo | Valor |
|---|---|
| **Número de requisito** | RF07 |
| **Nombre** | Cálculo de rutas reales |
| **Tipo** | Requisito |
| **Fuente** | Proyecto |
| **Prioridad** | Alta / Esencial |
| **Descripción** | El sistema calculará rutas reales por carretera usando GraphHopper, con caché LRU y fallback lineal. |
| **Estado** | Implementado |

#### RF08 — Simulación de movimiento

| Campo | Valor |
|---|---|
| **Número de requisito** | RF08 |
| **Nombre** | Simulación de movimiento del vehículo |
| **Tipo** | Requisito |
| **Fuente** | Proyecto |
| **Prioridad** | Alta / Esencial |
| **Descripción** | El sistema simulará el movimiento de un vehículo punto a punto, emitiendo telemetría cada segundo. |
| **Estado** | Implementado |

#### RF09 — Idempotencia de comandos

| Campo | Valor |
|---|---|
| **Número de requisito** | RF09 |
| **Nombre** | Idempotencia de comandos |
| **Tipo** | Requisito |
| **Fuente** | Proyecto |
| **Prioridad** | Alta / Esencial |
| **Descripción** | El sistema evitará procesar dos veces el mismo comando usando Redis con `SET NX` y TTL de 24h. |
| **Estado** | Implementado |

#### RF10 — Telemetría en tiempo real (WebSocket)

| Campo | Valor |
|---|---|
| **Número de requisito** | RF10 |
| **Nombre** | Telemetría en tiempo real |
| **Tipo** | Requisito |
| **Fuente** | Proyecto |
| **Prioridad** | Alta / Esencial |
| **Descripción** | El sistema emitirá telemetría en tiempo real vía WebSocket/STOMP, con canales separados por vehículo. |
| **Estado** | Implementado |

#### RF11 — Eventos globales de flota

| Campo | Valor |
|---|---|
| **Número de requisito** | RF11 |
| **Nombre** | Eventos globales de flota |
| **Tipo** | Requisito |
| **Fuente** | Proyecto |
| **Prioridad** | Media / Deseado |
| **Descripción** | El sistema emitirá eventos globales (`VEHICLE_STARTED`, `COMPLETED`, etc.) en el canal `/topic/fleet-status`. |
| **Estado** | Implementado |

#### RF12 — Histórico de telemetría

| Campo | Valor |
|---|---|
| **Número de requisito** | RF12 |
| **Nombre** | Histórico de telemetría |
| **Tipo** | Requisito |
| **Fuente** | Proyecto |
| **Prioridad** | Alta / Esencial |
| **Descripción** | El sistema persistirá cada punto de telemetría en PostgreSQL y permitirá consultarlo con filtros por fecha. |
| **Estado** | Implementado |

#### RF13 — Paginación de listados

| Campo | Valor |
|---|---|
| **Número de requisito** | RF13 |
| **Nombre** | Paginación de listados |
| **Tipo** | Requisito |
| **Fuente** | Mínimos del centro |
| **Prioridad** | Alta / Esencial |
| **Descripción** | Los endpoints de listado devolverán resultados paginados. |
| **Estado** | Planificado |

#### RF14 — Visualización en mapa

| Campo | Valor |
|---|---|
| **Número de requisito** | RF14 |
| **Nombre** | Visualización en mapa interactivo |
| **Tipo** | Requisito |
| **Fuente** | Proyecto |
| **Prioridad** | Alta / Esencial |
| **Descripción** | El frontend mostrará los vehículos sobre un mapa Leaflet, con marcadores y estelas. |
| **Estado** | En desarrollo |

#### RF15 — Documentación interactiva (Swagger)

| Campo | Valor |
|---|---|
| **Número de requisito** | RF15 |
| **Nombre** | Documentación interactiva de la API |
| **Tipo** | Requisito |
| **Fuente** | Proyecto |
| **Prioridad** | Media / Deseado |
| **Descripción** | El sistema expondrá Swagger UI con todos los endpoints documentados. |
| **Estado** | Implementado |

---

### 3.3. Requisitos no funcionales

#### RNF01 — Interfaz de usuario intuitiva

| Campo | Valor |
|---|---|
| **Número** | RNF01 |
| **Nombre** | Interfaz intuitiva |
| **Descripción** | La interfaz debe ser sencilla e intuitiva, con diseño moderno y responsive. |
| **Prioridad** | Alta |

#### RNF02 — Rendimiento

| Campo | Valor |
|---|---|
| **Número** | RNF02 |
| **Nombre** | Rendimiento |
| **Descripción** | El 95% de las peticiones REST deben responder en menos de 500 ms. Las actualizaciones WebSocket deben llegar en menos de 200 ms desde su emisión. |
| **Prioridad** | Alta |

#### RNF03 — Escalabilidad

| Campo | Valor |
|---|---|
| **Número** | RNF03 |
| **Nombre** | Escalabilidad |
| **Descripción** | El sistema debe poder escalar horizontalmente añadiendo instancias del consumer de Kafka. |
| **Prioridad** | Media |

#### RNF04 — Seguridad

| Campo | Valor |
|---|---|
| **Número** | RNF04 |
| **Nombre** | Seguridad |
| **Descripción** | Las comunicaciones deben usar HTTPS en producción. Las contraseñas se almacenarán con BCrypt. Los endpoints estarán protegidos por JWT. |
| **Prioridad** | Alta |

#### RNF05 — Disponibilidad

| Campo | Valor |
|---|---|
| **Número** | RNF05 |
| **Nombre** | Disponibilidad |
| **Descripción** | El sistema debe tener una disponibilidad del 99% en horario laboral. |
| **Prioridad** | Media |

#### RNF06 — Mantenibilidad

| Campo | Valor |
|---|---|
| **Número** | RNF06 |
| **Nombre** | Mantenibilidad |
| **Descripción** | El código debe estar documentado y seguir convenciones. Cobertura mínima de tests: 60%. |
| **Prioridad** | Alta |

#### RNF07 — Portabilidad

| Campo | Valor |
|---|---|
| **Número** | RNF07 |
| **Nombre** | Portabilidad |
| **Descripción** | El sistema debe desplegarse en cualquier entorno con Docker, sin dependencias del SO. |
| **Prioridad** | Alta |

#### RNF08 — Usabilidad

| Campo | Valor |
|---|---|
| **Número** | RNF08 |
| **Nombre** | Usabilidad |
| **Descripción** | El sistema debe ser usable desde cualquier navegador moderno, sin instalación. |
| **Prioridad** | Alta |

---

### 3.4. Otros requisitos

- **Requisitos legales**: cumplimiento del RGPD para datos de usuarios.
- **Requisitos culturales**: interfaz en español.
- **Requisitos de internacionalización**: preparado para añadir inglés en el futuro.

---

## 4. Modelo de negocio

### 4.1. Descripción del modelo

**E-Cargo Hub** se plantea como un **proyecto académico** (TFG) sin ánimo de lucro. No tiene modelo de negocio comercial.

### 4.2. Alternativas al modelo elegido

- **SaaS de pago**: suscripción mensual para empresas logísticas.
- **Freemium**: versión gratuita con límites + versión premium.
- **Open Source**: licencia MIT y comunidad de contribuidores.

### 4.3. Justificación del modelo elegido

Al tratarse de un TFG académico, se ha optado por **licencia de uso académico** sin ánimo de lucro, permitiendo su uso educativo y de investigación.

---

## 5. Estudio económico del proyecto

### 5.1. Recursos humanos

| Rol | Horas estimadas | Coste/hora (€) | Total (€) |
|---|---|---|---|
| Desarrollador (alumno) | 300 | 0 | 0 |
| Director del proyecto | 20 | 0 | 0 |
| **Total** | **320** | — | **0** |

### 5.2. Recursos materiales

| Recurso | Coste estimado (€) |
|---|---|
| Ordenador de desarrollo | 800 |
| Licencia IDE (IntelliJ / VS Code) | 0 (gratuito) |
| Hosting (Vercel + Render, plan gratuito) | 0 |
| Dominio (opcional) | 12 |
| **Total** | **812** |

### 5.3. Temporalización

| Fase | Duración | Fechas |
|---|---|---|
| Análisis y diseño | 3 semanas | Septiembre 2025 |
| Backend | 4 semanas | Octubre 2025 |
| Frontend | 4 semanas | Noviembre 2025 |
| Documentación y tests | 2 semanas | Noviembre-Diciembre 2025 |
| **Total** | **13 semanas** | Septiembre-Diciembre 2025 |

### 5.4. Presupuesto

#### 5.4.1. Desarrollo

| Concepto | Coste (€) |
|---|---|
| Recursos humanos | 0 |
| Recursos materiales | 812 |
| **Total desarrollo** | **812** |

#### 5.4.2. Mantenimiento

| Concepto | Coste anual (€) |
|---|---|
| Hosting | 0 (plan gratuito) |
| Dominio | 12 |
| **Total mantenimiento** | **12/año** |

---

## 6. Bibliografía

- IEEE Std 830-1998. *IEEE Recommended Practice for Software Requirements Specifications*. IEEE, 1998.
- Spring Boot Documentation. https://spring.io/projects/spring-boot
- Apache Kafka Documentation. https://kafka.apache.org/documentation/
- Angular Documentation. https://angular.io/docs
- GraphHopper Documentation. https://docs.graphhopper.com/
- PostgreSQL Documentation. https://www.postgresql.org/docs/
- Redis Documentation. https://redis.io/docs/