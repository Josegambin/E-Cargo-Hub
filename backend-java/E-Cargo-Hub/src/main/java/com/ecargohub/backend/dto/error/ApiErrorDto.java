package com.ecargohub.backend.dto.error;

import lombok.Builder;

import java.time.OffsetDateTime;

@Builder
public record ApiErrorDto(
        OffsetDateTime timestamp,
        Integer status,
        String error,
        String message,
        String path
) {}