package com.challenge.insurance_catalog_service.dto;

import com.challenge.insurance_catalog_service.entity.InsurancePlan;

public record InsurancePlanResponse(
        String namePlan,
        Double pricePlan,
        String scopeAge,
        Boolean active
) {

    public static InsurancePlanResponse fromInsurancePlan(InsurancePlan insurancePlan) {
        return new InsurancePlanResponse(
                insurancePlan.name(),
                insurancePlan.basePrice(),
                String.valueOf(insurancePlan.minAge()) + '-' + String.valueOf(insurancePlan.maxAge()),
                insurancePlan.active()
        );
    }
}
