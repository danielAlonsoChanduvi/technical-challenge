package com.challenge.insurance_catalog_service.service;

import com.challenge.insurance_catalog_service.dto.InsurancePlanResponse;
import com.challenge.insurance_catalog_service.entity.InsurancePlan;
import com.challenge.insurance_catalog_service.exception.PlanNotFoundException;
import com.challenge.insurance_catalog_service.repository.InsurancePlanRepository;
import com.challenge.insurance_catalog_service.service.impl.InsuranceCatalogServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InsuranceCatalogService Tests")
class InsuranceCatalogServiceTest {

    @Mock
    private InsurancePlanRepository insurancePlanRepository;

    @InjectMocks
    private InsuranceCatalogServiceImpl insuranceCatalogService;

    private InsurancePlan activePlan1;
    private InsurancePlan activePlan2;
    private InsurancePlan inactivePlan;

    @BeforeEach
    void setUp() {
        // Arrange: Preparar datos de prueba
        activePlan1 = new InsurancePlan(1L, "Plan Básico", 50.00, 18, 65, true);
        activePlan2 = new InsurancePlan(2L, "Plan Estándar", 100.00, 18, 70, true);
        inactivePlan = new InsurancePlan(3L, "Plan Premium", 150.00, 25, 75, false);
    }

    @Test
    @DisplayName("getActivePlans: Debe retornar todos los planes activos cuando existen")
    void getActivePlans_ShouldReturnActivePlans_WhenPlanExists() {
        // Arrange
        Flux<InsurancePlan> expectedFlux = Flux.just(activePlan1, activePlan2);
        when(insurancePlanRepository.findAllActive()).thenReturn(expectedFlux);

        // Act
        Flux<InsurancePlan> result = insuranceCatalogService.getActivePlans();

        // Assert
        StepVerifier.create(result)
                .assertNext(plan -> assertThat(plan.id()).isEqualTo(1L))
                .assertNext(plan -> assertThat(plan.id()).isEqualTo(2L))
                .verifyComplete();

        verify(insurancePlanRepository, times(1)).findAllActive();
    }

    @Test
    @DisplayName("getActivePlans: Debe retornar un plan activo")
    void getActivePlans_ShouldReturnSingleActivePlan() {
        // Arrange
        Flux<InsurancePlan> expectedFlux = Flux.just(activePlan1);
        when(insurancePlanRepository.findAllActive()).thenReturn(expectedFlux);

        // Act
        Flux<InsurancePlan> result = insuranceCatalogService.getActivePlans();

        // Assert
        StepVerifier.create(result)
                .assertNext(plan -> {
                    assertThat(plan.name()).isEqualTo("Plan Básico");
                    assertThat(plan.basePrice()).isEqualTo(50.00);
                    assertThat(plan.active()).isTrue();
                })
                .verifyComplete();

        verify(insurancePlanRepository, times(1)).findAllActive();
    }

    @Test
    @DisplayName("getActivePlans: Debe lanzar excepción cuando no hay planes activos")
    void getActivePlans_ShouldThrowException_WhenNoActivePlans() {
        // Arrange
        Flux<InsurancePlan> emptyFlux = Flux.empty();
        when(insurancePlanRepository.findAllActive()).thenReturn(emptyFlux);

        // Act
        Flux<InsurancePlan> result = insuranceCatalogService.getActivePlans();

        // Assert
        StepVerifier.create(result)
                .expectError(PlanNotFoundException.class)
                .verify();

        verify(insurancePlanRepository, times(1)).findAllActive();
    }

    @Test
    @DisplayName("getActivePlans: Debe propagar la excepción del repositorio")
    void getActivePlans_ShouldPropagateRepositoryError() {
        // Arrange
        Flux<InsurancePlan> errorFlux = Flux.error(new RuntimeException("Database error"));
        when(insurancePlanRepository.findAllActive()).thenReturn(errorFlux);

        // Act
        Flux<InsurancePlan> result = insuranceCatalogService.getActivePlans();

        // Assert
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();

        verify(insurancePlanRepository, times(1)).findAllActive();
    }

