# Troubleshooting — E-Cargo Hub

Guía de solución de problemas comunes del sistema **E-Cargo Hub**. Cada entrada incluye el síntoma, la causa probable y la solución.

---

## Índice

1. [Problemas de arranque](#1-problemas-de-arranque)
2. [Problemas de Kafka](#2-problemas-de-kafka)
3. [Problemas de PostgreSQL](#3-problemas-de-postgresql)
4. [Problemas de Redis](#4-problemas-de-redis)
5. [Problemas de GraphHopper](#5-problemas-de-graphhopper)
6. [Problemas de WebSocket](#6-problemas-de-websocket)
7. [Problemas de CORS](#7-problemas-de-cors)
8. [Problemas de Jackson 3](#8-problemas-de-jackson-3)
9. [Problemas de Lombok](#9-problemas-de-lombok)
10. [Problemas de Docker](#10-problemas-de-docker)
11. [Problemas de Angular](#11-problemas-de-angular)
12. [Problemas de rendimiento](#12-problemas-de-rendimiento)

---

## 1. Problemas de arranque

### 1.1. El backend no arranca: `ClassFormatError`

**Síntoma:**

```
Handler dispatch failed; nested exception is java.lang.ClassFormatError:
Invalid method Code length 0 in class file com/ecargohub/backend/dto/...
```

**Causa:** Lombok 1.18.30 genera bytecode inválido con `@Builder` en records con JDK 21.

**Solución:**

1. **Actualizar Lombok** a `1.18.48` o superior en `pom.xml`:

```xml
<properties>
    <lombok.version>1.18.48</lombok.version>
</properties>
```

2. **O eliminar `@Builder` de los records** (los records ya tienen constructor canónico).

3. **Limpiar el `target`:**

```bash
./mvnw clean install
```

---

### 1.2. El backend no arranca: `No enum constant SerializationFeature.write-dates-as-timestamps`

**Síntoma:**

```
Failed to bind properties under 'spring.jackson.serialization' to java.util.Map
```

**Causa:** estás usando Jackson 2 con `spring.jackson.serialization.write-dates-as-timestamps`, pero Spring Boot 4 usa Jackson 3 donde esa propiedad ya no existe.

**Solución:**

Elimina esa propiedad de `application.yml`:

```yaml
spring:
  jackson:
    # serialization:
    #   write-dates-as-timestamps: false   # ❌ ELIMINAR
    time-zone: Europe/Madrid
```

En Jackson 3, las fechas ya se serializan como ISO-8601 por defecto.

---

### 1.3. El backend no arranca: `Schema-validation: missing column`

**Síntoma:**

```
Schema-validation: missing column [xxx] in table [yyy]
```

**Causa:** tienes `ddl-auto: validate` y una entidad JPA no coincide con la tabla en BBDD.

**Solución:**

**Opción A (dev):** cambia temporalmente a `ddl-auto: update`:

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update
```

**Opción B (prod):** crea una migración Flyway que añada la columna:

```sql
-- V2__add_xxx_column.sql
ALTER TABLE yyy ADD COLUMN xxx VARCHAR(255);
```

---

### 1.4. El backend no arranca: `Ambiguous mapping`

**Síntoma:**

```
Ambiguous mapping. Cannot map 'xxxController' method ... to {GET /api/v1/...}:
There is already 'yyyController' bean method ...
```

**Causa:** dos métodos mapean la misma ruta en distintos controllers.

**Solución:**

1. Mira los logs al arrancar y busca la ruta duplicada.
2. Consulta `/actuator/mappings` para ver todas las rutas registradas.
3. Elimina o renombra uno de los métodos.

---

## 2. Problemas de Kafka

### 2.1. `No active consumer groups found` en Kafka UI

**Síntoma:** Kafka UI muestra 0 consumer groups.

**Causa:** el `@KafkaListener` no está arrancando.

**Solución:**

1. **Verifica `@EnableKafka`** en tu `KafkaConfig`:

```java
@Configuration
@EnableKafka
public class KafkaConfig { ... }
```

2. **Mira los logs al arrancar**. Debe aparecer:

```
Subscribed to topic(s): vehicle-commands
partitions assigned: [vehicle-commands-0]
```

3. **Si no aparece**, revisa el `containerFactory` del `@KafkaListener`:

```java
@KafkaListener(
    topics = "vehicle-commands",
    groupId = "e-cargo-hub-consumers",
    containerFactory = "stringKafkaListenerContainerFactory"
)
```

4. **Verifica que el bean `stringKafkaListenerContainerFactory` exista.**

---

### 2.2. El producer publica pero el consumer no recibe

**Síntoma:** en los logs ves `✅ Mensaje confirmado por Kafka en segundo plano: offset=X`, pero no ves `📥 ¡EVENTO DETECTADO EN KAFKA!`.

**Causa:** el listener no está arrancando.

**Solución:**

1. **Reinicia el backend** para asegurarte de que el listener arranca.
2. **Verifica el `groupId`**. Si dos apps usan el mismo grupo, solo una recibe.
3. **Verifica el `auto.offset.reset`**. Si es `latest` y el consumer arranca después de que se publique, no lo verá:

```java
props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
```

4. **Consulta los offsets en Kafka UI:** Topics → `vehicle-commands` → Consumers.

---

### 2.3. `Minutely API limit heavily violated` (GraphHopper)

**Síntoma:** GraphHopper devuelve error 429.

**Causa:** has superado el rate limit por minuto del plan gratuito.

**Solución:**

- **La caché LRU** ya implementada reduce las llamadas.
- **Si sigue pasando**, revisa que la caché esté funcionando. En los logs debes ver:

```
📦 Ruta servida desde caché: 38.1408,-0.8844->37.9922,-1.1307
```

- **Si no ves eso**, la caché no está activa o las coordenadas son ligeramente distintas.

---

### 2.4. `TimeoutException: Topic vehicle-commands not present in metadata`

**Síntoma:** al publicar, timeout.

**Causa:** el topic no existe y `auto.create.topics.enable=false` en el broker.

**Solución:**

Crea el topic manualmente:

```bash
docker exec -it simulador-kafka kafka-topics.sh \
  --bootstrap-server localhost:9092 \
  --create \
  --topic vehicle-commands \
  --partitions 1 \
  --replication-factor 1
```

O actívalo en el broker (no recomendado en producción).

---

## 3. Problemas de PostgreSQL

### 3.1. `Connection refused` al arrancar el backend

**Síntoma:** el backend no conecta a Postgres.

**Causa:** el contenedor no está corriendo o el puerto está mal.

**Solución:**

1. **Verifica el contenedor:**

```bash
docker ps | grep simulador-postgres
```

2. **Verifica el puerto:**

```bash
ss -tlnp | grep 5432
```

3. **Verifica la URL en `application.yml`:**

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/simulador_logistico
```

4. **Prueba la conexión:**

```bash
docker exec -it simulador-postgres psql -U simulador -d simulador_logistico -c "SELECT 1"
```

---

### 3.2. `DataIntegrityViolationException` con `uk_command_id`

**Síntoma:**

```
ERROR: duplicate key value violates unique constraint "uk_command_id"
```

**Causa:** has intentado insertar un `command_id` duplicado.

**Solución:**

- **En producción:** esto **no debería pasar** porque el `commandId` se genera en `@PrePersist`.
- **En tests manuales:** si estás insertando comandos a mano con el mismo `commandId`, genera uno nuevo.
- **Si el `commandId` viene del cliente**, valida que sea único.

---

### 3.3. `table "xxx" does not exist`

**Síntoma:** el backend arranca pero al ejecutar una query falla.

**Causa:** la tabla no existe (Hibernate no la creó).

**Solución:**

1. **Verifica `ddl-auto`:**

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update   # en dev, para que cree tablas
```

2. **Reinicia el backend.** Hibernate debe crear la tabla.

3. **Si no la crea, verifica que la entidad tenga `@Entity` y esté en un paquete escaneado.**

---

## 4. Problemas de Redis

### 4.1. `RedisConnectionFailureException`

**Síntoma:** el backend no conecta a Redis.

**Causa:** Redis no está corriendo o la config está mal.

**Solución:**

1. **Verifica el contenedor:**

```bash
docker ps | grep simulador-redis
```

2. **Verifica la conexión:**

```bash
docker exec -it simulador-redis redis-cli PING
# Respuesta: PONG
```

3. **Verifica la config en `application.yml`** (¡ojo con la indentación!):

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
```

**⚠️ Error común:** la sección `data.redis` **debe estar dentro de `spring`**, no de otra raíz. Un error de indentación hace que Spring Boot ignore la config y use los defaults (que casualmente coinciden con `localhost:6379`).

---

### 4.2. La idempotencia no funciona

**Síntoma:** el mismo comando se procesa dos veces.

**Causa:** Redis no está guardando las claves o las guarda sin TTL.

**Solución:**

1. **Verifica las claves:**

```bash
docker exec -it simulador-redis redis-cli KEYS "e-cargo-hub:*"
```

2. **Verifica el TTL:**

```bash
docker exec -it simulador-redis redis-cli TTL "e-cargo-hub:processed-command:47a1c31b-..."
# Debe devolver un número cercano a 86400 (24h)
```

3. **Si no hay claves**, revisa el `IdempotencyService`:

```java
Boolean wasSet = redis.opsForValue().setIfAbsent(key, "1", TTL);
```

---

## 5. Problemas de GraphHopper

### 5.1. `Minutely API limit heavily violated`

**Síntoma:** error 429 en los logs.

**Solución:** ver [2.3](#23-minutely-api-limit-heavily-violated-graphhopper).

---

### 5.2. `Usando ruta de contingencia en línea recta`

**Síntoma:**

```
WARN c.e.b.service.geo.GraphHopperService : Usando ruta de contingencia en línea recta
```

**Causa:** GraphHopper falló (rate limit, sin Internet, API key inválida).

**Solución:**

- **No es un bug**, es el fallback funcionando. El sistema sigue operando con ruta lineal.
- **Si quieres reducir la frecuencia**, verifica la caché.
- **Si quieres eliminar el warning**, cambia el nivel de log a `DEBUG`:

```yaml
logging:
  level:
    com.ecargohub.backend.service.geo.GraphHopperService: ERROR
```

---

### 5.3. La API key de GraphHopper no funciona

**Síntoma:** error 401 de GraphHopper.

**Solución:**

1. **Verifica la API key** en https://graphhopper.com/dashboard/.
2. **Actualiza la variable de entorno:**

```yaml
graphhopper:
  api:
    key: ${GRAPHHOPPER_API_KEY:tu-nueva-key}
```

---

## 6. Problemas de WebSocket

### 6.1. `WebSocket connection failed`

**Síntoma:** el frontend no conecta al WebSocket.

**Causa:** CORS, URL incorrecta o backend no arrancado.

**Solución:**

1. **Verifica que el backend esté arrancado:** `curl http://localhost:8081/actuator/health`.

2. **Verifica la URL del WebSocket** en el frontend:

```javascript
const wsUri = "ws://localhost:8081/ws-native";
```

3. **Verifica CORS en el backend:**

```java
registry.addEndpoint("/ws-native")
        .setAllowedOriginPatterns("http://localhost:4200", "http://localhost");
```

4. **Reinicia el backend** si has cambiado el `WebSocketConfig`.

---

### 6.2. `Handshake failed due to invalid Upgrade header`

**Síntoma:** el navegador no completa el handshake WebSocket.

**Causa:** Nginx no está pasando los headers de upgrade.

**Solución (si usas Nginx):**

```nginx
location /ws {
    proxy_pass http://localhost:8081;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
    proxy_set_header Host $host;
}
```

---

### 6.3. El WebSocket conecta pero no llegan mensajes

**Síntoma:** ves `CONNECTED` pero no `MESSAGE`.

**Causa:** no te has suscrito al canal correcto.

**Solución:**

1. **Verifica la suscripción:**

```
SUBSCRIBE
id:sub-1
destination:/topic/vehicle-status/1
ack:auto

\x00
```

2. **Verifica que el backend emite al canal correcto**:

```java
messagingTemplate.convertAndSend("/topic/vehicle-status/" + vehicleId, telemetry);
```

3. **Si el canal es dinámico** (`/topic/vehicle-status/{id}`), asegúrate de suscribirte con el ID correcto.

---

## 7. Problemas de CORS

### 7.1. `Access to fetch ... has been blocked by CORS policy`

**Síntoma:** el frontend no puede llamar al backend.

**Solución:**

1. **Verifica `CorsConfig`:**

```java
registry.addMapping("/api/**")
        .allowedOriginPatterns("http://localhost:4200", "http://localhost")
        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
        .allowedHeaders("*")
        .allowCredentials(true);
```

2. **Verifica el `application.yml`:**

```yaml
app:
  cors:
    allowed-origins: >
      http://localhost:4200,
      http://localhost,
      http://127.0.0.1
```

3. **Prueba con curl:**

```bash
curl -i -X OPTIONS http://localhost:8081/api/v1/vehicles \
  -H "Origin: http://localhost:4200" \
  -H "Access-Control-Request-Method: GET"
```

Debe devolver `Access-Control-Allow-Origin: http://localhost:4200`.

---

### 7.2. CORS funciona en GET pero no en POST con JSON

**Causa:** falta el header `Content-Type` en el preflight OPTIONS.

**Solución:** añade `allowedHeaders("*")` en la config de CORS.

---

## 8. Problemas de Jackson 3

### 8.1. `No enum constant tools.jackson.databind.SerializationFeature.xxx`

**Causa:** estás usando una propiedad de Jackson 2 en un proyecto con Jackson 3.

**Solución:** revisa `application.yml` y elimina propiedades obsoletas.

---

### 8.2. Las fechas se serializan como números

**Síntoma:** `"createdAt": 1790756789000` en lugar de ISO-8601.

**Causa:** falta el `JavaTimeModule` o el `ObjectMapper` no lo tiene registrado.

**Solución:**

En `KafkaConfig`:

```java
ObjectMapper mapper = new ObjectMapper();
mapper.registerModule(new JavaTimeModule());
mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
JsonSerializer<Object> serializer = new JsonSerializer<>(mapper);
```

**Con Jackson 3**, el `JavaTimeModule` es built-in, así que solo necesitas:

```java
JsonMapper mapper = JsonMapper.builder().build();
```

---

## 9. Problemas de Lombok

### 9.1. `ClassFormatError` con `@Builder` en records

**Solución:** ver [1.1](#11-el-backend-no-arranca-classformaterror).

---

### 9.2. Los getters/setters no se generan

**Síntoma:** el compilador no encuentra `getXxx()`.

**Causa:** Lombok no está como `annotationProcessor` en `pom.xml`.

**Solución:**

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPaths>
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
                <version>${lombok.version}</version>
            </path>
        </annotationProcessorPaths>
    </configuration>
</plugin>
```

---

## 10. Problemas de Docker

### 10.1. `OCI runtime exec failed: exec: "kafka-console-producer.sh": executable file not found`

**Síntoma:** al ejecutar comandos de Kafka en el contenedor.

**Causa:** la imagen `apache/kafka` no tiene el directorio `bin` en el PATH.

**Solución:** usa la ruta absoluta:

```bash
docker exec -it simulador-kafka /opt/kafka/bin/kafka-console-producer.sh \
  --bootstrap-server localhost:9092 \
  --topic vehicle-commands
```

O entra al contenedor y navega:

```bash
docker exec -it simulador-kafka bash
cd /opt/kafka/bin
./kafka-console-producer.sh --bootstrap-server localhost:9092 --topic vehicle-commands
```

---

### 10.2. `Port already in use`

**Síntoma:** Docker no puede exponer un puerto.

**Solución:**

1. **Verifica qué está usando el puerto:**

```bash
# Windows
netstat -ano | findstr :8081

# Linux/Mac
lsof -i :8081
```

2. **Mata el proceso o cambia el puerto en `docker-compose.yml`.**

---

### 10.3. `docker compose down` no elimina volúmenes

**Síntoma:** los datos persisten tras `down`.

**Causa:** los volúmenes no se eliminan por defecto.

**Solución:**

```bash
docker compose down -v   # ⚠️ elimina TODOS los volúmenes
```

---

## 11. Problemas de Angular

### 11.1. `Connection refused` al generar el cliente Angular

**Síntoma:**

```
Unable to read location `http://localhost:8081/v3/api-docs`
```

**Causa:** el backend no está arrancado.

**Solución:**

1. Arranca el backend: `./mvnw spring-boot:run`.
2. Verifica: `curl http://localhost:8081/v3/api-docs`.
3. Ejecuta el comando:

```bash
npx @openapitools/openapi-generator-cli generate \
  -i http://localhost:8081/v3/api-docs \
  -g typescript-angular \
  -o ./src/app/api
```

---

### 11.2. El cliente generado no tiene los modelos correctos

**Solución:** regenera el cliente:

```bash
npm run generate:api
```

Añade a `package.json`:

```json
{
  "scripts": {
    "generate:api": "openapi-generator-cli generate -i http://localhost:8081/v3/api-docs -g typescript-angular -o ./src/app/api"
  }
}
```

---

## 12. Problemas de rendimiento

### 12.1. El backend va lento

**Causas posibles:**

- **Demasiadas queries SQL**: activa `logging.level.org.hibernate.SQL=DEBUG` y revisa el N+1.
- **Pool de conexiones pequeño**: sube `hikari.maximum-pool-size`.
- **Consumer saturado**: sube `factory.setConcurrency(N)`.

**Soluciones:**

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
```

```java
factory.setConcurrency(4);   // 4 hilos de consumo
```

---

### 12.2. El WebSocket pierde mensajes

**Causa:** el buffer está lleno o el cliente es lento.

**Solución:**

- **Aumentar el buffer**:

```java
@Bean
public WebSocketMessageBrokerConfigurer webSocketConfigurer() {
    return new WebSocketMessageBrokerConfigurer() {
        @Override
        public void configureWebSocketTransport(WebSocketTransportRegistration registry) {
            registry.setSendBufferSizeLimit(512 * 1024);   // 512 KB
        }
    };
}
```

- **Reducir la frecuencia de emisión** (en lugar de 1s, cada 2s).

---

## 13. Contacto

Si encuentras un problema no documentado:

- Abre un [issue](https://github.com/jmgambin/E-Cargo-Hub/issues).
- Contacta con el autor: **Jose Manuel Gambin Manresa**.
- Director del proyecto: **Alejandro Brugarolas**.