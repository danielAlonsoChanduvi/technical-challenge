package com.challenge.quote_calculator_service.dto;

public record UserProspectRequest(
        String dni,
        String fullname,
        String occupation,
        Double annualIncome,
        Boolean isSmoker,
        Integer age
) {
}
