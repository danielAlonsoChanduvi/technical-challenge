package com.challenge.quote_calculator_service.exception;

public class PlanNotFoudForUser extends RuntimeException {
    public PlanNotFoudForUser(String message) {
        super(message);
    }
}
