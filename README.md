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
docker compose -f infrastructure/docker-compose.yml up -d
```

Servicios disponibles:

| Servicio | URL |
|---|---|
| Backend API | http://localhost:8081 |
| Consola HTML de pruebas (backend) | http://localhost:8081/test-console/ |
| Consola HTML de pruebas (XAMPP) | http://localhost/2%20DAW/DWC/test-console/ |
| Swagger UI | http://localhost:8081/swagger-ui.html |
| Frontend Angular | http://localhost:4200 |
| Keycloak (solo desarrollo local) | http://localhost:8180 |
| Kafka UI | http://localhost:8085 |
| PostgreSQL | localhost:5432 |
| Redis | localhost:6379 |

### Acceso y roles

La consola HTML de pruebas y el dashboard Angular usan OIDC con Keycloak: Authorization Code + PKCE (S256). La API valida los JWT y aplica los roles `ADMIN` y `VIEWER`; ocultar una opción en el cliente no sustituye la autorización del backend.

1. Inicia la infraestructura con `docker compose up -d`. Keycloak importa el realm `e-cargo-hub` desde `infrastructure/keycloak/realm-export.json`.
2. En la consola de Keycloak (`http://localhost:8180`, usuario local `admin`, contraseña `local-only-change-me`), crea usuarios y asígnales los roles de realm `ADMIN` o `VIEWER`. No se incluyen usuarios de aplicación ni contraseñas de demostración en el repositorio. El rol `VIEWER` solo permite lecturas; `ADMIN` permite operaciones de gestión. Si Keycloak ya tenía importado el realm, añade en el cliente `ecargo-dashboard` el URI de redirección `http://localhost:8081/test-console/` y el origen web `http://localhost:8081`; la importación inicial no modifica un realm existente.
3. El cliente público `ecargo-dashboard` solo admite login estándar con PKCE. No configures un client secret en Angular. Para producción, cambia el URL de Keycloak, realm y client ID de `environment.prod.ts`, configura los URI exactos de redirección/orígenes del dashboard en Keycloak y fija `ECARGO_CORS_ALLOWED_ORIGINS` con los dominios reales.

Las credenciales están separadas por propósito:

- **Dashboard:** JWT de usuario emitido por Keycloak. No se guarda un token de control compartido en el navegador.
- **Android/pantalla de vehículo:** código de pairing de un solo uso y token propio del dispositivo, limitado a consultar `/api/v1/device/state`.
- **`ECARGO_CONTROL_API_TOKEN`:** solo para clientes de confianza servidor-a-servidor. No lo incluyas en Angular, Android ni en archivos de entorno publicados.

La configuración Keycloak de Docker Compose usa `start-dev` y una contraseña de desarrollo por defecto: es exclusivamente local y no debe reutilizarse en producción.

### Arrancar en desarrollo (sin Docker)

**1. Levantar la infraestructura:**

```bash
docker compose -f infrastructure/docker-compose.yml up -d postgres kafka kafka-ui simulador-redis keycloak
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

### Consola HTML para pruebas del backend (sin Angular)

Arranca el backend y Keycloak. Puedes abrir la consola servida por Spring en `http://localhost:8081/test-console/` o copiar `backend-java/E-Cargo-Hub/src/main/resources/static/test-console/` a XAMPP:

- `C:\xampp\htdocs\2º DAW\DWC\test-console\` → `http://localhost/2%C2%BA%20DAW/DWC/test-console/`
- `C:\xampp\htdocs\2 DAW\DWC\test-console\` → `http://localhost/2%20DAW/DWC/test-console/`

La consola XAMPP se sirve en el puerto 80, pero conecta con la API en `http://localhost:8081` y Keycloak en `http://localhost:8180`. Si el realm ya existía antes de importar `infrastructure/keycloak/realm-export.json`, añade al cliente `ecargo-dashboard` los URI de redirección que correspondan a la carpeta usada y el origen web `http://localhost`. Incluye ambos orígenes localhost en `ECARGO_CORS_ALLOWED_ORIGINS` si defines esa variable manualmente.

El flujo es:

- Inicia sesión con usuario y contraseña en Keycloak mediante Authorization Code + PKCE. La contraseña nunca se introduce en la consola.
- Al volver, la consola consulta `/api/v1/vehicles` con `Authorization: Bearer <access_token>` y muestra si el backend acepta el token.
- Visualización del usuario/roles y cierre de sesión.
- Resumen de vehículos y rutas, estados, comandos de simulación, conexión STOMP autenticada y vinculación de dispositivos.
- Explorador generado desde `/v3/api-docs`, con parámetros y ejemplos de cuerpos editables para las operaciones publicadas.
- Enlace a Swagger y acceso a la consola local de usuarios Keycloak.

La consola no crea usuarios. Créales en Keycloak y asigna `ADMIN` o `VIEWER`. El token solo se mantiene en memoria; el botón de copia lo pone en el portapapeles de forma explícita para usarlo con Postman. El token dura cinco minutos por defecto y no debe guardarse ni compartirse.

### Postman

Importa `postman/E-Cargo-Hub.postman_collection.json` y `postman/E-Cargo-Hub.postman_environment.json`. Inicia sesión en la consola HTML y copia el access token al valor local de `accessToken` del entorno Postman. La colección usa el endpoint actual `/api/v1/vehicles/{vehicleId}/commands`; el token de control servidor-a-servidor no se debe poner en el navegador ni en Postman para probar como usuario. Ejecuta las peticiones de preparación antes de las que usan `vehicleId` o `routeId`. Las operaciones de control y borrado pueden cambiar o eliminar datos reales del entorno seleccionado.

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
| `docs/AAOS_INTEGRATION.md` | Puente Android Automotive, sensores y control de simulaciones |
| `docs/TROUBLESHOOTING.md` | Solución a problemas comunes |
| `docs/DOSSIER.md` | Dossier técnico extenso del proyecto |
| `CHANGELOG.md` | Historial de cambios por versión |
| `CONTRIBUTING.md` | Guía de contribución |

**API interactiva:** Swagger UI en http://localhost:8081/swagger-ui.html

**Pruebas Postman:** sigue [postman/README.md](./postman/README.md) para importar la colección y el entorno.

---

## 📡 API — Ejemplos rápidos

### Enviar un comando START

```http
POST /api/v1/vehicles/1/commands
Content-Type: application/json

{
  "command": "START",
  "routeId": 1
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
  connectHeaders: { Authorization: `Bearer ${accessToken}` },
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