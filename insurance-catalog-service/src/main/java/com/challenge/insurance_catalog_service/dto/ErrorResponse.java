package com.challenge.insurance_catalog_service.dto;

import java.time.LocalDateTime;

public record ErrorResponse(
        String message,
        LocalDateTime timestamp
) {

    public ErrorResponse(String message) {
        this(message, LocalDateTime.now());
    }
}
