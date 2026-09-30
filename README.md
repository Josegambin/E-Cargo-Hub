# 🚚 E-Cargo Hub

### Sistema de Simulación Logística en Tiempo Real

**Proyecto de Fin de Grado — Ciclo Formativo de Grado Superior en Desarrollo de Aplicaciones Web**

![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.8-brightgreen)
![Angular](https://img.shields.io/badge/Angular-18-red)
![Kafka](https://img.shields.io/badge/Apache%20Kafka-4.1-black)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-blue)
![Redis](https://img.shields.io/badge/Redis-7-red)
![Docker](https://img.shields.io/badge/Docker-Compose-blue)
![License](https://img.shields.io/badge/License-Uso%20Acad%C3%A9mico-lightgrey)

---

## 📖 Descripción

**E-Cargo Hub** es un sistema de información web para la **simulación y monitorización en tiempo real** de flotas de vehículos logísticos. Permite enviar comandos de control (arrancar, pausar, reanudar, detener), visualizar el movimiento de los vehículos sobre un mapa interactivo, consultar el histórico de telemetría y reaccionar a eventos de flota en vivo.

El proyecto implementa una **arquitectura orientada a eventos** con Apache Kafka como bus de mensajería, WebSocket/STOMP para la comunicación en tiempo real con el frontend, y Redis para garantizar la idempotencia del procesamiento de comandos.

---

## ✨ Características principales

- 🚀 **Control de vehículos en tiempo real** — comandos START, PAUSE, RESUME, STOP.
- 🗺️ **Rutas reales por carretera** — integración con GraphHopper, con caché y fallback lineal.
- 📡 **Telemetría en vivo** — WebSocket/STOMP con canales por vehículo.
- 📊 **Eventos globales de flota** — notificaciones en vivo.
- 💾 **Histórico completo de telemetría** — cada punto queda persistido en PostgreSQL.
- 🔒 **Idempotencia garantizada** — Redis evita procesar dos veces el mismo comando.
- 🎯 **Manejo de errores uniforme** — `GlobalExceptionHandler` con códigos HTTP coherentes.
- 📚 **Documentación viva** — Swagger UI + documentación completa en `/docs`.
- 🐳 **Despliegue reproducible** — Docker Compose levanta todo el stack con un comando.

---

## 🏗️ Arquitectura

```
Frontend Angular ──REST────► Spring Boot API ──► PostgreSQL
        │                          │
        │                          ▼
        └──WebSocket STOMP────► Apache Kafka ──► Kafka Consumer
                                       │              │
                                       │              ├──► GraphHopper (rutas)
                                       │              ├──► PostgreSQL (telemetría)
                                       │              └──► Redis (idempotencia)
                                       │
                                       └──────────────► Emite eventos (WebSocket)
```

**Diagrama detallado:** ver `ARCHITECTURE.md`.

---

## 🛠️ Stack tecnológico

### Backend

| Componente | Versión | Propósito |
|---|---|---|
| Java | 21 | Lenguaje principal |
| Spring Boot | 4.0.8 | Framework de aplicación |
| Spring Kafka | 4.0.x | Integración con Kafka |
| Spring Data JPA | 3.x | Persistencia |
| Spring WebSocket | — | Comunicación en tiempo real |
| Hibernate | 7.2 | ORM |
| Jackson | 3.x | Serialización JSON |
| Lombok | 1.18.42 | Reducción de boilerplate |
| MapStruct | 1.6.3 | Mapeo de DTOs |
| SpringDoc OpenAPI | 2.8.5 | Documentación Swagger |

### Frontend

| Componente | Versión | Propósito |
|---|---|---|
| Angular | 18+ | Framework SPA |
| TypeScript | 5.x | Lenguaje tipado |
| Leaflet | 1.9 | Mapas interactivos |
| @stomp/stompjs | 7.x | Cliente WebSocket STOMP |
| Angular Material | 18+ | Componentes UI |

### Infraestructura

| Componente | Versión | Propósito |
|---|---|---|
| PostgreSQL | 17 | Base de datos relacional |
| Apache Kafka | 4.1 | Bus de eventos |
| Redis | 7 | Caché e idempotencia |
| Kafka UI | 0.7.x | Interfaz web para Kafka |
| Docker Compose | — | Orquestación |

### APIs externas

| Servicio | Propósito |
|---|---|
| GraphHopper | Cálculo de rutas por carretera |

---

## 🚀 Inicio rápido

### Requisitos previos

- **Java 21** o superior
- **Node.js 20+** y **npm**
- **Docker Desktop**
- **Git**
- **Angular CLI** (`npm install -g @angular/cli`)

### Arrancar el stack completo (Docker)

```bash
git clone https://github.com/jmgambin/E-Cargo-Hub.git
cd E-Cargo-Hub
docker compose up -d
```

Servicios disponibles:

| Servicio | URL |
|---|---|
| Backend API | http://localhost:8081 |
| Swagger UI | http://localhost:8081/swagger-ui.html |
| Frontend Angular | http://localhost:4200 |
| Kafka UI | http://localhost:8085 |
| PostgreSQL | localhost:5432 |
| Redis | localhost:6379 |

### Arrancar en desarrollo (sin Docker)

**1. Levantar la infraestructura:**

```bash
docker compose up -d simulador-postgres simulador-kafka simulador-kafka-ui simulador-redis
```

**2. Arrancar el backend:**

```bash
cd backend-java/E-Cargo-Hub
./mvnw spring-boot:run
```

**3. Arrancar el frontend:**

```bash
cd frontend-angular/simulador-logistico-web
npm install
npm start
```

**4. Abrir en el navegador:**

```
http://localhost:4200
```

---

## 📚 Documentación

Toda la documentación del proyecto está organizada en la carpeta `docs/`:

| Documento | Descripción |
|---|---|
| `docs/ERS.md` | Especificación de Requisitos Software (IEEE 830) |
| `ARCHITECTURE.md` | Diseño técnico y decisiones arquitectónicas |
| `docs/DATA_MODEL.md` | Modelo de datos (ERD, tablas, índices) |
| `docs/API.md` | Referencia completa de la API REST y WebSocket |
| `docs/DEPLOYMENT.md` | Guía de despliegue |
| `docs/TROUBLESHOOTING.md` | Solución a problemas comunes |
| `docs/DOSSIER.md` | Dossier técnico extenso del proyecto |
| `CHANGELOG.md` | Historial de cambios por versión |
| `CONTRIBUTING.md` | Guía de contribución |

**API interactiva:** Swagger UI en http://localhost:8081/swagger-ui.html

---

## 📡 API — Ejemplos rápidos

### Enviar un comando START

```http
POST /api/v1/vehicle-commands/vehicle/1
Content-Type: application/json

{
  "command": "START",
  "value": null,
  "originLat": 38.1408,
  "originLon": -0.8844,
  "destLat": 37.9922,
  "destLon": -1.1307
}
```

### Consultar el estado actual

```http
GET /api/v1/vehicles/1/status
```

### Consultar el histórico de telemetría

```http
GET /api/v1/vehicles/1/telemetry
```

### Conectar al WebSocket

```javascript
const client = new Client({
  webSocketFactory: () => new SockJS('http://localhost:8081/ws'),
  onConnect: () => {
    client.subscribe('/topic/vehicle-status/1', (msg) => {
      console.log('Telemetría:', JSON.parse(msg.body));
    });
    client.subscribe('/topic/fleet-status', (msg) => {
      console.log('Evento de flota:', JSON.parse(msg.body));
    });
  }
});
client.activate();
```

**Más ejemplos:** ver `docs/API.md`.

---

## 🧪 Tests

```bash
# Backend
cd backend-java/E-Cargo-Hub
./mvnw test

# Frontend
cd frontend-angular/simulador-logistico-web
npm test
```

---

## 🐳 Docker

```bash
docker compose up -d      # Levantar todo
docker compose down       # Parar todo
docker compose logs -f    # Ver logs
```

---

## 📄 Licencia

Este proyecto está licenciado bajo **Uso Académico**. Consulta el archivo `LICENSE` para más detalles.

---

## 👤 Autor

**Jose Manuel Gambin Manresa**

- Estudiante de **Ciclo Formativo de Grado Superior en Desarrollo de Aplicaciones Web (DAW)**
- Centro: **EFA El Campico** (Jacarilla, Alicante)
- Director del proyecto: **Alejandro Brugarolas**
- Curso académico: **2025-2026**

---

## 🙏 Agradecimientos

- **Alejandro Brugarolas** — director del proyecto, por su guía y apoyo.
- **EFA El Campico** — por proporcionar el entorno formativo y los recursos.
- **Comunidad Open Source** — por las herramientas y librerías utilizadas.

---

Hecho con ❤️ en Cox - Alicante, España