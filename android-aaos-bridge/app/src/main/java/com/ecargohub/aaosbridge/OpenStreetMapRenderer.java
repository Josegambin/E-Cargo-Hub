package com.ecargohub.aaosbridge;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.LruCache;
import android.util.Log;
import android.view.Surface;

import androidx.car.app.SurfaceCallback;
import androidx.car.app.SurfaceContainer;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

final class OpenStreetMapRenderer {
    private static final String TAG = "E-Cargo-Map";
    private static final int ZOOM = 14;
    private static final int TILE_SIZE = 256;
    private static final double MAX_MERCATOR_LAT = 85.05112878;

    private final ExecutorService tileExecutor = Executors.newFixedThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable, "ecargo-osm-tiles");
        thread.setDaemon(true);
        return thread;
    });
    private final LruCache<String, Bitmap> tileCache = new LruCache<>(96);
    private final Map<String, Long> failedTiles = new ConcurrentHashMap<>();
    private final Map<String, Boolean> loadingTiles = new ConcurrentHashMap<>();
    private final Paint tilePaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Paint routePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint markerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hudPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final SurfaceCallback surfaceCallback = new SurfaceCallback() {
        @Override
        public void onSurfaceAvailable(SurfaceContainer surfaceContainer) {
            synchronized (OpenStreetMapRenderer.this) {
                surface = surfaceContainer.getSurface();
                redraw();
            }
        }

        @Override
        public void onVisibleAreaChanged(Rect visibleArea) {
            safeArea = new Rect(visibleArea);
            redraw();
        }

        @Override
        public void onStableAreaChanged(Rect stableArea) {
            safeArea = new Rect(stableArea);
            redraw();
        }

        @Override
        public void onSurfaceDestroyed(SurfaceContainer surfaceContainer) {
            synchronized (OpenStreetMapRenderer.this) {
                surface = null;
            }
        }

        @Override
        public void onScroll(float distanceX, float distanceY) {
        }

        @Override
        public void onScale(float focusX, float focusY, float scaleFactor) {
            synchronized (OpenStreetMapRenderer.this) {
                if (scaleFactor < 1f) {
                    zoom = Math.max(11, zoom - 1);
                } else {
                    zoom = Math.min(17, zoom + 1);
                }
            }
            redraw();
        }
    };

    private volatile Surface surface;
    private volatile MapFrame frame;
    private volatile Rect safeArea = new Rect();
    private int zoom = ZOOM;

    OpenStreetMapRenderer() {
        routePaint.setColor(Color.rgb(30, 144, 255));
        routePaint.setStrokeWidth(9f);
        routePaint.setStrokeCap(Paint.Cap.ROUND);
        routePaint.setStrokeJoin(Paint.Join.ROUND);
        routePaint.setStyle(Paint.Style.STROKE);
        markerPaint.setColor(Color.rgb(255, 71, 87));
        hudPaint.setColor(Color.argb(220, 12, 20, 31));
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(25f);
        textPaint.setTypeface(android.graphics.Typeface.create("sans-serif-medium",
                android.graphics.Typeface.NORMAL));
    }

    SurfaceCallback getSurfaceCallback() {
        return surfaceCallback;
    }

    void update(MapFrame newFrame) {
        frame = newFrame;
        redraw();
    }

    void close() {
        tileExecutor.shutdownNow();
        tileCache.evictAll();
    }

    private synchronized void redraw() {
        Surface activeSurface = surface;
        if (activeSurface == null || !activeSurface.isValid()) {
            return;
        }
        Canvas canvas = null;
        try {
            canvas = activeSurface.lockCanvas(null);
            if (canvas == null) {
                return;
            }
            canvas.drawColor(Color.rgb(30, 39, 48));
            MapFrame currentFrame = frame;
            if (currentFrame == null) {
                drawHud(canvas, "Esperando telemetría del backend", "", "", 0);
            } else {
                if (hasMapData(currentFrame)) {
                    double[] center = mapCenter(currentFrame);
                    drawTiles(canvas, center[0], center[1]);
                    drawRoute(canvas, currentFrame, center[0], center[1]);
                }
                drawHud(canvas, currentFrame.primaryTelemetry(), currentFrame.secondaryTelemetry(),
                        currentFrame.status(), currentFrame.progress());
            }
        } catch (IllegalArgumentException | IllegalStateException error) {
            Log.w(TAG, "No se pudo dibujar el mapa AAOS", error);
        } finally {
            if (canvas != null) {
                try {
                    activeSurface.unlockCanvasAndPost(canvas);
                } catch (IllegalArgumentException | IllegalStateException error) {
                    Log.w(TAG, "No se pudo publicar el fotograma del mapa", error);
                }
            }
        }
    }

    private void drawTiles(Canvas canvas, double latitude, double longitude) {
        double worldSize = worldSize(zoom);
        double centerX = worldX(longitude, worldSize);
        double centerY = worldY(latitude, worldSize);
        double left = centerX - canvas.getWidth() / 2.0;
        double top = centerY - canvas.getHeight() / 2.0;
        int firstTileX = (int) Math.floor(left / TILE_SIZE);
        int lastTileX = (int) Math.floor((left + canvas.getWidth()) / TILE_SIZE);
        int firstTileY = Math.max(0, (int) Math.floor(top / TILE_SIZE));
        int lastTileY = Math.min((1 << zoom) - 1,
                (int) Math.floor((top + canvas.getHeight()) / TILE_SIZE));
        int tileCount = 1 << zoom;
        RectF destination = new RectF();
        for (int tileY = firstTileY; tileY <= lastTileY; tileY++) {
            for (int tileX = firstTileX; tileX <= lastTileX; tileX++) {
                int wrappedX = Math.floorMod(tileX, tileCount);
                String key = zoom + "/" + wrappedX + "/" + tileY;
                float screenX = (float) (tileX * (double) TILE_SIZE - left);
                float screenY = (float) (tileY * (double) TILE_SIZE - top);
                destination.set(screenX, screenY, screenX + TILE_SIZE, screenY + TILE_SIZE);
                Bitmap bitmap = tileCache.get(key);
                if (bitmap == null || bitmap.isRecycled()) {
                    requestTile(key);
                } else {
                    canvas.drawBitmap(bitmap, null, destination, tilePaint);
                }
            }
        }
    }

    private void requestTile(String key) {
        if (tileExecutor.isShutdown()) {
            return;
        }
        if (loadingTiles.putIfAbsent(key, Boolean.TRUE) != null) {
            return;
        }
        Long failedAt = failedTiles.get(key);
        if (failedAt != null && System.currentTimeMillis() - failedAt < 60_000) {
            loadingTiles.remove(key);
            return;
        }
        tileExecutor.execute(() -> {
            try {
                String tileUrl = "https://tile.openstreetmap.org/" + key + ".png";
                HttpURLConnection connection = (HttpURLConnection) new URL(tileUrl).openConnection();
                connection.setConnectTimeout(7000);
                connection.setReadTimeout(7000);
                connection.setRequestProperty("User-Agent",
                        "E-Cargo-Hub-AAOS/1.0 (+https://github.com/Josegambin/E-Cargo-Hub)");
                try {
                    int status = connection.getResponseCode();
                    if (status != HttpURLConnection.HTTP_OK) {
                        throw new java.io.IOException("OSM tile server returned HTTP " + status + " for " + key);
                    }
                    try (InputStream input = connection.getInputStream()) {
                        Bitmap bitmap = BitmapFactory.decodeStream(input);
                        if (bitmap == null) {
                            throw new java.io.IOException("OSM returned an unreadable tile for " + key);
                        }
                        tileCache.put(key, bitmap);
                    }
                } finally {
                    connection.disconnect();
                }
                failedTiles.remove(key);
            } catch (Exception error) {
                failedTiles.put(key, System.currentTimeMillis());
                Log.w(TAG, "No se pudo cargar la tesela de OpenStreetMap " + key, error);
            } finally {
                loadingTiles.remove(key);
                redraw();
            }
        });
    }

    private void drawRoute(Canvas canvas, MapFrame currentFrame, double centerLatitude, double centerLongitude) {
        double worldSize = worldSize(zoom);
        double centerX = worldX(centerLongitude, worldSize);
        double centerY = worldY(centerLatitude, worldSize);
        float halfWidth = canvas.getWidth() / 2f;
        float halfHeight = canvas.getHeight() / 2f;
        Path path = new Path();
        boolean first = true;
        for (double[] coordinate : currentFrame.routeCoordinates()) {
            if (coordinate.length < 2 || !validCoordinate(coordinate[0], coordinate[1])) {
                continue;
            }
            float x = (float) (worldX(coordinate[1], worldSize) - centerX + halfWidth);
            float y = (float) (worldY(coordinate[0], worldSize) - centerY + halfHeight);
            if (first) {
                path.moveTo(x, y);
                first = false;
            } else {
                path.lineTo(x, y);
            }
        }
        if (!first) {
            canvas.drawPath(path, routePaint);
        }

        double latitude = currentFrame.latitude();
        double longitude = currentFrame.longitude();
        if (validCoordinate(latitude, longitude)) {
            float x = (float) (worldX(longitude, worldSize) - centerX + halfWidth);
            float y = (float) (worldY(latitude, worldSize) - centerY + halfHeight);
            markerPaint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(x, y, 15f, markerPaint);
            markerPaint.setColor(Color.WHITE);
            markerPaint.setStyle(Paint.Style.STROKE);
            markerPaint.setStrokeWidth(4f);
            canvas.drawCircle(x, y, 15f, markerPaint);
            markerPaint.setColor(Color.rgb(255, 71, 87));
            markerPaint.setStyle(Paint.Style.FILL);
        }
    }

    private boolean hasMapData(MapFrame currentFrame) {
        if (validCoordinate(currentFrame.latitude(), currentFrame.longitude())) {
            return true;
        }
        return currentFrame.routeCoordinates().stream()
                .anyMatch(point -> point.length >= 2 && validCoordinate(point[0], point[1]));
    }

    private void drawHud(Canvas canvas, String primary, String secondary, String status, int progress) {
        Rect safe = safeArea;
        if (safe == null || safe.isEmpty()) {
            safe = new Rect(0, 0, canvas.getWidth(), canvas.getHeight());
        }
        float left = Math.max(16f, safe.left + 16f);
        float right = Math.min(canvas.getWidth() - 16f, safe.right - 16f);
        float bottom = Math.min(canvas.getHeight() - 8f, safe.bottom - 24f);
        float top = Math.max(safe.top + 10f, bottom - 126f);
        canvas.drawRoundRect(new RectF(left, top, right, bottom), 18f, 18f, hudPaint);
        textPaint.setTextSize(27f);
        canvas.drawText(primary, left + 18f, top + 34f, textPaint);
        textPaint.setTextSize(20f);
        canvas.drawText(secondary, left + 18f, top + 67f, textPaint);
        canvas.drawText(status + " · Trayecto " + progress + "%", left + 18f, top + 98f, textPaint);
        textPaint.setTextSize(15f);
        canvas.drawText("© OpenStreetMap contributors", left + 10f, Math.min(canvas.getHeight() - 6f,
                safe.bottom - 4f), textPaint);
    }

    private double[] mapCenter(MapFrame currentFrame) {
        if (validCoordinate(currentFrame.latitude(), currentFrame.longitude())) {
            return new double[] { currentFrame.latitude(), currentFrame.longitude() };
        }
        if (!currentFrame.routeCoordinates().isEmpty()) {
            double[] first = currentFrame.routeCoordinates().get(0);
            if (first.length >= 2 && validCoordinate(first[0], first[1])) {
                return new double[] { first[0], first[1] };
            }
        }
        return new double[] { 0.0, 0.0 };
    }

    private double worldSize(int currentZoom) {
        return TILE_SIZE * Math.pow(2, currentZoom);
    }

    private double worldX(double longitude, double worldSize) {
        return (longitude + 180.0) / 360.0 * worldSize;
    }

    private double worldY(double latitude, double worldSize) {
        double clamped = Math.max(-MAX_MERCATOR_LAT, Math.min(MAX_MERCATOR_LAT, latitude));
        double radians = Math.toRadians(clamped);
        return (1.0 - Math.log(Math.tan(radians) + 1.0 / Math.cos(radians)) / Math.PI) / 2.0 * worldSize;
    }

    private boolean validCoordinate(double latitude, double longitude) {
        return Double.isFinite(latitude) && Double.isFinite(longitude)
                && latitude >= -90.0 && latitude <= 90.0
                && longitude >= -180.0 && longitude <= 180.0;
    }

    record MapFrame(long vehicleId, double latitude, double longitude, double speedKmh, double energyPercent,
            String energyType, String gearPosition, String status, int progress, double rangeKm, int rpm,
            List<double[]> routeCoordinates) {

        static MapFrame fromJson(JSONObject state) throws org.json.JSONException {
            JSONObject vehicle = state.getJSONObject("vehicle");
            JSONArray route = state.optJSONArray("routeCoordinates");
            List<double[]> coordinates = new ArrayList<>();
            if (route != null) {
                for (int index = 0; index < route.length(); index++) {
                    JSONArray point = route.optJSONArray(index);
                    if (point != null && point.length() >= 2) {
                        coordinates.add(new double[] { point.optDouble(0, Double.NaN),
                                point.optDouble(1, Double.NaN) });
                    }
                }
            }
            return new MapFrame(vehicle.optLong("vehicleId"), vehicle.optDouble("latitude", Double.NaN),
                    vehicle.optDouble("longitude", Double.NaN), vehicle.optDouble("speedKmh", 0.0),
                    vehicle.optDouble("energyPercent", 0.0), vehicle.optString("energyType", "BATTERY_ELECTRIC"),
                    vehicle.optString("gearPosition", "P"), vehicle.optString("status", "IDLE"),
                    vehicle.optInt("progress", 0), state.optDouble("estimatedRangeKm", Double.NaN),
                    state.optInt("estimatedRpm", 0), Collections.unmodifiableList(new ArrayList<>(coordinates)));
        }

        String primaryTelemetry() {
            String energyLabel = "FUEL".equals(energyType) ? "combustible" : "batería";
            return String.format(Locale.getDefault(), "%.0f km/h · %s · %.0f%% %s",
                    speedKmh, gearPosition, energyPercent, energyLabel);
        }

        String secondaryTelemetry() {
            String range = Double.isFinite(rangeKm) ? String.format(Locale.getDefault(), "%.0f km", rangeKm) : "-- km";
            return "RPM virtual aprox. " + rpm + " · autonomía estimada " + range;
        }
    }
}
