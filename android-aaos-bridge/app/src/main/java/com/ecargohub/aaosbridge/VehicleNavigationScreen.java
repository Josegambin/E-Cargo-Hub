package com.ecargohub.aaosbridge;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.car.app.AppManager;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.navigation.model.MessageInfo;
import androidx.car.app.navigation.model.NavigationTemplate;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public final class VehicleNavigationScreen extends Screen {
    private static final String TAG = "E-Cargo-Navigation";
    private static final long REFRESH_INTERVAL_MS = 2500;

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService backendExecutor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean requestInFlight = new AtomicBoolean();
    private final OpenStreetMapRenderer mapRenderer;
    private final String backendUrl;
    private volatile boolean started;
    private volatile String deviceToken;
    private volatile String message = "Conectando con E-Cargo Hub…";

    private final Runnable refreshLoop = new Runnable() {
        @Override
        public void run() {
            refreshVehicleState();
        }
    };

    public VehicleNavigationScreen(CarContext carContext) {
        super(carContext);
        backendUrl = normalizeBackendUrl(BuildConfig.BACKEND_URL);
        deviceToken = VehicleCredentials.loadToken(carContext);
        mapRenderer = new OpenStreetMapRenderer();
        carContext.getCarService(AppManager.class).setSurfaceCallback(mapRenderer.getSurfaceCallback());
        getLifecycle().addObserver(new DefaultLifecycleObserver() {
            @Override
            public void onStart(LifecycleOwner owner) {
                started = true;
                refreshVehicleState();
            }

            @Override
            public void onStop(LifecycleOwner owner) {
                started = false;
                mainHandler.removeCallbacks(refreshLoop);
            }

            @Override
            public void onDestroy(LifecycleOwner owner) {
                started = false;
                mainHandler.removeCallbacks(refreshLoop);
                backendExecutor.shutdownNow();
                mapRenderer.close();
            }
        });
    }

    @Override
    public androidx.car.app.model.Template onGetTemplate() {
        NavigationTemplate.Builder builder = new NavigationTemplate.Builder();
        builder.setNavigationInfo(new MessageInfo.Builder(message).build());
        builder.setActionStrip(new ActionStrip.Builder()
                .addAction(new Action.Builder()
                        .setTitle("Actualizar")
                        .setOnClickListener(this::refreshVehicleState)
                        .build())
                .build());
        return builder.build();
    }

    private void refreshVehicleState() {
        if (!started || !requestInFlight.compareAndSet(false, true)) {
            return;
        }
        String token = deviceToken;
        if (token == null || token.isBlank()) {
            message = "Dispositivo pendiente de aprovisionamiento del administrador";
            mapRenderer.update(null);
            requestInFlight.set(false);
            invalidate();
            scheduleRefresh();
            return;
        }
        backendExecutor.execute(() -> {
            try {
                JSONObject state = getJson(backendUrl + "/api/v1/device/state", token);
                OpenStreetMapRenderer.MapFrame frame = OpenStreetMapRenderer.MapFrame.fromJson(state);
                String vehicleStatus = frame.status();
                String updatedMessage = "Vehículo " + frame.vehicleId() + " · " + vehicleStatus;
                mainHandler.post(() -> {
                    message = updatedMessage;
                    mapRenderer.update(frame);
                    invalidate();
                });
            } catch (Exception error) {
                if (error instanceof HttpStatusException statusError && statusError.statusCode == 401) {
                    if (token.equals(deviceToken)) {
                        deviceToken = null;
                        VehicleCredentials.clearToken(getCarContext());
                    }
                    message = "Credencial rechazada. Se requiere aprovisionamiento del administrador";
                } else {
                    Log.w(TAG, "No se pudo actualizar el estado del vehículo desde el backend", error);
                    message = "Backend no disponible · reintentando";
                }
                mainHandler.post(() -> {
                    mapRenderer.update(null);
                    invalidate();
                });
            } finally {
                requestInFlight.set(false);
                scheduleRefresh();
            }
        });
    }

    private void scheduleRefresh() {
        if (started) {
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

    private String normalizeBackendUrl(String url) {
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }

    private static final class HttpStatusException extends Exception {
        private final int statusCode;

        private HttpStatusException(int statusCode) {
            super("HTTP " + statusCode);
            this.statusCode = statusCode;
        }
    }
}
