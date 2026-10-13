package com.ecargohub.aaosbridge;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public class BridgeActivity extends Activity {

    public static final String ACTION_ADMIN_PROVISION =
            "com.ecargohub.aaosbridge.action.ADMIN_PROVISION";
    public static final String EXTRA_PAIRING_CODE = "pairingCode";
    private static final long REFRESH_INTERVAL_MS = 2000;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final AtomicBoolean refreshInFlight = new AtomicBoolean();
    private final String backendUrl = normalizeBackendUrl(BuildConfig.BACKEND_URL);
    private String deviceToken;
    private TextView speed;
    private TextView vehicleStatus;
    private TextView progress;
    private TextView energy;
    private TextView derivedTelemetry;
    private TextView controls;
    private TextView connection;
    private boolean activityStarted;

    private final Runnable refreshLoop = new Runnable() {
        @Override
        public void run() {
            refreshDashboard();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(24, 24, 24, 24);

        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(content);

        TextView heading = text("E-CARGO HUB", 24, Color.WHITE);
        heading.setTypeface(null, Typeface.BOLD);
        content.addView(heading);
        content.addView(text("ESTADO DEL VEHICULO · SOLO LECTURA", 14, Color.rgb(148, 163, 184)));

        deviceToken = VehicleCredentials.loadToken(this);

        LinearLayout dashboard = new LinearLayout(this);
        dashboard.setOrientation(LinearLayout.VERTICAL);
        dashboard.setPadding(24, 24, 24, 24);
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.rgb(18, 25, 38));
        background.setCornerRadius(24);
        dashboard.setBackground(background);

        speed = text("-- km/h", 52, Color.WHITE);
        speed.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        dashboard.addView(speed);
        vehicleStatus = text("Estado: esperando datos", 20, Color.rgb(34, 211, 238));
        dashboard.addView(vehicleStatus);
        progress = text("Trayecto: --", 16, Color.LTGRAY);
        progress.setPadding(0, 8, 0, 16);
        dashboard.addView(progress);

        energy = text("Energía: -- · Marcha --", 16, Color.LTGRAY);
        energy.setPadding(0, 0, 0, 12);
        dashboard.addView(energy);

        derivedTelemetry = text("Autonomía estimada: -- km · RPM virtual: --", 16, Color.LTGRAY);
        derivedTelemetry.setPadding(0, 0, 0, 12);
        dashboard.addView(derivedTelemetry);

        controls = text("Encendido --\nLuces -- · Largas -- · Emergencia --\nIntermitente -- · Limpiaparabrisas --\nClimatizador -- · -- °C · Ventilador -- · Modo --",
                16, Color.WHITE);
        controls.setPadding(0, 8, 0, 0);
        dashboard.addView(controls);

        content.addView(dashboard);

        connection = text("Conectando con el backend...", 14, Color.rgb(148, 163, 184));
        connection.setPadding(0, 16, 0, 8);
        content.addView(connection);

        content.addView(text(
                "Panel de solo lectura. La conexión y el control del vehículo son gestionados por el administrador de flota.",
                13, Color.rgb(148, 163, 184)));

        setContentView(scrollView);
        handleAdminProvisionIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleAdminProvisionIntent(intent);
    }

    @Override
    protected void onStart() {
        super.onStart();
        activityStarted = true;
        refreshDashboard();
    }

    @Override
    protected void onStop() {
        activityStarted = false;
        mainHandler.removeCallbacks(refreshLoop);
        super.onStop();
    }

    private void refreshDashboard() {
        if (!activityStarted || !refreshInFlight.compareAndSet(false, true)) {
            return;
        }

        String token = deviceToken;
        if (token == null || token.isBlank()) {
            connection.setText("Dispositivo pendiente de aprovisionamiento del administrador.");
            refreshInFlight.set(false);
            return;
        }

        new Thread(() -> {
            try {
                JSONObject state = getJson(backendUrl + "/api/v1/device/state", token);
                JSONObject vehicle = state.getJSONObject("vehicle");
                JSONObject controlsState = state.getJSONObject("controls");
                double currentSpeed = vehicle.optDouble("speedKmh", 0.0);
                String currentStatus = vehicle.optString("status", "IDLE");
                int currentProgress = vehicle.optInt("progress", 0);
                String energyType = vehicle.optString("energyType", "BATTERY_ELECTRIC").equals("FUEL")
                        ? "Combustible" : "Batería";
                double energyPercent = vehicle.optDouble("energyPercent", 100.0);
                String gear = vehicle.optString("gearPosition", "P");
                double rangeKm = state.optDouble("estimatedRangeKm", Double.NaN);
                int estimatedRpm = state.optInt("estimatedRpm", 0);
                String signal = controlsState.optString("turnSignal", "OFF");
                String climateMode = controlsState.optString("climateMode", "AUTO");
                String summary = "Encendido " + onOff(controlsState.optBoolean("ignitionOn"))
                        + "\nLuces " + onOff(controlsState.optBoolean("headlightsOn"))
                        + " · Largas " + onOff(controlsState.optBoolean("highBeamsOn"))
                        + " · Emergencia " + onOff(controlsState.optBoolean("hazardLightsOn"))
                        + "\nIntermitente " + signal
                        + " · Limpiaparabrisas " + onOff(controlsState.optBoolean("wipersOn"))
                        + "\nClimatizador " + onOff(controlsState.optBoolean("climateOn"))
                        + " · " + String.format(Locale.getDefault(), "%.1f °C",
                                controlsState.optDouble("climateTemperatureC", 22.0))
                        + " · Ventilador " + controlsState.optInt("climateFanSpeed", 0)
                        + " · Modo " + climateMode;
                String currentVehicleId = String.valueOf(vehicle.optLong("vehicleId"));
                mainHandler.post(() -> {
                    speed.setText(String.format(Locale.getDefault(), "%.1f km/h", currentSpeed));
                    vehicleStatus.setText("Estado: " + currentStatus);
                    progress.setText("Trayecto: " + currentProgress + "%");
                    energy.setText(String.format(Locale.getDefault(), "%s: %.1f%% · Marcha %s",
                            energyType, energyPercent, gear));
                    String range = Double.isFinite(rangeKm)
                            ? String.format(Locale.getDefault(), "%.1f km", rangeKm) : "-- km";
                    derivedTelemetry.setText("Autonomía estimada: " + range + " · RPM virtual aprox.: "
                            + estimatedRpm);
                    controls.setText(summary);
                    connection.setText("Conectado · Vehículo " + currentVehicleId + " · Solo lectura");
                });
            } catch (Exception error) {
                boolean unauthorized = error instanceof HttpStatusException statusError && statusError.statusCode == 401;
                if (unauthorized && token.equals(deviceToken)) {
                    deviceToken = null;
                    VehicleCredentials.clearToken(this);
                    mainHandler.post(() -> connection
                            .setText("Credencial rechazada. Se requiere un nuevo aprovisionamiento del administrador."));
                } else if (!unauthorized) {
                    mainHandler.post(() -> connection.setText("Backend no disponible: " + error.getMessage()));
                }
            } finally {
                refreshInFlight.set(false);
                scheduleRefresh();
            }
        }, "ecargo-status-dashboard").start();
    }

    private void scheduleRefresh() {
        if (activityStarted) {
            mainHandler.removeCallbacks(refreshLoop);
            mainHandler.postDelayed(refreshLoop, REFRESH_INTERVAL_MS);
        }
    }

    private JSONObject getJson(String url, String token) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        try {
            connection.setRequestMethod("GET");
            connection.setRequestProperty("X-Vehicle-Device-Token", token);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            int responseCode = connection.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                throw new HttpStatusException(responseCode);
            }
            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }
            return new JSONObject(response.toString());
        } finally {
            connection.disconnect();
        }
    }

    private void handleAdminProvisionIntent(Intent intent) {
        if (intent == null || !ACTION_ADMIN_PROVISION.equals(intent.getAction())) {
            return;
        }
        String pairingCode = intent.getStringExtra(EXTRA_PAIRING_CODE);
        if (pairingCode == null || pairingCode.isBlank()) {
            connection.setText("Aprovisionamiento rechazado: falta el código del administrador.");
            return;
        }
        connection.setText("Aprovisionamiento administrativo en curso…");
        claimDevice(pairingCode.trim());
    }

    private void claimDevice(String pairingCode) {
        if (!pairingCode.matches("[0-9a-f]{64}")) {
            connection.setText("Aprovisionamiento rechazado: formato de código no válido.");
            return;
        }
        new Thread(() -> {
            try {
                JSONObject claimRequest = new JSONObject();
                claimRequest.put("pairingCode", pairingCode);
                claimRequest.put("deviceName", "AAOS " + android.os.Build.MODEL);
                JSONObject claim = postJson(backendUrl + "/api/v1/device-enrollments/claim", claimRequest);
                String newToken = claim.getString("deviceToken");
                String newVehicleId = String.valueOf(claim.getLong("vehicleId"));
                VehicleCredentials.saveToken(this, newToken);
                deviceToken = newToken;
                mainHandler.post(() -> {
                    connection.setText("Pantalla vinculada · Vehículo " + newVehicleId);
                    refreshDashboard();
                });
            } catch (Exception error) {
                String message = error instanceof HttpStatusException statusError && statusError.statusCode == 401
                        ? "Código rechazado o vencido. La credencial actual se conserva; el administrador debe generar otro código."
                        : "Aprovisionamiento no completado: " + error.getMessage();
                mainHandler.post(() -> connection.setText(message));
            }
        }, "ecargo-device-pairing").start();
    }

    private JSONObject postJson(String url, JSONObject requestBody) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        try {
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");
            byte[] payload = requestBody.toString().getBytes(StandardCharsets.UTF_8);
            try (java.io.OutputStream output = connection.getOutputStream()) {
                output.write(payload);
            }
            int responseCode = connection.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                throw new HttpStatusException(responseCode);
            }
            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
            }

            return new JSONObject(response.toString());
        } finally {
            connection.disconnect();
        }
    }

    private static final class HttpStatusException extends Exception {
        private final int statusCode;

        private HttpStatusException(int statusCode) {
            super("HTTP " + statusCode);
            this.statusCode = statusCode;
        }
    }

    private String normalizeBackendUrl(String url) {
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    private String onOff(boolean enabled) {
        return enabled ? "encendido" : "apagado";
    }

    private TextView text(String value, float size, int color) {
        TextView label = new TextView(this);
        label.setText(value);
        label.setTextSize(size);
        label.setTextColor(color);
        label.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return label;
    }
}
