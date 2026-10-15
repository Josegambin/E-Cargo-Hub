# Integración con Android Automotive OS

## Alcance

`android-aaos-bridge` es una app Android para ejecutar **dentro de un emulador AAOS** y mostrar, en modo de solo lectura, el estado de un vehículo simulado. El backend es el único punto de control: la app del coche no envía órdenes.

El estado mostrado es el de la simulación E-Cargo Hub, **no propiedades del VHAL ni actuadores del emulador o de un vehículo físico**. El climatizador, las luces, el encendido y la velocidad son valores virtuales.

## Preparación del backend

1. Arranca PostgreSQL, Kafka y Redis con `docker compose up -d` desde la raíz del repositorio.
2. Arranca el backend, por ejemplo desde PowerShell en `backend-java\E-Cargo-Hub`:

   ```powershell
   $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
   $bytes = New-Object byte[] 32
   $rng.GetBytes($bytes)
   $env:ECARGO_CONTROL_API_TOKEN = [System.Convert]::ToBase64String($bytes)
   $rng.Dispose()
   Write-Host "Token para Postman: $env:ECARGO_CONTROL_API_TOKEN"

   .\mvnw.cmd spring-boot:run
   ```

   $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
   $bytes = New-Object byte[] 6
   $rng.GetBytes($bytes)
   $env:ECARGO_CONTROL_API_TOKEN = [System.Convert]::ToBase64String($bytes)
   $rng.Dispose()

Write-Host "Token para Postman: $env:ECARGO_CONTROL_API_TOKEN"
.\mvnw.cmd spring-boot:run

   Guarda el token de forma segura y configúralo también en el servicio backend que enviará órdenes. El token debe tener al menos 32 bytes UTF-8. Si no está configurado o es demasiado corto, las rutas de control responden `503`; con un token ausente o incorrecto responden `401`.

3. Crea al menos un vehículo y conserva su ID:

   ```http
   POST http://localhost:8081/api/v1/vehicles
   Content-Type: application/json

   {"name":"AAOS-EMU-1","type":"CAR","maxSpeed":120,"energyType":"BATTERY_ELECTRIC","energyCapacity":75,"energyConsumptionPer100":18}
   ```

## Pantalla de estado AAOS

La app AAOS muestra velocidad, estado, progreso, energía, marcha, encendido, luces, limpiaparabrisas y climatizador. No permite seleccionar el vehículo, cambiar su configuración, mandar comandos ni publica telemetría VHAL. La URL del backend se fija en el despliegue (propiedad Gradle `ecargoBackendUrl`; por defecto `http://10.0.2.2:8081`). Para teléfonos Android/iOS, usa la PWA `/vehicle-display`; ambas pantallas solo reciben permisos de lectura tras una vinculación de un solo uso:

1. Desde un servicio de confianza, solicita `POST /api/v1/vehicles/{vehicleId}/device-enrollments` con `X-Control-Token`.
2. Para un teléfono, muestra el campo `qrSvg` de la respuesta y escanéalo con la cámara en los siguientes diez minutos; la PWA reclama el código al abrirse. Para AAOS, introduce el `pairingCode` en el botón **Vincular pantalla**. Cada dispositivo necesita su propio código.
3. El dispositivo guarda una credencial de solo lectura y consulta `GET /api/v1/device/state`; no guarda ni conoce el token de control.

La PWA requiere servir frontend y backend mediante HTTPS para una instalación fiable como app en Android/iOS. El código de emparejamiento caduca y solo puede utilizarse una vez; no compartas la respuesta ni el QR.

Para compilarla, abre `android-aaos-bridge` como proyecto en Android Studio y sincroniza Gradle, o compila desde PowerShell:

```powershell
$env:JAVA_HOME="$env:ProgramFiles\Android\Android Studio\jbr"
$env:Path="$env:JAVA_HOME\bin;$env:Path"
cd android-aaos-bridge
.\gradlew.bat assembleDebug
```

Usa el JDK compatible configurado por Android Studio (el JBR incluido); el APK queda en `app\build\outputs\apk\debug\app-debug.apk`. Instálala en el perfil Driver y ábrela desde el launcher AAOS para ver el estado.

## Control desde el backend

Envía los comandos directamente al backend. La pantalla del coche no necesita token ni participa en el control. Ejemplo para el vehículo `1`:

