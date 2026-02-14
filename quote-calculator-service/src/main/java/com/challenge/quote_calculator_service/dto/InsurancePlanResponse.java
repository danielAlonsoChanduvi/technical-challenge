package com.challenge.quote_calculator_service.dto;

public record InsurancePlanResponse(
        Long id,
        String name,
        Double basePrice,
        Integer minAge,
        Integer maxAge,
        Boolean active
) {
}
