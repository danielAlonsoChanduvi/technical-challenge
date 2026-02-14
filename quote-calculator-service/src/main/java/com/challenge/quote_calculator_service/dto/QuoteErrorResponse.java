package com.challenge.quote_calculator_service.dto;

import java.time.LocalDateTime;

public record QuoteErrorResponse(
        String message,
        LocalDateTime timestamp
) {
    public QuoteErrorResponse(String message) {
        this(message, LocalDateTime.now());
    }
}
