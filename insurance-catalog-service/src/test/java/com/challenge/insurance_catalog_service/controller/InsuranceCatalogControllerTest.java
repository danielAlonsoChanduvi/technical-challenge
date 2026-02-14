package com.challenge.insurance_catalog_service.controller;

import com.challenge.insurance_catalog_service.dto.InsurancePlanResponse;
import com.challenge.insurance_catalog_service.entity.InsurancePlan;
import com.challenge.insurance_catalog_service.service.InsuranceCatalogService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import static org.assertj.core.api.Assertions.assertThat;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InsuranceCatalogControllerTest {

    @Mock
    private InsuranceCatalogService insuranceCatalogService;

    @InjectMocks
    private InsuranceCatalogController insuranceCatalogController;

    @Test
    void executeGetAllPlansWhenSuccessfully() {

        InsurancePlanResponse planone = new InsurancePlanResponse(
                "Plan Básico",
                50.0,
                "18-65",
                true
        );

        InsurancePlanResponse plantwo = new InsurancePlanResponse(
                "Plan Estándar",
                100.0,
                "18-70",
                false
        );

        when(insuranceCatalogService.getAllPlans())
                .thenReturn(Flux.just(planone, plantwo));

        Flux<InsurancePlanResponse> result = insuranceCatalogController.getAllPlans();

        StepVerifier.create(result)
                .assertNext(plan -> {
                    assertThat(plan.namePlan()).isEqualTo("Plan Básico");
                    assertThat(plan.pricePlan()).isEqualTo(50.0);
                })
                .assertNext(plan -> {
                    assertThat(plan.namePlan()).isEqualTo("Plan Estándar");
                    assertThat(plan.pricePlan()).isEqualTo(100.0);
                })
                .verifyComplete();


    }

    @Test
    void executeGetActivePlansWhenSuccessfully() {

        InsurancePlan planone = new InsurancePlan(
                1L,
                "Plan Básico",
                50.0,
                18,
                65,
                true
        );

        InsurancePlan plantwo = new InsurancePlan(
                1L,
                "Plan Estándar",
                100.0,
                18,
                70,
                true
        );

        when(insuranceCatalogService.getActivePlans())
                .thenReturn(Flux.just(planone, plantwo));

        Flux<InsurancePlan> result = insuranceCatalogController.getActivePlans();

        StepVerifier.create(result)
                .assertNext(plan -> {
                    assertThat(plan.name()).isEqualTo("Plan Básico");
                    assertThat(plan.basePrice()).isEqualTo(50.0);
                })
                .assertNext(plan -> {
                    assertThat(plan.name()).isEqualTo("Plan Estándar");
                    assertThat(plan.basePrice()).isEqualTo(100.0);
                })
                .verifyComplete();


    }
}
