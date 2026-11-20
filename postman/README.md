# Pruebas de API con Postman

## Preparacion

1. Arranca PostgreSQL, Kafka, Redis y Keycloak con `docker compose -f infrastructure/docker-compose.yml up -d`.
2. Arranca Spring Boot desde `backend-java/E-Cargo-Hub`.
3. En Keycloak, crea un usuario de desarrollo y asignale `ADMIN` o `VIEWER` en los roles de realm. No hay cuentas ni contrasenas de aplicacion en estos archivos.
4. Abre `http://localhost:8081/test-console/`, inicia sesion y usa **Copiar token para Postman**.
5. Importa `E-Cargo-Hub.postman_collection.json` y `E-Cargo-Hub.postman_environment.json`. Selecciona el entorno **E-Cargo Hub local**, pega el token en `accessToken` y guardalo solo como valor local.

El JWT dura cinco minutos por defecto. Si Postman empieza a responder `401`, inicia sesion otra vez y actualiza `accessToken`. No exportes ni compartas el entorno con credenciales reales.

## Orden recomendado

1. Ejecuta las tres peticiones de `00 - Preparacion` con un usuario `ADMIN`. Crean un vehiculo, una ruta y un almacen; las respuestas guardan sus IDs en el entorno.
2. Ejecuta las peticiones de lectura y las pruebas de API.
3. La carpeta de simulaciones crea datos persistentes y cambia el estado de la simulacion; no hay endpoint para borrar una simulacion.
4. La carpeta de comandos puede publicar eventos en Kafka. Los comandos START/PAUSE/RESUME/STOP y el comando de flota son operaciones con efectos en el entorno.
5. Ejecuta `11 - Limpieza opcional (destructiva)` solo con recursos de prueba que no esten asociados a una simulacion.

La coleccion usa Bearer JWT de Keycloak. No requiere `ECARGO_CONTROL_API_TOKEN`; ese secreto se reserva para integraciones servidor-a-servidor. Los casos de autorizacion incluyen una peticion esperada `403` para ejecutar con un usuario `VIEWER` y otra `401` sin token. El puente VHAL solo puede responder correctamente si esta habilitado y hay un emulador AAOS disponible.

Los endpoints REST tienen un limite compartido de 100 peticiones por usuario o IP cada 60 segundos. Si una ejecucion excede el limite, espera el tiempo indicado en `Retry-After`; la coleccion no reintenta automaticamente operaciones que puedan modificar datos.
