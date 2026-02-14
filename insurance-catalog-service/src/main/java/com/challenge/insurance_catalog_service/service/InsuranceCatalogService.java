package com.challenge.insurance_catalog_service.service;

import com.challenge.insurance_catalog_service.dto.InsurancePlanResponse;
import com.challenge.insurance_catalog_service.entity.InsurancePlan;
import reactor.core.publisher.Flux;


public interface InsuranceCatalogService {


    Flux<InsurancePlan> getActivePlans();
    Flux<InsurancePlanResponse> getAllPlans();

}