    @Test
    @DisplayName("getAllPlans: Debe retornar todos los planes con respuestas mapeadas")
    void getAllPlans_ShouldReturnAllPlansAsMapped() {
        // Arrange
        Flux<InsurancePlan> allPlans = Flux.just(activePlan1, activePlan2, inactivePlan);
        when(insurancePlanRepository.findAll()).thenReturn(allPlans);

        // Act
        Flux<InsurancePlanResponse> result = insuranceCatalogService.getAllPlans();

        // Assert
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.namePlan()).isEqualTo("Plan Básico");
                    assertThat(response.pricePlan()).isEqualTo(50.00);
                    assertThat(response.scopeAge()).isEqualTo("18-65");
                    assertThat(response.active()).isTrue();
                })
                .assertNext(response -> {
                    assertThat(response.namePlan()).isEqualTo("Plan Estándar");
                    assertThat(response.pricePlan()).isEqualTo(100.00);
                    assertThat(response.scopeAge()).isEqualTo("18-70");
                    assertThat(response.active()).isTrue();
                })
                .assertNext(response -> {
                    assertThat(response.namePlan()).isEqualTo("Plan Premium");
                    assertThat(response.pricePlan()).isEqualTo(150.00);
                    assertThat(response.scopeAge()).isEqualTo("25-75");
                    assertThat(response.active()).isFalse();
                })
                .verifyComplete();

        verify(insurancePlanRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("getAllPlans: Debe retornar lista vacía cuando no hay planes")
    void getAllPlans_ShouldReturnEmptyFlux_WhenNoPlanExists() {
        // Arrange
        Flux<InsurancePlan> emptyFlux = Flux.empty();
        when(insurancePlanRepository.findAll()).thenReturn(emptyFlux);

        // Act
        Flux<InsurancePlanResponse> result = insuranceCatalogService.getAllPlans();

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        verify(insurancePlanRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("getAllPlans: Debe retornar un solo plan mapeado correctamente")
    void getAllPlans_ShouldReturnSinglePlanMappedCorrectly() {
        // Arrange
        Flux<InsurancePlan> singlePlan = Flux.just(activePlan1);
        when(insurancePlanRepository.findAll()).thenReturn(singlePlan);

        // Act
        Flux<InsurancePlanResponse> result = insuranceCatalogService.getAllPlans();

        // Assert
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response)
                            .isNotNull()
                            .hasFieldOrPropertyWithValue("namePlan", "Plan Básico")
                            .hasFieldOrPropertyWithValue("pricePlan", 50.00)
                            .hasFieldOrPropertyWithValue("scopeAge", "18-65")
                            .hasFieldOrPropertyWithValue("active", true);
                })
                .verifyComplete();

        verify(insurancePlanRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("getAllPlans: Debe propagar errores del repositorio")
    void getAllPlans_ShouldPropagateRepositoryError() {
        // Arrange
        Flux<InsurancePlan> errorFlux = Flux.error(new RuntimeException("Connection failed"));
        when(insurancePlanRepository.findAll()).thenReturn(errorFlux);

        // Act
        Flux<InsurancePlanResponse> result = insuranceCatalogService.getAllPlans();

        // Assert
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();

        verify(insurancePlanRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("getAllPlans: Debe mapear correctamente el rango de edad")
    void getAllPlans_ShouldMapAgeScopeCorrectly() {
        // Arrange
        InsurancePlan planWithAges = new InsurancePlan(4L, "Plan Especial", 200.00, 30, 85, true);
        Flux<InsurancePlan> plans = Flux.just(planWithAges);
        when(insurancePlanRepository.findAll()).thenReturn(plans);

        // Act
        Flux<InsurancePlanResponse> result = insuranceCatalogService.getAllPlans();

        // Assert
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.scopeAge()).isEqualTo("30-85");
                })
                .verifyComplete();

        verify(insurancePlanRepository, times(1)).findAll();
    }
}
