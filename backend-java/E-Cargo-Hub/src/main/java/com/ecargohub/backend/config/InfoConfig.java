package com.ecargohub.backend.config;

import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Map;

@Component
public class InfoConfig implements InfoContributor {

    @Override
    public void contribute(Info.Builder builder) {
        builder.withDetail("app", Map.of("name", "E-Cargo Hub", "description", "Backend de simulación logística",
                "startedAt", OffsetDateTime.now().toString()));
    }
}