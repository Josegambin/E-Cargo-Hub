# Guía de Despliegue — E-Cargo Hub

Este documento describe cómo desplegar el sistema **E-Cargo Hub** en diferentes entornos.

---

## 1. Entornos

| Entorno | Propósito | URL |
|---|---|---|
| **Local (dev)** | Desarrollo | http://localhost:8081 |
| **Staging** | Pruebas pre-producción | https://staging.ecargohub.example |
| **Producción** | Usuarios reales | https://ecargohub.example |

---

## 2. Requisitos

### 2.1. Hardware mínimo

| Recurso | Mínimo | Recomendado |
|---|---|---|
| CPU | 2 núcleos | 4 núcleos |
| RAM | 4 GB | 8 GB |
| Disco | 20 GB | 50 GB SSD |

### 2.2. Software

| Software | Versión mínima |
|---|---|
| Java | 21 |
| Docker | 24.x |
| Docker Compose | 2.x |
| Node.js | 20.x |
| PostgreSQL | 17 |
| Kafka | 4.x |
| Redis | 7.x |

---

## 3. Despliegue en local (dev)

### 3.1. Con Docker Compose (recomendado)

```bash
# 1. Clonar repositorio
git clone https://github.com/jmgambin/E-Cargo-Hub.git
cd E-Cargo-Hub

# 2. Levantar infraestructura
docker compose up -d simulador-postgres simulador-kafka simulador-kafka-ui simulador-redis

# 3. Arrancar backend
cd backend-java/E-Cargo-Hub
./mvnw spring-boot:run

# 4. Arrancar frontend (en otra terminal)
cd frontend-angular/simulador-logistico-web
npm install
npm start
```

### 3.2. Verificar

- Backend: http://localhost:8081/actuator/health
- Swagger: http://localhost:8081/swagger-ui.html
- Kafka UI: http://localhost:8085
- Frontend: http://localhost:4200

---

## 4. Despliegue con Docker Compose completo

### 4.1. Levantar todo el stack

```bash
docker compose up -d
```

Esto levanta:

- PostgreSQL (puerto 5432)
- Kafka (puerto 9092)
- Kafka UI (puerto 8085)
- Redis (puerto 6379)
- Backend Spring Boot (puerto 8081)
- Frontend Angular (puerto 4200)

### 4.2. Ver logs

```bash
docker compose logs -f backend
docker compose logs -f frontend
```

### 4.3. Parar todo

```bash
docker compose down              # Para pero conserva datos
docker compose down -v           # Para y borra volúmenes (¡cuidado!)
```

---

## 5. Variables de entorno

### 5.1. Backend

| Variable | Descripción | Ejemplo |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Perfil activo | `prod` |
| `DATABASE_URL` | URL JDBC de PostgreSQL | `jdbc:postgresql://db:5432/ecargohub` |
| `DATABASE_USER` | Usuario de BBDD | `ecargohub` |
| `DATABASE_PASSWORD` | Contraseña de BBDD | `***` |
| `KAFKA_BOOTSTRAP_SERVERS` | Servidores de Kafka | `kafka:9092` |
| `REDIS_HOST` | Host de Redis | `redis` |
| `REDIS_PORT` | Puerto de Redis | `6379` |
| `REDIS_PASSWORD` | Contraseña de Redis | `` |
| `GRAPHHOPPER_API_KEY` | API key de GraphHopper | `***` |
| `CORS_ALLOWED_ORIGINS` | Orígenes permitidos | `https://ecargohub.example` |

### 5.2. Frontend

| Variable | Descripción | Ejemplo |
|---|---|---|
| `API_URL` | URL del backend | `https://api.ecargohub.example` |
| `WS_URL` | URL del WebSocket | `wss://api.ecargohub.example/ws` |

### 5.3. Ejemplo `.env`

```bash
# .env
SPRING_PROFILES_ACTIVE=prod
DATABASE_URL=jdbc:postgresql://db:5432/ecargohub
DATABASE_USER=ecargohub
DATABASE_PASSWORD=changeme
KAFKA_BOOTSTRAP_SERVERS=kafka:9092
REDIS_HOST=redis
REDIS_PORT=6379
GRAPHHOPPER_API_KEY=tu-api-key
CORS_ALLOWED_ORIGINS=https://ecargohub.example
```

**⚠️ IMPORTANTE:** añade `.env` a `.gitignore`.

---

## 6. Despliegue en producción

### 6.1. Opción A: Docker Compose en VPS

**Recomendado para el TFG.**

**Pasos:**

1. **Provisionar VPS** (ej. Hetzner, DigitalOcean, OVH).
   - 2 vCPU, 4 GB RAM, 40 GB SSD.
   - Ubuntu 22.04 LTS.

2. **Instalar Docker y Docker Compose:**

```bash
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker $USER
```

3. **Clonar el repositorio:**

```bash
git clone https://github.com/jmgambin/E-Cargo-Hub.git
cd E-Cargo-Hub
```

4. **Configurar `.env` con valores de producción.**

5. **Configurar Nginx como reverse proxy:**

