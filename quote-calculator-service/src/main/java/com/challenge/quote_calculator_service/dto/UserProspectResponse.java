package com.challenge.quote_calculator_service.dto;

public record UserProspectResponse(
        String fullname,
        String planName,
        Double price
) {
}
