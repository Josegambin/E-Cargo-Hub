package com.ecargohub.backend.service;

import com.ecargohub.backend.domain.enums.VehicleEnergyTypeEnum;
import jakarta.annotation.PreDestroy;
import java.io.File;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Puente opcional hacia el VHAL del emulador AAOS mediante ADB.
 *
 * <p>
 * El emulador Automotive no expone ningún servidor gRPC en el puerto 45001; la vía soportada desde el host es
 * {@code adb shell cmd car_service
 * inject-vhal-event}. Este servicio está desactivado por defecto y solo se activa con {@code ECARGO_VHAL_ENABLED=true}.
 * </p>
 */
@Service
public class VhalBridgeService {

    private static final Logger log = LoggerFactory.getLogger(VhalBridgeService.class);

    /** Nivel de combustible (mL) que el emulador AAOS representa como tanque lleno (100%). */
    private static final double EMULATOR_FUEL_FULL_ML = 15000.0;

    private final boolean enabled;
    private final String adbPath;
    private final String serial;
    private final long timeoutSeconds;
    private final ExecutorService vhalExecutor;
    private volatile String lastError;
    private volatile String lastOutput;
    private volatile boolean unsupportedBuildDetected;

    public VhalBridgeService(@Value("${app.vhal.enabled:false}") boolean enabled,
            @Value("${app.vhal.adb-path:adb}") String adbPath, @Value("${app.vhal.serial:emulator-5554") String serial,
            @Value("${app.vhal.timeout-seconds:5}") long timeoutSeconds) {
        this.enabled = enabled;
        this.adbPath = resolveAdbPath(adbPath);
        this.serial = sanitizeSerial(serial);
        this.timeoutSeconds = timeoutSeconds;
        this.vhalExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "vhal-bridge");
            t.setDaemon(true);
            return t;
        });
        log.info("VHAL bridge config: enabled={} adb='{}' serial={} timeout={}s", enabled, this.adbPath, this.serial,
                timeoutSeconds);
    }

    @PreDestroy
    void shutdown() {
        vhalExecutor.shutdownNow();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getAdbPath() {
        return adbPath;
    }

    public String getSerial() {
        return serial;
    }

    public String getLastError() {
        return lastError;
    }

    public String getLastOutput() {
        return lastOutput;
    }

    /**
     * Inyecta velocidad, energía y marcha derivadas de la simulación. No bloquea: encola el trabajo en un hilo propio
     * para no frenar la simulación.
     */
    public void pushSimulationStep(double speedKmh, double energyPercent, double energyCapacity,
            double energyConsumptionPer100, VehicleEnergyTypeEnum energyType, String gearPosition) {
        if (!enabled) {
            return;
        }
        vhalExecutor.execute(() -> {
            double speedMs = Math.max(0.0, speedKmh / 3.6);
            inject("PERF_VEHICLE_SPEED", 0, speedMs);
            inject("PERF_VEHICLE_SPEED_DISPLAY", 0, speedKmh);
            inject("ENGINE_RPM", 0, estimatedRpm(speedKmh, gearPosition));
            // Autonomía: misma fórmula que la app (VehicleDeviceService):
            // capacidad * porcentaje / consumo-por-100, convertida a metros para el VHAL.
            double rangeKm = energyCapacity > 0 && energyConsumptionPer100 > 0
                    ? energyCapacity * energyPercent / energyConsumptionPer100 : 0.0;
            inject("RANGE_REMAINING", 0, rangeKm * 1000.0);
            inject("GEAR_SELECTION", 0, gearSelection(gearPosition));
            inject("CURRENT_GEAR", 0, currentGear(gearPosition));
            // Freno de estacionamiento: activo en P y N, inactivo en D y R
            double parkingBrake = ("P".equals(gearPosition) || "N".equals(gearPosition)) ? 1.0 : 0.0;
            inject("PARKING_BRAKE", 0, parkingBrake);
            double energyUnits = energyCapacity * energyPercent / 100.0 * 1000.0;
            if (energyType == VehicleEnergyTypeEnum.FUEL) {
                inject("FUEL_LEVEL", 0, energyUnits);
            } else {
                inject("EV_BATTERY_LEVEL", 0, energyUnits);
                inject("EV_CURRENT_BATTERY_CAPACITY", 0, energyCapacity * 1000.0);
                // El clúster por defecto del emulador dibuja la aguja "Fuel" a partir de
                // FUEL_LEVEL (no de EV_BATTERY_LEVEL). Referencia 100% = 15000 mL del
                // emulador; escalamos al % de batería para que refleje la carga real.
                inject("FUEL_LEVEL", 0, EMULATOR_FUEL_FULL_ML * energyPercent / 100.0);
            }
        });
    }

    /** Deja el emulador parado (velocidad 0, marcha P, freno de estacionamiento activo). */
    public void pushStopped(double energyPercent, double energyCapacity, double energyConsumptionPer100) {
        if (!enabled) {
            return;
        }
        vhalExecutor.execute(() -> {
            inject("PERF_VEHICLE_SPEED", 0, 0.0);
            inject("PERF_VEHICLE_SPEED_DISPLAY", 0, 0.0);
            inject("ENGINE_RPM", 0, 0.0);
            inject("GEAR_SELECTION", 0, 4.0);
            inject("CURRENT_GEAR", 0, 0.0);
            // Freno de estacionamiento activo cuando está en P
            inject("PARKING_BRAKE", 0, 1.0);
            // Autonomía restante al detenerse
            double rangeKm = energyCapacity > 0 && energyConsumptionPer100 > 0
                    ? energyCapacity * energyPercent / energyConsumptionPer100 : 0.0;
            inject("RANGE_REMAINING", 0, rangeKm * 1000.0);
        });
    }

    public record VhalResult(boolean ok, String output) {
    }

    public VhalResult inject(String property, int areaId, double value) {
        if (!enabled) {
            return new VhalResult(false, "VHAL bridge disabled (ECARGO_VHAL_ENABLED!=true)");
        }
        if (property == null || !property.matches("[A-Z0-9_]{1,64}")) {
            return new VhalResult(false, "Invalid property name: " + property);
        }
        if (unsupportedBuildDetected) {
            return new VhalResult(false,
                    "Puente VHAL inactivo: el emulador es build 'user' y no acepta inyección VHAL");
        }
        VhalResult viaCarService = runAdb(false, "shell", "cmd", "car_service", "inject-vhal-event", property,
                String.valueOf(areaId), formatTyped(property, value));
        if (viaCarService.ok()) {
            return viaCarService;
        }
        if (viaCarService.output() != null && viaCarService.output().contains("requires non-user build")) {
            VhalResult viaDumpsys = runAdb("shell", "dumpsys", "android.hardware.automotive.vehicle.IVehicle/default",
                    "--set", property, "-a", String.valueOf(areaId), typeFlag(property), formatTyped(property, value));
            if (viaDumpsys.ok()) {
                log.debug("VHAL {} inyectado vía dumpsys --set (build user)", property);
                return viaDumpsys;
            }
            if (!unsupportedBuildDetected) {
                unsupportedBuildDetected = true;
                log.warn("VHAL no soportado en este emulador (build 'user', p. ej. imagen Google Play): "
                        + "car_service solo acepta inyectar en build no-user y dumpsys --set tampoco funciona aquí. "
                        + "Usa el emulador AOSP (userdebug) o desactiva ECARGO_VHAL_ENABLED. "
                        + "El puente queda inactivo para no repetir el intento en bucle.");
            }
            return viaDumpsys;
        }
        log.warn("VHAL no disponible en {} (adb='{}'): {}", serial, adbPath, viaCarService.output());
        return viaCarService;
    }

    private VhalResult runAdb(String... args) {
        return runAdb(true, args);
    }

    private VhalResult runAdb(boolean noisy, String... args) {
        String[] command = new String[args.length + 3];
        command[0] = adbPath;
        command[1] = "-s";
        command[2] = serial;
        System.arraycopy(args, 0, command, 3, args.length);
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        try {
            Process process = builder.start();
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            String output = new String(process.getInputStream().readAllBytes()).trim();
            lastOutput = output;
            if (!finished) {
                process.destroyForcibly();
                lastError = "timeout tras " + timeoutSeconds + "s: " + output;
                log.warn("VHAL adb sin respuesta tras {}s (emulador {} adb='{}'): {}", timeoutSeconds, serial, adbPath,
                        output);
                return new VhalResult(false, lastError);
            }
            if (process.exitValue() != 0) {
                lastError = output;
                if (noisy) {
                    log.warn("VHAL no disponible en {} (adb='{}'): {}", serial, adbPath, output);
                } else {
                    log.debug("VHAL adb falló: {}", output);
                }
                return new VhalResult(false, output);
            }
            lastError = null;
            return new VhalResult(true, output);
        } catch (Exception ex) {
            lastError = ex.getMessage();
            log.warn("No se pudo ejecutar adb VHAL ({} adb='{}'): {}", serial, adbPath, ex.getMessage());
            return new VhalResult(false, ex.getMessage());
        }
    }

    private static String formatValue(double value) {
        return String.format(Locale.US, "%.3f", value);
    }

    private static String typeFlag(String property) {
        return switch (property) {
        case "GEAR_SELECTION", "CURRENT_GEAR" -> "-i";
        default -> "-f";
        };
    }

    private static String formatTyped(String property, double value) {
        if ("-i".equals(typeFlag(property))) {
            return String.valueOf((int) Math.round(value));
        }
        return formatValue(value);
    }

    /**
     * Normaliza el serial de ADB. Si la variable de entorno llegó sin resolver (placeholder {@code ${...}} crudo) o
     * vacía, cae al serial por defecto {@code emulator-5554} para no pasar basura a {@code adb -s}.
     */
    private static String sanitizeSerial(String configured) {
        if (configured == null || configured.isBlank() || configured.contains("${")) {
            return "emulator-5554";
        }
        return configured.trim();
    }

    private static String resolveAdbPath(String configured) {
        if (configured != null && !configured.isBlank() && !configured.equals("adb")) {
            return configured;
        }
        String localAppData = System.getenv("LOCALAPPDATA");
        String[] roots = { System.getenv("ANDROID_HOME"), System.getenv("ANDROID_SDK_ROOT"),
                localAppData == null ? null : localAppData + "\\Android\\Sdk" };
        for (String root : roots) {
            if (root == null || root.isBlank()) {
                continue;
            }
            File exe = new File(root, "platform-tools\\adb.exe");
            if (exe.exists()) {
                return exe.getAbsolutePath();
            }
        }
        return configured == null || configured.isBlank() ? "adb" : configured;
    }

    private static double estimatedRpm(double speedKmh, String gearPosition) {
        if (speedKmh <= 0) {
            return 0.0;
        }
        int gear = 1;
        if (gearPosition != null && gearPosition.matches("^D[1-6]$")) {
            gear = gearPosition.charAt(1) - '0';
        }
        double[] ratios = { 3.8, 2.3, 1.5, 1.1, 0.85, 0.65 };
        return speedKmh * 1000.0 / 60.0 / 2.0 * ratios[gear - 1] * 3.5;
    }

    private static double gearSelection(String gearPosition) {
        if ("R".equals(gearPosition)) {
            return 2;
        }
        if ("N".equals(gearPosition)) {
            return 1;
        }
        if (gearPosition != null && gearPosition.startsWith("D")) {
            return 8;
        }
        return 4;
    }

    private static double currentGear(String gearPosition) {
        if (gearPosition != null && gearPosition.matches("^D[1-6]$")) {
            return new double[] { 16, 32, 64, 128, 256, 512 }[gearPosition.charAt(1) - '1'];
        }
        if ("R".equals(gearPosition)) {
            return -1;
        }
        return 0;
    }
}