```nginx
server {
    listen 80;
    server_name ecargohub.example;

    location /api/ {
        proxy_pass http://localhost:8081;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    location /ws {
        proxy_pass http://localhost:8081;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }

    location / {
        proxy_pass http://localhost:4200;
    }
}
```

6. **HTTPS con Let's Encrypt:**

```bash
sudo apt install certbot python3-certbot-nginx
sudo certbot --nginx -d ecargohub.example
```

7. **Levantar el stack:**

```bash
docker compose up -d
```

### 6.2. Opción B: Vercel (solo frontend) + Render (backend)

**Frontend en Vercel:**

1. Conectar el repositorio a Vercel.
2. Configurar variables de entorno (`API_URL`, `WS_URL`).
3. Deploy automático en cada push.

**Backend en Render:**

1. Crear un servicio web.
2. Conectar el repositorio.
3. Configurar variables de entorno.
4. Comando de build: `./mvnw clean package -DskipTests`
5. Comando de start: `java -jar target/*.jar`

**BBDD en Render:**

1. Crear un PostgreSQL managed.
2. Copiar la URL JDBC a las variables de entorno del backend.

**Kafka y Redis:**

1. Usar servicios gestionados (Confluent Cloud, Upstash, Redis Cloud).
2. O desplegar en un VPS aparte.

---

## 7. Health checks y monitorización

### 7.1. Health checks

```bash
curl https://api.ecargohub.example/actuator/health
```

Respuesta esperada:

```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "redis": { "status": "UP" },
    "kafka": { "status": "UP" }
  }
}
```

### 7.2. Monitorización con Prometheus + Grafana

**Añadir a `pom.xml`:**

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

**Endpoint:** `/actuator/prometheus`

**Grafana:** importar dashboard predefinido o custom.

### 7.3. Logs

**En Docker:**

```bash
docker compose logs -f backend
```

**En fichero (producción):**

Los logs se escriben en `/var/log/e-cargo-hub/backend.log` con rotación automática.

---

## 8. Backups

### 8.1. PostgreSQL

**Backup manual:**

```bash
docker exec simulador-postgres pg_dump -U ecargohub ecargohub > backup_$(date +%Y%m%d).sql
```

**Backup automático (cron):**

```bash
# Añadir a crontab:
0 3 * * * docker exec simulador-postgres pg_dump -U ecargohub ecargohub | gzip > /backups/db_$(date +\%Y\%m\%d).sql.gz
```

**Restaurar:**

```bash
gunzip -c backup_20260930.sql.gz | docker exec -i simulador-postgres psql -U ecargohub ecargohub
```

### 8.2. Redis

Redis almacena solo la idempotencia de comandos. Si se pierde, no es crítico.

### 8.3. Kafka

Kafka almacena los comandos. Si se pierde, los comandos pendientes no se procesan.

---

## 9. Actualización del sistema

### 9.1. Actualizar backend

```bash
cd E-Cargo-Hub
git pull origin main
docker compose build backend
docker compose up -d backend
```

### 9.2. Actualizar frontend

```bash
cd frontend-angular/simulador-logistico-web
git pull origin main
npm install
npm run build
# Desplegar el contenido de dist/ a Vercel o al servidor
```

---

## 10. Rollback

Si una actualización rompe algo:

```bash
# Volver a la versión anterior
git checkout v1.0.0
docker compose build backend
docker compose up -d backend
```

---

## 11. Troubleshooting de despliegue

### Problema: Backend no arranca en Docker

**Solución:**

```bash
docker compose logs backend
# Buscar el error, probablemente falta una variable de entorno
```

### Problema: Frontend no conecta al backend

**Solución:**

1. Verifica `API_URL` en el `.env` del frontend.
2. Verifica CORS en el backend (`app.cors.allowed-origins`).
3. Verifica que el backend esté escuchando en `0.0.0.0` (no solo `127.0.0.1`).

### Problema: WebSocket no conecta

**Solución:**

1. Verifica que Nginx hace proxy de `/ws` con upgrade de headers.
2. Verifica que `setAllowedOriginPatterns` incluye el dominio del frontend.
3. Verifica el handshake con `wscat`:

```bash
wscat -c wss://api.ecargohub.example/ws
```

---

## 12. Seguridad en producción

- ✅ HTTPS obligatorio (Let's Encrypt).
- ✅ Contraseñas fuertes en BBDD y Redis.
- ✅ Firewall: solo 80, 443, y SSH abiertos.
- ✅ SSH con clave pública (no contraseña).
- ✅ Actualizaciones de seguridad automáticas.
- ✅ Backups diarios cifrados.
- ✅ Logs monitorizados.
- ✅ Rate limiting en la API.
- ✅ JWT para autenticación (planificado).

---

## 13. Checklist de despliegue

- [ ] VPS provisionado
- [ ] Docker y Docker Compose instalados
- [ ] Repositorio clonado
- [ ] `.env` configurado con valores de producción
- [ ] Nginx como reverse proxy
- [ ] HTTPS con Let's Encrypt
- [ ] Firewall configurado
- [ ] Backups automáticos
- [ ] Logs rotando
- [ ] Health checks pasando
- [ ] Monitorización activa
- [ ] DNS apuntando al servidor