# Backend de E-Cargo Hub — Documentación Técnica

Este documento explica en profundidad **qué hace el backend de E-Cargo Hub**, qué tecnologías utiliza, para qué sirve cada una y por qué se eligió cada decisión técnica.

**Proyecto:** Sistema de Simulación Logística en Tiempo Real
**Autor:** Jose Manuel Gambin Manresa
**Director del proyecto:** Alejandro Brugarolas
**Centro:** EFA El Campico (Jacarilla, Alicante)
**Ciclo:** Desarrollo de Aplicaciones Web (DAW) — 2º Curso

---

## Índice

1. [Visión general del backend](#1-visión-general-del-backend)
2. [Arquitectura en capas](#2-arquitectura-en-capas)
3. [Stack tecnológico](#3-stack-tecnológico)
4. [Java 21](#4-java-21)
5. [Spring Boot 4](#5-spring-boot-4)
6. [Spring MVC (REST)](#6-spring-mvc-rest)
7. [Spring Data JPA + Hibernate](#7-spring-data-jpa--hibernate)
8. [PostgreSQL](#8-postgresql)
9. [Apache Kafka](#9-apache-kafka)
10. [Spring Kafka](#10-spring-kafka)
11. [Redis](#11-redis)
12. [Spring Data Redis](#12-spring-data-redis)
13. [WebSocket + STOMP](#13-websocket--stomp)
14. [Jackson 3](#14-jackson-3)
15. [Lombok](#15-lombok)
16. [MapStruct](#16-mapstruct)
17. [Bean Validation](#17-bean-validation)
18. [GraphHopper](#18-graphhopper)
19. [SpringDoc OpenAPI (Swagger)](#19-springdoc-openapi-swagger)
20. [Actuator](#20-actuator)
21. [Maven](#21-maven)
22. [Flujo completo de una petición](#22-flujo-completo-de-una-petición)
23. [Decisiones técnicas clave](#23-decisiones-técnicas-clave)
24. [Estructura de paquetes](#24-estructura-de-paquetes)

---

## 1. Visión general del backend

El backend de **E-Cargo Hub** es el **núcleo lógico** del sistema. Se encarga de:

1. **Exponer una API REST** para que el frontend Angular pueda:
   - Gestionar vehículos (CRUD).
   - Enviar comandos (START, PAUSE, RESUME, STOP).
   - Consultar estado actual e histórico de telemetría.

2. **Procesar comandos de forma asíncrona** mediante Apache Kafka:
   - Desacoplar el envío del procesamiento.
   - Garantizar que ningún comando se pierde.
   - Permitir escalar horizontalmente.

3. **Simular el movimiento de vehículos**:
   - Calcular rutas con GraphHopper (o fallback lineal).
   - Emitir telemetría punto a punto.

4. **Emitir eventos en tiempo real** al frontend:
   - WebSocket/STOMP para telemetría.
   - Canales por vehículo y canal global de flota.

5. **Persistir datos** en PostgreSQL:
   - Vehículos.
   - Comandos.
   - Estado actual.
   - Histórico de telemetría.

6. **Garantizar la idempotencia** con Redis:
   - Evitar procesar el mismo comando dos veces.

7. **Documentar la API** con Swagger/OpenAPI.

8. **Monitorizar la salud** del sistema con Actuator.

**En una frase:** el backend recibe comandos por REST, los publica en Kafka, los procesa de forma asíncrona, calcula rutas, simula movimiento, persiste en BBDD y emite telemetría en tiempo real al frontend.

---

## 2. Arquitectura en capas

El backend sigue el **patrón MVC en capas** (Model-View-Controller adaptado a APIs REST):

```
┌────────────────────────────────────────────────────────────┐
│                      CAPA DE PRESENTACIÓN                   │
│                     (Controllers + DTOs)                    │
│  - Expone endpoints REST                                    │
│  - Valida entradas con @Valid                               │
│  - Devuelve DTOs (nunca entidades)                          │
│  - Maneja errores con @RestControllerAdvice                 │
└──────────────────────────┬─────────────────────────────────┘
                           │
                           ▼
┌────────────────────────────────────────────────────────────┐
│                       CAPA DE NEGOCIO                       │
│                         (Services)                          │
│  - Lógica de negocio                                        │
│  - Orquestación de repositorios y servicios externos        │
│  - Gestión de transacciones                                 │
│  - Validaciones de dominio                                  │
└──────────────────────────┬─────────────────────────────────┘
                           │
                           ▼
┌────────────────────────────────────────────────────────────┐
│                     CAPA DE PERSISTENCIA                    │
│                    (Repositories + Entities)                │
│  - Acceso a PostgreSQL (Spring Data JPA)                    │
│  - Entidades mapeadas con Hibernate                         │
│  - Consultas derivadas y personalizadas                     │
└────────────────────────────────────────────────────────────┘
                           │
                           ▼
┌────────────────────────────────────────────────────────────┐
│                   CAPA DE INFRAESTRUCTURA                   │
│         (Kafka, Redis, WebSocket, GraphHopper)              │
│  - Comunicación con servicios externos                      │
│  - Publicación/consumo de eventos                           │
│  - Emisión de mensajes en tiempo real                       │
└────────────────────────────────────────────────────────────┘
```

**Ventajas del patrón en capas:**

- **Separación de responsabilidades**: cada capa tiene una función clara.
- **Testabilidad**: se pueden mockear capas enteras.
- **Mantenibilidad**: cambios localizados en una capa no afectan a las demás.
- **Escalabilidad**: cada capa puede escalar por separado.

---

## 3. Stack tecnológico

| Tecnología | Versión | Categoría | Propósito |
|---|---|---|---|
| **Java** | 21 | Lenguaje | Lenguaje base del backend |
| **Spring Boot** | 4.0.8 | Framework | Framework de aplicación |
| **Spring MVC** | 7.x | Web | API REST |
| **Spring Data JPA** | 3.x | Persistencia | Abstracción de acceso a datos |
| **Hibernate** | 7.2 | ORM | Mapeo objeto-relacional |
| **PostgreSQL** | 17 | BBDD | Base de datos relacional |
| **Apache Kafka** | 4.1 | Mensajería | Bus de eventos |
| **Spring Kafka** | 4.0.x | Integración | Cliente de Kafka |
| **Redis** | 7 | Caché | Idempotencia |
| **Spring Data Redis** | 3.x | Integración | Cliente de Redis |
| **Spring WebSocket** | 7.x | Tiempo real | Comunicación bidireccional |
| **STOMP** | 1.2 | Protocolo | Pub/sub sobre WebSocket |
| **Jackson** | 3.x | Serialización | JSON |
| **Lombok** | 1.18.42 | Utilidad | Reducción de boilerplate |
| **MapStruct** | 1.6.3 | Utilidad | Mapeo Entity ↔ DTO |
| **Bean Validation** | 3.x | Validación | Validación de DTOs |
| **GraphHopper** | API externa | Rutas | Cálculo de rutas reales |
| **SpringDoc OpenAPI** | 2.8.5 | Documentación | Swagger UI |
| **Actuator** | 3.x | Monitorización | Health checks |
| **Maven** | 3.9+ | Build | Gestión de dependencias |

---

## 4. Java 21

### ¿Qué es?

Java 21 es la **versión LTS** (Long-Term Support) más reciente del lenguaje Java, publicada en septiembre de 2023.

### ¿Qué aporta al proyecto?

Java 21 introduce **features modernas** que simplifican el código:

#### **1. Records** (Java 14+)

Sustituyen a las clases POJO con getters/setters/equals/hashCode. Perfectos para DTOs.

```java
public record VehicleCommandRequest(
    VehicleCommandTypeEnum command,
    Double value,
    Double originLat,
    Double originLon,
    Double destLat,
    Double destLon
) {}
```

**Antes** (Java 8) habrías necesitado 60+ líneas.

#### **2. Pattern Matching** (Java 17+)

Simplifica los `switch` y `instanceof`.

```java
switch (cmd) {
    case START  -> handleStart(command);
    case PAUSE  -> handlePause(command.vehicleId());
    case RESUME -> handleResume(command.vehicleId());
    case STOP   -> handleStop(command.vehicleId());
    default     -> log.warn("Comando desconocido: {}", cmd);
}
```

#### **3. Text Blocks** (Java 15+)

Strings multilínea sin escapes.

```java
String description = """
        Devuelve la última posición conocida del vehículo.
        Si no tiene estado, devuelve un DTO vacío con status IDLE.
        """;
```

#### **4. `var`** (Java 10+)

Inferencia de tipos locales.

```java
var entity = new VehicleCommandEntity();
```

#### **5. Virtual Threads** (Java 21, preview)

Concurrencia ligera. **Aún no usado en el proyecto**, pero preparado para el futuro.

### ¿Por qué Java 21 y no Java 17?

- Java 21 es la **última LTS**.
- Soporte a largo plazo (hasta 2028+).
- Requerido por **Spring Boot 4**.
- Mejor rendimiento y seguridad.

---

## 5. Spring Boot 4

### ¿Qué es?

Spring Boot es un **framework de aplicación** que simplifica la configuración y el arranque de aplicaciones Spring. Elimina la necesidad de configuración XML y proporciona:

- **Auto-configuración**: detecta dependencias y configura beans automáticamente.
- **Starters**: dependencias empaquetadas por funcionalidad.
- **Servidor embebido**: Tomcat integrado, sin necesidad de desplegar WAR.
- **Actuator**: endpoints de monitorización.
- **DevTools**: recarga en caliente durante el desarrollo.

### ¿Qué aporta al proyecto?

#### **1. Arranque simplificado**

```java
@SpringBootApplication
public class ECargoHubApplication {
    public static void main(String[] args) {
        SpringApplication.run(ECargoHubApplication.class, args);
    }
}
```

**Una sola clase** arranca todo el backend.

#### **2. Auto-configuración**

Spring Boot detecta:
- Que tienes PostgreSQL en el classpath → configura el `DataSource`.
- Que tienes Spring Kafka → configura el `KafkaTemplate` y el `ConsumerFactory`.
- Que tienes Spring Data Redis → configura el `RedisTemplate`.

**Tú solo pones las propiedades en `application.yml`.**

#### **3. Servidor embebido**

Tomcat viene integrado. El backend arranca con:

```bash
./mvnw spring-boot:run
```

Y escucha en el puerto 8081 (configurable).

#### **4. Actuator**

Añadiendo una dependencia, tienes endpoints como `/actuator/health` funcionando.

#### **5. Starters**

Los starters agrupan dependencias relacionadas:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webmvc</artifactId>
</dependency>
```

Esto trae: Spring MVC, Jackson, Tomcat, Validación, etc. **Sin ti de dependencias**.

### ¿Por qué Spring Boot 4 y no Spring Boot 3?

- Spring Boot 4 es la **última versión** (2025).
- Compatible con **Java 21**.
- Soporte para **Jakarta EE 10+**.
- Mejoras en **rendimiento** y **observabilidad**.
- **Jackson 3** integrado.

---

## 6. Spring MVC (REST)

### ¿Qué es?

Spring MVC es el módulo de Spring para construir **aplicaciones web** siguiendo el patrón **MVC** (Model-View-Controller). En APIs REST, la "Vista" desaparece y el Controller devuelve JSON.

### ¿Qué aporta al proyecto?

#### **1. Anotaciones para endpoints**

```java
@RestController
@RequestMapping("/api/v1/vehicles")
public class VehicleController {

    @GetMapping
    public List<VehicleDto> getAll() { ... }

    @GetMapping("/{id}")
    public VehicleDto getOne(@PathVariable Long id) { ... }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VehicleDto create(@Valid @RequestBody CreateVehicleRequest request) { ... }

    @PutMapping("/{id}")
    public VehicleDto update(@PathVariable Long id, @Valid @RequestBody UpdateVehicleRequest request) { ... }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { ... }
}
```

#### **2. Serialización automática**

Spring MVC convierte automáticamente:
- **Java → JSON** en las respuestas (con Jackson).
- **JSON → Java** en las peticiones.

No tienes que parsear nada manualmente.

#### **3. Validación automática**

Con `@Valid`, Spring aplica las validaciones de Bean Validation:

```java
public VehicleCommandDto dispatchCommand(
        @PathVariable Long vehicleId,
        @Valid @RequestBody VehicleCommandRequest request) {
    // Si request es inválido, Spring devuelve 400 automáticamente
}
```

#### **4. Manejo de errores centralizado**

Con `@RestControllerAdvice`, capturas todas las excepciones y devuelves respuestas uniformes:

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(...) { ... }
}
```

#### **5. CORS configurable**

Spring MVC permite configurar CORS de forma declarativa:

```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("http://localhost:4200")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS");
    }
}
```

### ¿Por qué Spring MVC y no WebFlux?

- **Spring MVC** es **imperativo y bloqueante**, más fácil de entender.
- **WebFlux** es **reactivo**, mejor para alta concurrencia.
- Para el alcance del TFG (pocos usuarios, pocas peticiones), **MVC es suficiente**.
- MVC tiene **más documentación y ejemplos** en internet.

---

## 7. Spring Data JPA + Hibernate

### ¿Qué son?

- **JPA (Java Persistence API)**: especificación estándar para ORM en Java.
- **Hibernate**: implementación de referencia de JPA.
- **Spring Data JPA**: abstracción sobre JPA que **elimina el boilerplate**.

### ¿Qué aportan al proyecto?

#### **1. Entidades declarativas**

```java
@Entity
@Table(name = "vehicle_commands")
public class VehicleCommandEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "command_id", nullable = false, unique = true)
    private UUID commandId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private VehicleEntity vehicle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private VehicleCommandTypeEnum command;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (commandId == null) commandId = UUID.randomUUID();
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }
}
```

#### **2. Repositorios sin implementación**

```java
public interface VehicleCommandRepository extends JpaRepository<VehicleCommandEntity, Long> {

    List<VehicleCommandEntity> findByVehicleIdOrderByCreatedAtDesc(Long vehicleId);

    Optional<VehicleCommandEntity> findByCommandId(UUID commandId);
}
```

**Spring genera la implementación automáticamente.** Tú solo declaras métodos.

#### **3. Consultas derivadas**

Spring Data JPA deduce la query del nombre del método:

- `findByVehicleIdOrderByCreatedAtDesc(Long vehicleId)` → `SELECT * FROM vehicle_commands WHERE vehicle_id = ? ORDER BY created_at DESC`

**No escribes SQL** para consultas simples.

#### **4. Transacciones declarativas**

```java
@Service
@Transactional
public class VehicleCommandServiceImpl implements VehicleCommandService {

    @Override
    public VehicleCommandDto sendCommand(Long vehicleId, VehicleCommandRequest request) {
        // Todo aquí es transaccional
    }
}
```

**Con una anotación** envuelves el método en una transacción.

#### **5. DDL automático (en desarrollo)**

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: update   # Crea/actualiza tablas al arrancar
```

Hibernate lee las entidades y crea las tablas. **En producción se usará Flyway**.

### ¿Por qué Spring Data JPA y no JDBC puro?

- **Menos código**: un repositorio de 3 líneas vs 50 con JDBC.
- **Type-safe**: errores en compilación, no en runtime.
- **Portabilidad**: cambiar de BBDD es cuestión de configuración.
- **Caché de primer nivel**: Hibernate cachea entidades.
- **Lazy loading**: carga relaciones solo cuando se necesitan.

### ¿Por qué Hibernate y no EclipseLink?

- **Hibernate** es la implementación de referencia.
- **Mayor comunidad** y documentación.
- **Mejor integración** con Spring Boot.

---

## 8. PostgreSQL

### ¿Qué es?

PostgreSQL es un **sistema de gestión de bases de datos relacional** (RDBMS) open source, conocido por su robustez, extensibilidad y cumplimiento de estándares.

### ¿Qué aporta al proyecto?

#### **1. Almacenamiento persistente**

Guarda:

- **Vehículos**: catálogo de camiones.
- **Comandos**: historial de START/PAUSE/RESUME/STOP.
- **Estado actual**: última posición conocida.
- **Telemetría**: histórico completo de puntos.

#### **2. Tipos de datos avanzados**

- **`UUID`**: para el `commandId` (idempotencia).
- **`TIMESTAMPTZ`**: timestamps con zona horaria.
- **`BIGSERIAL`**: IDs autoincrementales.
- **`DOUBLE PRECISION`**: coordenadas geográficas.

#### **3. Índices eficientes**

```sql
CREATE INDEX idx_telemetry_vehicle_time
    ON vehicle_telemetry (vehicle_id, recorded_at DESC);
```

**Consultas rápidas** por vehículo y fecha.

#### **4. Restricciones de integridad**

```sql
CONSTRAINT uk_command_id UNIQUE (command_id)
FOREIGN KEY (vehicle_id) REFERENCES vehicles(id) ON DELETE CASCADE
```

**Garantiza la integridad referencial.**

### ¿Por qué PostgreSQL y no MySQL?

- **Estándares**: PostgreSQL cumple SQL:2016 al 100%.
- **Tipos avanzados**: UUID, JSON, arrays, hstore.
- **Rendimiento**: mejor en consultas complejas.
- **Open source**: sin licencias comerciales.
- **Extensible**: PostGIS (geoespacial), pg_trgm (búsqueda).
- **Comunidad**: muy activa y madura.

---

## 9. Apache Kafka

### ¿Qué es?

Apache Kafka es una **plataforma distribuida de streaming de eventos** que funciona como un bus de mensajería de alto rendimiento, tolerante a fallos y escalable.

### ¿Qué aporta al proyecto?

#### **1. Desacoplamiento**

El **Controller** no espera a que la simulación termine. Solo publica el comando a Kafka y devuelve 200 OK al usuario.

```
Controller → Publica en Kafka → Devuelve 200 OK
                                    ↓
                            (En paralelo)
                                    ↓
                        Consumer procesa el comando
```

**Ventaja:** el usuario recibe respuesta **inmediata**, aunque la simulación tarde 20 segundos.

#### **2. Persistencia de eventos**

Kafka guarda los mensajes en disco durante un tiempo configurable (por defecto 7 días). Si el consumer cae, los mensajes **se conservan** y se procesan al reiniciar.

#### **3. Escalabilidad horizontal**

Se pueden añadir **más consumers** en el mismo grupo, y Kafka reparte las particiones. Escalado horizontal.

#### **4. Modelo Pub/Sub**

Múltiples consumers pueden leer el mismo topic:

- `consumer-1`: procesa comandos.
- `consumer-2`: audita comandos.
- `consumer-3`: envía notificaciones.

**Cada uno lee independientemente.**

#### **5. Topic principal del proyecto**

**`vehicle-commands`** — topic donde se publican todos los comandos.

**Particiones:** 1 (suficiente para el TFG).

**Grupo de consumidores:** `e-cargo-hub-consumers`.

**Formato del mensaje:** JSON del `VehicleCommandDto`.

**Ejemplo de mensaje:**

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

### ¿Por qué Kafka y no RabbitMQ?

- **Kafka** está diseñado para **eventos** (log append-only).
- **RabbitMQ** está diseñado para **colas** (punto a punto).
- **Kafka** permite **múltiples consumers** leyendo independientemente.
- **Kafka** persiste los mensajes durante días.
- **Kafka** es el **estándar de facto** para arquitecturas orientadas a eventos.
- **Kafka** es más **escalable** (millones de mensajes/segundo).

**Para este proyecto:** Kafka es más apropiado porque:

1. Queremos que los comandos **no se pierdan**.
2. Podemos añadir consumers para auditoría en el futuro.
3. Es el **estándar de la industria**.

---

## 10. Spring Kafka

### ¿Qué es?

Spring Kafka es el módulo de Spring que **integra Apache Kafka** con el ecosistema Spring. Proporciona abstracciones de alto nivel para producir y consumir mensajes.

### ¿Qué aporta al proyecto?

#### **1. `KafkaTemplate` para producir**

```java
@Service
public class KafkaProducerService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendVehicleCommand(VehicleCommandDto command) {
        Message<VehicleCommandDto> message = MessageBuilder
                .withPayload(command)
                .setHeader(KafkaHeaders.TOPIC, "vehicle-commands")
                .setHeader(KafkaHeaders.KEY, String.valueOf(command.vehicleId()))
                .build();

        kafkaTemplate.send(message).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("❌ Error publicando en Kafka: {}", ex.getMessage());
            } else {
                log.info("✅ Mensaje confirmado: offset={}", result.getRecordMetadata().offset());
            }
        });
    }
}
```

#### **2. `@KafkaListener` para consumir**

```java
@KafkaListener(
    topics = "vehicle-commands",
    groupId = "e-cargo-hub-consumers",
    containerFactory = "stringKafkaListenerContainerFactory"
)
public void listenVehicleCommands(String messageString) {
    // Procesar el mensaje
}
```

**Con una anotación** tienes un consumer funcionando.

#### **3. Configuración declarativa**

```java
@Configuration
@EnableKafka
public class KafkaConfig {

    @Bean
    public ProducerFactory<String, Object> producerFactory() { ... }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() { ... }

    @Bean
    public ConsumerFactory<String, String> consumerFactory() { ... }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> listenerContainerFactory() { ... }
}
```

**Todo con beans de Spring.** Sin XML.

#### **4. Serialización/deserialización**

Spring Kafka integra **Jackson** para serializar objetos Java a JSON automáticamente:

```java
configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
```

#### **5. Ack manual y control de offset**

```java
props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

@Bean
public ConcurrentKafkaListenerContainerFactory<String, String> factory() {
    factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
    return factory;
}
```

**Si el listener falla, el offset NO se commitea**, y Kafka reintenta el mensaje.

### ¿Por qué Spring Kafka y no el cliente nativo?

- **Menos código**: `@KafkaListener` vs crear un `KafkaConsumer` manual.
- **Integración con Spring**: DI, transacciones, etc.
- **Manejo de errores**: `DefaultErrorHandler`, `@RetryableTopic`.
- **Serialización automática**: no parsear JSON manualmente.
- **Testabilidad**: `@EmbeddedKafka` para tests.

---

## 11. Redis

### ¿Qué es?

Redis es un **almacén de estructuras de datos en memoria**, usado como base de datos, caché y broker de mensajes. Es **extremadamente rápido** (microsegundos).

### ¿Qué aporta al proyecto?

#### **1. Idempotencia de comandos**

**Problema:** ¿qué pasa si el mismo comando se procesa dos veces?

- Un reintento de Kafka.
- Un rebalanceo del consumer group.
- Un doble clic del usuario.
- Un bug que publica dos veces.

**Solución:** Redis con `SET NX` (Set if Not eXists).

```java
@Service
public class IdempotencyService {

    private static final String KEY_PREFIX = "e-cargo-hub:processed-command:";
    private static final Duration TTL = Duration.ofHours(24);

    public boolean tryMarkAsProcessed(UUID commandId) {
        String key = KEY_PREFIX + commandId;
        Boolean wasSet = redis.opsForValue().setIfAbsent(key, "1", TTL);
        return Boolean.TRUE.equals(wasSet);
    }
}
```

**Cómo funciona:**

1. Llega un comando con `commandId = X`.
2. Redis intenta `SET NX e-cargo-hub:processed-command:X 1 EX 86400`.
3. Si la clave **no existía** → devuelve `true` → procesar.
4. Si la clave **ya existía** → devuelve `false` → descartar.

**Atomicidad:** `SET NX` es **atómico**, así que incluso con múltiples consumers concurrentes, solo uno gana.

**TTL de 24h:** la clave se borra automáticamente después de 24 horas. No hay que limpiar manualmente.

#### **2. Almacenamiento en memoria**

- **Latencia:** microsegundos (vs milisegundos de PostgreSQL).
- **Throughput:** ~100.000 operaciones/segundo en una sola instancia.
- **Persistencia opcional:** Redis puede persistir a disco (AOF o RDB).

#### **3. Estructuras de datos**

Redis no es solo clave-valor. Tiene:
- Strings.
- Hashes.
- Lists.
- Sets.
- Sorted Sets.
- Streams.
- Bitmaps.
- HyperLogLog.

**Para este proyecto solo usamos Strings**, pero la flexibilidad está ahí.

### ¿Por qué Redis y no PostgreSQL para idempotencia?

| Criterio | Redis | PostgreSQL |
|---|---|---|
| **Velocidad** | Microsegundos | Milisegundos |
| **TTL automático** | Sí (`EXPIRE`) | No (job manual) |
| **Atomicidad** | `SET NX` atómico | Requiere `SELECT FOR UPDATE` |
| **Escalabilidad** | Muy alta | Limitada por disco |

**Conclusión:** Redis es **10-100x más rápido** para esta tarea.

---

## 12. Spring Data Redis

### ¿Qué es?

Spring Data Redis es el módulo de Spring que **integra Redis** con el ecosistema Spring. Proporciona:

- `RedisTemplate`: abstracción de alto nivel.
- `StringRedisTemplate`: para strings.
- Serialización automática.
- Pool de conexiones con Lettuce.

### ¿Qué aporta al proyecto?

#### **1. `StringRedisTemplate` para idempotencia**

```java
@Service
public class IdempotencyService {

    private final StringRedisTemplate redis;

    public boolean tryMarkAsProcessed(UUID commandId) {
        String key = "e-cargo-hub:processed-command:" + commandId;
        Boolean wasSet = redis.opsForValue().setIfAbsent(key, "1", Duration.ofHours(24));
        return Boolean.TRUE.equals(wasSet);
    }
}
```

**`StringRedisTemplate`** simplifica el uso de strings.

#### **2. Pool de conexiones con Lettuce**

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      timeout: 2s
      lettuce:
        pool:
          max-active: 8
          max-idle: 8
          min-idle: 0
```

**Lettuce** es el cliente por defecto, basado en Netty (asíncrono y no bloqueante).

#### **3. Serialización automática**

Aunque usamos strings, Spring Data Redis puede serializar objetos automáticamente con Jackson.

### ¿Por qué Spring Data Redis y no Jedis directo?

- **Integración con Spring**: DI, transacciones.
- **Lettuce** es más moderno que Jedis.
- **Pool de conexiones** gestionado por Spring.
- **Testabilidad**: `@DataRedisTest`.

---

## 13. WebSocket + STOMP

### ¿Qué son?

- **WebSocket**: protocolo de comunicación **bidireccional** sobre una sola conexión TCP.
- **STOMP** (Simple Text Oriented Messaging Protocol): protocolo de mensajería sobre WebSocket, con modelo **pub/sub**.

### ¿Qué aportan al proyecto?

#### **1. Comunicación en tiempo real**

A diferencia de REST (petición-respuesta), WebSocket permite que el **servidor envíe mensajes al cliente sin que este los pida**.

**En el proyecto:** el backend emite telemetría cada segundo al frontend.

#### **2. Modelo Pub/Sub con STOMP**

```
Backend publica en /topic/vehicle-status/1
                    ↓
        (todos los clientes suscritos)
                    ↓
        Frontend recibe el mensaje
```

**Multi-vehículo:** cada vehículo tiene su propio canal.

#### **3. Canales del proyecto**

| Canal | Propósito |
|---|---|
| `/topic/vehicle-status/{vehicleId}` | Telemetría de un vehículo concreto |
| `/topic/fleet-status` | Eventos globales de flota |

#### **4. Ejemplo de mensaje (telemetría)**

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

#### **5. Configuración Spring**

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-native")
                .setAllowedOriginPatterns("http://localhost:4200");
        registry.addEndpoint("/ws").withSockJS();
    }
}
```

#### **6. Emisión desde el servicio**

```java
messagingTemplate.convertAndSend(
    "/topic/vehicle-status/" + vehicleId,
    telemetry
);
```

**Una línea** emite a todos los suscritos.

### ¿Por qué WebSocket + STOMP y no SSE?

| Criterio | WebSocket + STOMP | SSE |
|---|---|---|
| **Dirección** | Bidireccional | Solo servidor→cliente |
| **Protocolos** | Múltiples canales | Un canal |
| **Pub/Sub** | Nativo con STOMP | Manual |
| **Soporte navegadores** | Excelente | Excelente |
| **Complejidad** | Media | Baja |

**Para el proyecto:** WebSocket + STOMP es **más apropiado** porque:

1. Necesitamos **múltiples canales** (uno por vehículo + uno global).
2. Podríamos querer **enviar comandos** por WebSocket en el futuro.
3. STOMP proporciona **pub/sub nativo**.

---

## 14. Jackson 3

### ¿Qué es?

Jackson es la librería de **serialización/deserialización JSON** más usada en Java. **Jackson 3** es la versión más reciente (2024+), con paquete renombrado a `tools.jackson`.

### ¿Qué aporta al proyecto?

#### **1. Serialización automática**

Spring Boot convierte automáticamente:
- **Entity/DTO → JSON** en las respuestas REST.
- **JSON → DTO** en las peticiones REST.
- **DTO → JSON** en los mensajes de Kafka.
- **DTO → JSON** en los mensajes WebSocket.

#### **2. Soporte nativo de `java.time`**

Jackson 3 incluye soporte integrado para `OffsetDateTime`, `LocalDate`, `Instant`, etc. **Sin necesidad de registrar `JavaTimeModule`** (a diferencia de Jackson 2).

```java
JsonMapper mapper = JsonMapper.builder().build();
// Ya soporta java.time
```

#### **3. Configuración en Spring Boot**

```yaml
spring:
  jackson:
    time-zone: Europe/Madrid
    # serialization:
    #   write-dates-as-timestamps: false   # Ya no hace falta en Jackson 3
```

### ¿Por qué Jackson 3 y no Jackson 2?

- **Jackson 3 es el futuro**: Spring Boot 4 lo integra por defecto.
- **Rendimiento mejorado**: especial en deserialización.
- **Soporte nativo de `java.time`**.
- **API más limpia**.
- **Migración obligatoria** en Spring Boot 4.

---

## 15. Lombok

### ¿Qué es?

Lombok es una **librería de anotaciones** que genera automáticamente código boilerplate (getters, setters, constructores, etc.) en tiempo de compilación.

### ¿Qué aporta al proyecto?

#### **1. `@Data`, `@Getter`, `@Setter`**

```java
@Entity
@Data
public class VehicleEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String plate;
}
```

`@Data` genera: getters, setters, `equals`, `hashCode`, `toString`.

**Sin Lombok:** ~50 líneas.
**Con Lombok:** 1 anotación.

#### **2. `@Slf4j`**

```java
@Slf4j
@Service
public class KafkaConsumerService {
    public void listen(String msg) {
        log.info("Mensaje: {}", msg);
    }
}
```

**Sin Lombok:**

```java
private static final Logger log = LoggerFactory.getLogger(KafkaConsumerService.class);
```

#### **3. `@RequiredArgsConstructor`**

```java
@Service
@RequiredArgsConstructor
public class VehicleCommandServiceImpl {
    private final VehicleRepository vehicleRepository;
    private final KafkaProducerService kafkaProducerService;
    // Constructor generado automáticamente
}
```

### Cuidado con `@Builder` en records

**En Java 21 con Lombok 1.18.30**, `@Builder` en records genera bytecode inválido (`ClassFormatError`).

**Solución:**

1. **Actualizar Lombok a 1.18.42+**.
2. **No usar `@Builder` en records** (los records ya tienen constructor canónico).

### ¿Por qué Lombok y no escribir el código a mano?

- **Menos código**: 1 anotación vs 50 líneas.
- **Menos errores**: no te olvidas de un getter.
- **Más legible**: la clase se centra en lo importante.
- **Estándar de facto**: usado en el 90% de proyectos Spring Boot.

---

## 16. MapStruct

### ¿Qué es?

MapStruct es un **generador de código** para mapear entre objetos Java (Entity ↔ DTO). Genera la implementación **en tiempo de compilación**.

### ¿Qué aporta al proyecto?

#### **1. Mapper declarativo**

```java
@Mapper(componentModel = "spring")
public interface VehicleCommandMapper {

    VehicleCommandDto toDto(VehicleCommandEntity entity);
}
```

**Una interfaz** y MapStruct genera la implementación.

#### **2. Mapeo automático por nombres**

Si los campos coinciden, no tienes que especificar nada:

```java
// Entity tiene: id, commandId, vehicleId, command, value, createdAt
// DTO tiene:    id, commandId, vehicleId, command, value, createdAt
// → MapStruct lo mapea automáticamente
```

#### **3. Mapeo personalizado**

```java
@Mapper(componentModel = "spring")
public interface VehicleCommandMapper {

    @Mapping(source = "vehicle.id", target = "vehicleId")
    VehicleCommandDto toDto(VehicleCommandEntity entity);
}
```

#### **4. Integración con Lombok**

Con `lombok-mapstruct-binding`, MapStruct detecta los getters/setters generados por Lombok.

### ¿Por qué MapStruct y no escribir mappers a mano?

- **Menos código**: interfaz de 5 líneas vs clase de 30.
- **Type-safe**: errores en compilación, no en runtime.
- **Rendimiento**: código generado (sin reflexión).
- **Mantenimiento**: si añades un campo, se mapea solo.

### ¿Por qué MapStruct y no ModelMapper?

| Criterio | MapStruct | ModelMapper |
|---|---|---|
| **Cuándo mapea** | Compilación | Runtime |
| **Rendimiento** | Excelente | Regular (reflexión) |
| **Type-safe** | Sí | No |
| **Errores** | Compilación | Runtime |

**MapStruct gana.**

---

## 17. Bean Validation

### ¿Qué es?

Bean Validation (Jakarta Validation) es una **especificación estándar** para validar objetos Java con anotaciones. Hibernate Validator es la implementación de referencia.

### ¿Qué aporta al proyecto?

#### **1. Validación declarativa**

```java
public record VehicleCommandRequest(

    @NotNull(message = "command is required")
    VehicleCommandTypeEnum command,

    Double value,

    @DecimalMin(value = "-90.0",  message = "originLat must be >= -90")
    @DecimalMax(value = "90.0",   message = "originLat must be <= 90")
    Double originLat,

    @DecimalMin(value = "-180.0", message = "originLon must be >= -180")
    @DecimalMax(value = "180.0",  message = "originLon must be <= 180")
    Double originLon

) {}
```

#### **2. Activación con `@Valid`**

```java
@PostMapping
public ResponseEntity<VehicleCommandDto> dispatchCommand(
        @PathVariable Long vehicleId,
        @Valid @RequestBody VehicleCommandRequest request) {
    // Si request es inválido, Spring devuelve 400 automáticamente
}
```

#### **3. Manejo de errores de validación**

```java
@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<Map<String, Object>> handleValidation(
        MethodArgumentNotValidException ex) {
    String msg = ex.getBindingResult().getFieldErrors().stream()
            .map(f -> f.getField() + ": " + f.getDefaultMessage())
            .reduce((a, b) -> a + "; " + b)
            .orElse("Validation failed");
    return ResponseEntity.badRequest().body(Map.of("message", msg));
}
```

### Anotaciones más usadas

| Anotación | Propósito |
|---|---|
| `@NotNull` | No puede ser null |
| `@NotEmpty` | No puede ser null ni vacío |
| `@NotBlank` | No puede ser null ni solo espacios |
| `@Size(min, max)` | Longitud entre min y max |
| `@Min`, `@Max` | Valor numérico mínimo/máximo |
| `@DecimalMin`, `@DecimalMax` | Valor decimal mínimo/máximo |
| `@Email` | Debe ser email válido |
| `@Pattern` | Debe coincidir con regex |
| `@Past`, `@Future` | Fecha pasada/futura |

### ¿Por qué Bean Validation y no validar a mano?

- **Declarativo**: la validación está en el DTO, no dispersa.
- **Reutilizable**: el mismo DTO valida en todos los endpoints.
- **Integrado con Spring**: `@Valid` funciona out-of-the-box.
- **Estándar**: Jakarta EE.

---

## 18. GraphHopper

### ¿Qué es?

GraphHopper es un **motor de enrutamiento** open source que calcula rutas entre puntos geográficos usando datos de OpenStreetMap. Ofrece una **API REST** (de pago) y una **librería Java** (open source).

### ¿Qué aporta al proyecto?

#### **1. Cálculo de rutas reales**

Sin GraphHopper, la simulación usaría una **línea recta** entre origen y destino.

Con GraphHopper, la simulación sigue **rutas reales por carretera**, con curvas, intersecciones, autopistas, etc.

#### **2. API REST**

```
GET https://graphhopper.com/api/1/route
    ?point=38.1408,-0.8844
    &point=37.9922,-1.1307
    &profile=car
    &points_encoded=false
    &key=YOUR_API_KEY
```

**Respuesta:** lista de coordenadas `[lon, lat]` que forman la ruta.

#### **3. Integración con `RestClient` de Spring**

```java
@Service
public class GraphHopperService {

    private final RestClient restClient;

    public GraphHopperService(@Value("${graphhopper.api.url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public RouteResponseDto calculateRoute(double startLat, double startLon,
                                            double endLat, double endLon) {
        JsonNode response = restClient.get()
                .uri(uriBuilder -> uriBuilder.path("/route")
                        .queryParam("point", startLat + "," + startLon)
                        .queryParam("point", endLat + "," + endLon)
                        .queryParam("profile", "car")
                        .queryParam("points_encoded", "false")
                        .queryParam("key", apiKey)
                        .build())
                .retrieve()
                .body(JsonNode.class);

        // Parsear respuesta
    }
}
```

#### **4. Caché LRU**

**Problema:** el plan gratuito de GraphHopper tiene **500 créditos/día** y un **límite por minuto**.

**Solución:** caché LRU en memoria con máximo 100 rutas.

```java
private final Map<String, RouteResponseDto> routeCache =
        Collections.synchronizedMap(new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, RouteResponseDto> eldest) {
                return size() > 100;
            }
        });
```

**Resultado:** si pides la misma ruta 100 veces, solo se llama a GraphHopper **una vez**.

#### **5. Fallback lineal**

Si GraphHopper falla (rate limit, sin Internet, API key inválida), se usa una **línea recta**.

```java
catch (Exception e) {
    log.warn("GraphHopper falló: {}. Aplicando fallback lineal.", e.getMessage());
    return buildLinearRoute(startLat, startLon, endLat, endLon);
}
```

**Resultado:** la simulación **nunca se rompe**, aunque la ruta no sea realista.

### ¿Por qué GraphHopper y no Google Maps?

| Criterio | GraphHopper | Google Maps |
|---|---|---|
| **Coste** | Plan gratuito (500 créditos/día) | De pago (desde $5/1000 req) |
| **Open source** | Sí (librería Java) | No |
| **Datos** | OpenStreetMap | Google Maps |
| **Calidad** | Excelente | Excelente |

**Para un TFG sin presupuesto, GraphHopper es la opción correcta.**

---

## 19. SpringDoc OpenAPI (Swagger)

### ¿Qué es?

SpringDoc OpenAPI es una librería que **genera documentación OpenAPI 3** automáticamente a partir de los controllers de Spring. Incluye una **UI web** (Swagger UI) donde puedes probar los endpoints.

### ¿Qué aporta al proyecto?

#### **1. Documentación automática**

SpringDoc lee los `@RestController`, `@GetMapping`, etc., y genera el JSON OpenAPI.

```java
@RestController
@RequestMapping("/api/v1/vehicles")
@Tag(name = "Vehículos", description = "Gestión de vehículos")
public class VehicleController {

    @Operation(summary = "Listar todos los vehículos")
    @ApiResponse(responseCode = "200", description = "Lista de vehículos")
    @GetMapping
    public List<VehicleDto> getAll() { ... }
}
```

#### **2. Swagger UI interactiva**

Accesible en: **http://localhost:8081/swagger-ui.html**

Permite:
- Ver todos los endpoints.
- Ver los esquemas de petición/respuesta.
- **Probar los endpoints** (botón "Try it out").

#### **3. Generación de clientes**

A partir del JSON OpenAPI (`/v3/api-docs`), se puede generar un cliente tipado para Angular:

```bash
npx @openapitools/openapi-generator-cli generate \
  -i http://localhost:8081/v3/api-docs \
  -g typescript-angular \
  -o ./src/app/api
```

**Resultado:** servicios HTTP tipados listos para usar en Angular.

### ¿Por qué SpringDoc y no Springfox?

- **Springfox está abandonado** (última actualización 2020).
- **SpringDoc está activo** y soporta Spring Boot 3+.
- **SpringDoc genera OpenAPI 3** (Springfox solo OpenAPI 2).
- **Mejor integración** con Spring WebFlux y MVC.

---

## 20. Actuator

### ¿Qué es?

Spring Boot Actuator añade **endpoints de monitorización** a la aplicación: health checks, métricas, información, etc.

### ¿Qué aporta al proyecto?

#### **1. Health checks**

```
GET /actuator/health
```

Respuesta:

```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "redis": { "status": "UP" },
    "kafka": { "status": "UP" },
    "diskSpace": { "status": "UP" }
  }
}
```

**Sabes en tiempo real si todo funciona.**

#### **2. Info del proyecto**

```
GET /actuator/info
```

Devuelve la información configurada:

```yaml
info:
  app:
    name: E-Cargo Hub
    description: Backend de simulación logística
    version: 1.0.0-SNAPSHOT
```

#### **3. Métricas**

```
GET /actuator/metrics
```

Devuelve métricas de JVM, HTTP, Kafka, etc.

#### **4. Rutas registradas**

```
GET /actuator/mappings
```

Lista **todas las rutas** que Spring ha registrado. **Útil para detectar duplicados**.

#### **5. Liveness y readiness**

```
GET /actuator/health/liveness   → ¿está vivo el proceso?
GET /actuator/health/readiness  → ¿puede recibir tráfico?
```

**Útil para Kubernetes o Docker healthchecks.**

### ¿Por qué Actuator?

- **Monitorización gratuita**: sin añadir código.
- **Integración con Spring**: detecta automáticamente DB, Redis, Kafka.
- **Estándar de la industria**.
- **Útil en producción**.

---

## 21. Maven

### ¿Qué es?

Maven es una **herramienta de gestión de proyectos** Java. Se encarga de:

- **Dependencias**: descarga las librerías necesarias.
- **Build**: compila, empaqueta, ejecuta tests.
- **Ciclo de vida**: `compile`, `test`, `package`, `install`.

### ¿Qué aporta al proyecto?

#### **1. `pom.xml` declarativo**

```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-webmvc</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.kafka</groupId>
        <artifactId>spring-kafka</artifactId>
    </dependency>
    <!-- ... -->
</dependencies>
```

**Solo declaras dependencias.** Maven descarga las transitivas.

#### **2. Wrapper de Maven**

El proyecto incluye `mvnw` (Maven Wrapper). No necesitas instalar Maven.

```bash
./mvnw spring-boot:run
```

#### **3. Plugins**

Spring Boot incluye un plugin para ejecutar la aplicación:

```bash
./mvnw spring-boot:run
```

Y para empaquetar:

```bash
./mvnw clean package
```

Genera un **JAR ejecutable** (`java -jar app.jar`).

### ¿Por qué Maven y no Gradle?

| Criterio | Maven | Gradle |
|---|---|---|
| **Configuración** | XML | Groovy/Kotlin DSL |
| **Curva de aprendizaje** | Baja | Media |
| **Rendimiento** | Bueno | Excelente |
| **Documentación** | Excelente | Excelente |
| **Ecosistema Spring** | Estándar | Soportado |

**Para un TFG, Maven es la opción segura** (más ejemplos, más documentación).

---

## 22. Flujo completo de una petición

Vamos a trazar **una petición completa** desde el frontend Angular hasta la respuesta, pasando por **cada tecnología**.

### Escenario: el usuario pulsa START en el vehículo 1

#### Paso 1: Angular envía la petición

```typescript
// En Angular
this.vehicleCommandService.dispatchCommand(1, {
  command: 'START',
  value: null
}).subscribe(response => {
  console.log('Comando enviado:', response);
});
```

**HTTP POST** a `http://localhost:8081/api/v1/vehicle-commands/vehicle/1`.

#### Paso 2: Spring MVC recibe la petición

**Tomcat** (servidor embebido) recibe la petición y la pasa a **Spring MVC**.

**Spring MVC** busca el **controller** que coincida con la ruta:

```java
@PostMapping("/vehicle/{vehicleId}")
public ResponseEntity<VehicleCommandDto> dispatchCommand(
        @PathVariable Long vehicleId,
        @Valid @RequestBody VehicleCommandRequest request) {
```

**Spring MVC deserializa** el JSON con **Jackson**:

```json
{ "command": "START", "value": null }
```

→ `new VehicleCommandRequest(START, null, null, null, null, null)`.

**Spring MVC valida** el DTO con **Bean Validation**:

- `command` no es null ✅
- coordenadas son null (opcionales) ✅

#### Paso 3: El controller llama al service

```java
return ResponseEntity.ok(vehicleCommandService.sendCommand(vehicleId, request));
```

#### Paso 4: El service ejecuta la lógica de negocio

```java
@Override
public VehicleCommandDto sendCommand(Long vehicleId, VehicleCommandRequest request) {

    // 1. Buscar el vehículo
    VehicleEntity vehicle = vehicleRepository.findById(vehicleId)
            .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found: " + vehicleId));

    // 2. Validar el comando según el estado actual
    switch (request.command()) {
        case START -> {
            if (simulationRegistry.isRunning(vehicleId)) {
                throw new VehicleAlreadyRunningException(vehicleId);
            }
        }
        case PAUSE -> { ... }
        case RESUME -> { ... }
        case STOP -> { ... }
    }

    // 3. Guardar el comando en BBDD
    VehicleCommandEntity entity = new VehicleCommandEntity();
    entity.setVehicle(vehicle);
    entity.setCommand(request.command());
    // ...
    VehicleCommandEntity saved = commandRepository.save(entity);

    // 4. Mapear a DTO
    VehicleCommandDto dto = commandMapper.toDto(saved);

    // 5. Publicar en Kafka
    kafkaProducerService.sendVehicleCommand(dto);

    // 6. Devolver
    return dto;
}
```

**Spring Data JPA** ejecuta el `INSERT` en PostgreSQL.

#### Paso 5: El producer publica en Kafka

```java
public void sendVehicleCommand(VehicleCommandDto command) {
    Message<VehicleCommandDto> message = MessageBuilder
            .withPayload(command)
            .setHeader(KafkaHeaders.TOPIC, "vehicle-commands")
            .setHeader(KafkaHeaders.KEY, String.valueOf(command.vehicleId()))
            .build();

    kafkaTemplate.send(message).whenComplete((result, ex) -> {
        if (ex != null) {
            log.error("❌ Kafka error: {}", ex.getMessage());
        } else {
            log.info("✅ Mensaje confirmado: offset={}", result.getRecordMetadata().offset());
        }
    });
}
```

**Spring Kafka** serializa el DTO con **Jackson** y lo publica en el topic.

**Kafka** persiste el mensaje y confirma al producer.

#### Paso 6: El controller devuelve 200 OK

**Spring MVC** serializa el DTO a JSON con **Jackson**:

```json
{
  "id": 42,
  "commandId": "47a1c31b-f21b-433e-ba85-ed2e38db4a5f",
  "vehicleId": 1,
  "command": "START",
  "value": null,
  "createdAt": "2026-09-30T14:30:00Z"
}
```

**Angular** recibe la respuesta en menos de 100ms. **La simulación aún no ha empezado**, pero el usuario ya tiene confirmación.

#### Paso 7: El consumer procesa el comando (asíncrono)

**Spring Kafka** entrega el mensaje al `@KafkaListener`:

```java
@KafkaListener(topics = "vehicle-commands", groupId = "e-cargo-hub-consumers")
public void listenVehicleCommands(String messageString) {

    // 1. Deserializar
    VehicleCommandDto command = objectMapper.readValue(messageString, VehicleCommandDto.class);

    // 2. Idempotencia (Redis)
    if (!idempotencyService.tryMarkAsProcessed(command.commandId())) {
        return;   // Ya procesado, descartar
    }

    // 3. Enrutar según el comando
    switch (command.command()) {
        case START -> handleStart(command);
        // ...
    }
}
```

**Redis** recibe un `SET NX` para el `commandId`.

#### Paso 8: El consumer calcula la ruta

```java
private RouteResponseDto buildRoute(VehicleCommandDto command) {
    // ...
    RouteResponseDto route = graphHopperService.calculateRoute(
            originLat, originLon, destLat, destLon);
    // ...
}
```

**GraphHopperService** llama a la API externa de GraphHopper.

**Si ya estaba en caché**, se devuelve sin llamar.

**Si falla**, se aplica el fallback lineal.

#### Paso 9: El consumer arranca la simulación

```java
simulationExecutor.submit(() -> simulateVehicleMovement(vehicleId, route, holder[0]));
```

**Un hilo del pool** empieza a emitir telemetría cada segundo.

#### Paso 10: El consumer emite telemetría por WebSocket

```java
Map<String, Object> telemetry = new HashMap<>();
telemetry.put("vehicleId", vehicleId);
telemetry.put("latitude", coord[0]);
// ...

messagingTemplate.convertAndSend("/topic/vehicle-status/" + vehicleId, (Object) telemetry);
```

**Spring WebSocket** envía el mensaje a todos los clientes suscritos al canal.

#### Paso 11: Persiste la telemetría

```java
vehicleTelemetryService.savePoint(vehicleId, coord[0], coord[1], progress, speed, "EN_RUTA");
```

**Spring Data JPA** inserta el punto en `vehicle_telemetry`.

#### Paso 12: Angular recibe la telemetría

```typescript
this.stompClient.subscribe('/topic/vehicle-status/1', (message) => {
  const telemetry = JSON.parse(message.body);
  this.updateMap(telemetry);
});
```

**El marcador se mueve en el mapa.**

#### Paso 13: Al terminar, se emite un evento global

```java
fleetEventService.vehicleCompleted(vehicleId);
```

**Spring WebSocket** envía a `/topic/fleet-status`.

**Angular** muestra una notificación.

#### Paso 14: Redis limpia la clave

Después de 24 horas, Redis borra automáticamente la clave del `commandId`.

**Fin del flujo.**

### Diagrama resumen

```
Angular
   │
   │ POST /api/v1/vehicle-commands/vehicle/1
   ▼
┌──────────────────────────────────────────┐
│ Spring MVC (Tomcat + Jackson + Validation)│
└─────────────┬────────────────────────────┘
              │
              ▼
┌──────────────────────────────────────────┐
│ Service (lógica + transacciones)          │
└─────────────┬────────────────────────────┘
              │
              ├──► PostgreSQL (INSERT comando)
              │
              └──► Kafka Producer (publicar)
                       │
                       │ (asíncrono)
                       ▼
              ┌──────────────────────────┐
              │ Kafka Consumer            │
              │  ├── Redis (idempotencia) │
              │  ├── GraphHopper (ruta)   │
              │  └── Simulación           │
              │       ├── WebSocket       │
              │       └── PostgreSQL      │
              └──────────────────────────┘
                       │
                       │ (tiempo real)
                       ▼
                    Angular
```

---

## 23. Decisiones técnicas clave

| Decisión | Alternativas | Motivo |
|---|---|---|
| **Kafka** para comandos | RabbitMQ, cola en memoria | Persistencia, escalabilidad, estándar |
| **Redis** para idempotencia | PostgreSQL, Memcached | Velocidad, TTL automático, atomicidad |
| **WebSocket + STOMP** | SSE, long polling | Pub/sub, bidireccional, canales múltiples |
| **PostgreSQL** | MySQL, MariaDB | Tipos avanzados, estándares, extensibilidad |
| **Jackson 3** | Jackson 2, Gson | Spring Boot 4, soporte java.time |
| **Lombok 1.18.42** | Lombok 1.18.30 | Compatibilidad con Java 21 y records |
| **MapStruct** | ModelMapper, manual | Type-safe, compilación, rendimiento |
| **Spring MVC** | WebFlux | Simplicidad, menos concurrencia |
| **GraphHopper** | Google Maps, OSRM | Plan gratuito, open source, calidad |
| **Maven** | Gradle | Estándar, documentación, ecosistema Spring |

---

## 24. Estructura de paquetes

```
com.ecargohub.backend
│
├── ECargoHubApplication.java          ← Clase principal
│
├── config/                            ← Configuración
│   ├── KafkaConfig.java               ← Producer/Consumer Kafka
│   ├── WebSocketConfig.java           ← WebSocket STOMP
│   ├── CorsConfig.java                ← CORS
│   ├── OpenApiConfig.java             ← Swagger/OpenAPI
│   └── InfoConfig.java                ← Info de Actuator
│
├── controller/                        ← Controllers REST
│   ├── VehicleController.java
│   └── VehicleCommandController.java
│
├── domain/enums/                      ← Enumerados
│   ├── VehicleCommandTypeEnum.java    ← START, PAUSE, RESUME, STOP
│   ├── VehicleStatusEnum.java         ← IDLE, EN_RUTA, ...
│   └── FleetEventTypeEnum.java        ← VEHICLE_STARTED, ...
│
├── dto/                               ← Data Transfer Objects
│   ├── command/
│   │   ├── VehicleCommandRequest.java
│   │   └── VehicleCommandDto.java
│   ├── fleet/
│   │   └── FleetEventDto.java
│   ├── route/
│   │   └── RouteResponseDto.java
│   ├── status/
│   │   └── VehicleStatusDto.java
│   ├── telemetry/
│   │   └── TelemetryPointDto.java
│   └── vehicle/
│       ├── CreateVehicleRequest.java
│       ├── UpdateVehicleRequest.java
│       └── VehicleDto.java
│
├── entity/                            ← Entidades JPA
│   ├── VehicleEntity.java
│   ├── VehicleCommandEntity.java
│   ├── VehicleCurrentStatusEntity.java
│   └── VehicleTelemetryEntity.java
│
├── exception/                         ← Excepciones
│   ├── GlobalExceptionHandler.java
│   ├── ResourceNotFoundException.java
│   └── VehicleAlreadyRunningException.java
│
├── interfaces/                        ← Interfaces de servicios
│   ├── VehicleService.java
│   └── VehicleCommandService.java
│
├── kafka/                             ← Kafka
│   ├── KafkaProducerService.java
│   └── KafkaConsumerService.java
│
├── mapper/                            ← Mappers MapStruct
│   ├── CommandMapper.java
│   └── VehicleCommandMapper.java
│
├── repository/                        ← Repositorios JPA
│   ├── VehicleRepository.java
│   ├── VehicleCommandRepository.java
│   ├── VehicleCurrentStatusRepository.java
│   └── VehicleTelemetryRepository.java
│
└── service/                           ← Servicios
    ├── VehicleServiceImpl.java
    ├── VehicleCommandServiceImpl.java
    ├── VehicleStatusService.java
    ├── VehicleTelemetryService.java
    ├── IdempotencyService.java
    ├── SimulationRegistry.java
    ├── FleetEventService.java
    └── geo/
        └── GraphHopperService.java
```

**Convención:** los paquetes siguen una estructura **por capa** (no por feature). Cada capa tiene una responsabilidad clara.

---

## Conclusión

El backend de **E-Cargo Hub** combina **tecnologías modernas** y **patrones empresariales** para construir un sistema **robusto, escalable y mantenible**:

- **Java 21 + Spring Boot 4** como base.
- **Arquitectura MVC en capas** para separación de responsabilidades.
- **Apache Kafka** para desacoplamiento asíncrono.
- **PostgreSQL** para persistencia relacional.
- **Redis** para idempotencia.
- **WebSocket + STOMP** para tiempo real.
- **GraphHopper** para rutas reales.
- **Swagger + Actuator** para documentación y monitorización.

Cada decisión técnica está **justificada** y **alineada** con los objetivos del proyecto. El resultado es un backend **profesional**, apto tanto para el TFG como para un entorno de producción real.

---

**Fin del documento.**