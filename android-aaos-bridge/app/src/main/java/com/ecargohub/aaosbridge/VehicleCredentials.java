package com.ecargohub.aaosbridge;

import android.content.Context;
import android.content.SharedPreferences;

final class VehicleCredentials {
    static final String PREFERENCES_NAME = "ecargo_device_credentials";
    static final String DEVICE_TOKEN_KEY = "deviceToken";

    private VehicleCredentials() {
    }

    static String loadToken(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        String token = preferences.getString(DEVICE_TOKEN_KEY, null);
        if (token != null && !token.isBlank()) {
            return token;
        }
        SharedPreferences legacy = context.getSharedPreferences(BridgeActivity.class.getSimpleName(),
                Context.MODE_PRIVATE);
        token = legacy.getString(DEVICE_TOKEN_KEY, null);
        if (token != null && !token.isBlank()) {
            saveToken(context, token);
            legacy.edit().remove(DEVICE_TOKEN_KEY).apply();
        }
        return token;
    }

    static void saveToken(Context context, String token) {
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(DEVICE_TOKEN_KEY, token)
                .apply();
    }

    static void clearToken(Context context) {
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(DEVICE_TOKEN_KEY)
                .apply();
    }
}
