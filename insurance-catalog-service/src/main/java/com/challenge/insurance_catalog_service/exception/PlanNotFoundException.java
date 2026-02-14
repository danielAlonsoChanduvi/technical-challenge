package com.challenge.insurance_catalog_service.exception;

public class PlanNotFoundException extends RuntimeException {
    public PlanNotFoundException(String  message) {
        super(message);
    }
}
