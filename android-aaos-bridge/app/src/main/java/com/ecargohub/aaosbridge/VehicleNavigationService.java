package com.ecargohub.aaosbridge;

import android.content.Intent;
import android.content.pm.ApplicationInfo;

import androidx.car.app.CarAppService;
import androidx.car.app.Session;
import androidx.car.app.SessionInfo;
import androidx.car.app.validation.HostValidator;

public final class VehicleNavigationService extends CarAppService {
    @Override
    public Session onCreateSession(SessionInfo sessionInfo) {
        return new Session() {
            @Override
            public androidx.car.app.Screen onCreateScreen(Intent intent) {
                return new VehicleNavigationScreen(getCarContext());
            }
        };
    }

    @Override
    public HostValidator createHostValidator() {
        if ((getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR;
        }
        return new HostValidator.Builder(getApplicationContext())
                .addAllowedHosts(androidx.car.app.R.array.hosts_allowlist_sample)
                .build();
    }
}