```http
POST http://localhost:8081/api/v1/vehicles/1/commands
Content-Type: application/json
X-Control-Token: <ECARGO_CONTROL_API_TOKEN>

{"command":"START","routeId":1}
```

Envía `X-Control-Token` en cada orden. El token solo debe estar en servicios de confianza del backend; no lo incluyas en una app de navegador o en el vehículo. `START` requiere `routeId` de una ruta creada previamente y límites de velocidad completos de GraphHopper. Usa el endpoint de vehículo con `PAUSE`, `RESUME`, `STOP`, `SET_SPEED`, `INCREASE_SPEED`, `DECREASE_SPEED`, `EMERGENCY_BRAKE`, `SET_IGNITION`, `SET_HEADLIGHTS`, `SET_HIGH_BEAMS`, `SET_HAZARD_LIGHTS`, `SET_TURN_SIGNAL`, `SET_WIPERS`, `SET_CLIMATE_POWER`, `SET_CLIMATE_TEMPERATURE`, `SET_CLIMATE_FAN_SPEED` o `SET_CLIMATE_MODE`. Consulta el estado virtual con `GET /api/v1/vehicles/{vehicleId}/aaos/control-state`; los cambios se notifican por WebSocket en `/topic/vehicles/{vehicleId}/control-state`.

En PowerShell, un cliente backend puede enviar la cabecera así:

```powershell
Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8081/api/v1/vehicles/1/commands" `
  -Headers @{ "X-Control-Token" = $env:ECARGO_CONTROL_API_TOKEN } `
  -ContentType "application/json" `
  -Body '{"command":"START","routeId":1}'
```

## Control de simulaciones

Comandos aceptados en `/api/v1/vehicles/{vehicleId}/commands` y `/api/v1/fleet/commands`:

- `START`, `PAUSE`, `RESUME`, `STOP`.
- `SET_SPEED` con un objetivo de 0 a `maxSpeed`.
- `INCREASE_SPEED` y `DECREASE_SPEED` con un paso positivo en km/h (10 por defecto).
- `EMERGENCY_BRAKE`, que detiene solo el vehículo simulado.
- `SET_IGNITION`, `SET_HEADLIGHTS`, `SET_HIGH_BEAMS`, `SET_HAZARD_LIGHTS` y `SET_WIPERS`, con `value: 0` para apagar y `value: 1` para encender.
- `SET_TURN_SIGNAL`, con `value: 0` para apagar, `1` para izquierda y `2` para derecha.
- `SET_CLIMATE_POWER`, con `value: 0` apagado y `1` encendido.
- `SET_CLIMATE_TEMPERATURE`, con un objetivo de 16 a 30 °C.
- `SET_CLIMATE_FAN_SPEED`, con un nivel entero de 0 a 7.
- `SET_CLIMATE_MODE`, con `value: 0` AUTO, `1` COOL o `2` HEAT.

Los cambios de velocidad modifican el movimiento y la velocidad anunciada por la simulación. Apagar el encendido simulado detiene un trayecto activo y bloquea nuevos arranques hasta volver a encenderlo. El estado de controles en memoria se consulta mediante `GET /api/v1/vehicles/{vehicleId}/aaos/control-state` y se publica en `/topic/vehicles/{vehicleId}/control-state`. La ruta de flota devuelve `207 Multi-Status` cuando no todos los vehículos pueden aceptar el mismo comando, por ejemplo al iniciar una flota con vehículos que ya están en marcha.

El estado climático forma parte de `GET /api/v1/vehicles/{vehicleId}/aaos/control-state` y del mensaje `/topic/vehicles/{vehicleId}/control-state`. Todo el estado virtual vive en memoria y vuelve a valores iniciales al reiniciar el backend.

`POST /api/v1/vehicles/{vehicleId}/aaos/simulated-commands` sigue disponible para integraciones administradas y exige el token de control. La app de pantalla no lo almacena ni lo utiliza.

## Integraciones AAOS adicionales

El backend conserva endpoints independientes de telemetría AAOS y notificaciones para otros clientes; no son necesarios para mostrar ni controlar la simulación desde este flujo.

> El token protege las rutas REST que envían órdenes o alteran la ejecución de simulaciones. No sustituye autenticación completa para las demás API, autorización por flota, HTTPS ni protecciones de despliegue; no expongas el servicio directamente a redes no confiables.
