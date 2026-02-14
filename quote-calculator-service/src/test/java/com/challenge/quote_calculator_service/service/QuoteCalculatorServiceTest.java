package com.challenge.quote_calculator_service.service;

import com.challenge.quote_calculator_service.dto.InsurancePlanResponse;
import com.challenge.quote_calculator_service.dto.UserProspectRequest;
import com.challenge.quote_calculator_service.dto.UserProspectResponse;
import com.challenge.quote_calculator_service.exception.PlanNotFoudForUser;
import com.challenge.quote_calculator_service.proxy.InsuranceCatalogProxy;
import com.challenge.quote_calculator_service.service.impl.QuoteCalculatorServiceImpl;
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
@DisplayName("Pruebas unitarias de QuoteCalculatorServiceImpl")
public class QuoteCalculatorServiceTest {

    @Mock
    private InsuranceCatalogProxy insuranceCatalogProxy;

    @InjectMocks
    private QuoteCalculatorServiceImpl quoteCalculatorService;

    private UserProspectRequest userProspectRequest;
    private InsurancePlanResponse planEligible1;
    private InsurancePlanResponse planEligible2;
    private InsurancePlanResponse planNotEligibleByAge;

    @BeforeEach
    void setUp() {
        // Arrange: Preparar datos de prueba
        userProspectRequest = new UserProspectRequest(
                "71195649",
                "Daniel Chanduvi",
                "Backend Developer",
                50000.00,  // Ingreso anual mayor a 30000
                false,     // No fumador
                29         // Edad dentro de rango típico
        );

        // Planes elegibles para el usuario
        planEligible1 = new InsurancePlanResponse(
                1L,
                "Seguro Básico",
                100.00,
                25,
                40,
                true
        );

        planEligible2 = new InsurancePlanResponse(
                2L,
                "Seguro Premium",
                150.00,
                20,
                65,
                true
        );

        // Plan no elegible por edad (usuario muy joven)
        planNotEligibleByAge = new InsurancePlanResponse(
                3L,
                "Seguro Senior",
                200.00,
                50,
                70,
                true
        );
    }

    @Test
    @DisplayName("Debe retornar múltiples cotizaciones cuando el usuario es elegible para varios planes")
    void testCreateQuoteForUserWithMultiplePlans() {
        // Arrange
        Flux<InsurancePlanResponse> plansFlux = Flux.just(planEligible1, planEligible2);
        when(insuranceCatalogProxy.getActivePlans()).thenReturn(plansFlux);

        // Act
        Flux<UserProspectResponse> result = quoteCalculatorService.createQuoteForUser(userProspectRequest);

        // Assert
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response).isNotNull();
                    assertThat(response.fullname()).isEqualTo("Daniel Chanduvi");
                    assertThat(response.planName()).isEqualTo("Seguro Básico");
                    assertThat(response.price()).isEqualTo(100.00); // No fumador, sin recargo
                })
                .assertNext(response -> {
                    assertThat(response).isNotNull();
                    assertThat(response.fullname()).isEqualTo("Daniel Chanduvi");
                    assertThat(response.planName()).isEqualTo("Seguro Premium");
                    assertThat(response.price()).isEqualTo(150.00); // No fumador, sin recargo
                })
                .verifyComplete();

        verify(insuranceCatalogProxy, times(1)).getActivePlans();
    }

    @Test
    @DisplayName("Debe aplicar recargo del 20% cuando el usuario es fumador")
    void testCreateQuoteForUserWithSmoker() {
        // Arrange
        UserProspectRequest smokerRequest = new UserProspectRequest(
                "12345678B",
                "Juan Pérez",
                "Ingeniero",
                60000.00,
                true,  // Es fumador
                35
        );

        Flux<InsurancePlanResponse> plansFlux = Flux.just(planEligible1);
        when(insuranceCatalogProxy.getActivePlans()).thenReturn(plansFlux);

        // Act
        Flux<UserProspectResponse> result = quoteCalculatorService.createQuoteForUser(smokerRequest);

        // Assert
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.fullname()).isEqualTo("Juan Pérez");
                    assertThat(response.planName()).isEqualTo("Seguro Básico");
                    assertThat(response.price()).isEqualTo(120.00); // 100 * 1.2 por ser fumador
                })
                .verifyComplete();

        verify(insuranceCatalogProxy, times(1)).getActivePlans();
    }

    @Test
    @DisplayName("Debe filtrar planes donde el usuario es menor que la edad mínima")
    void testCreateQuoteForUserUnderMinimumAge() {
        // Arrange
        UserProspectRequest youngRequest = new UserProspectRequest(
                "11111111A",
                "Carlos López",
                "Estudiante",
                40000.00,
                false,
                18  // Menor que minAge de planNotEligibleByAge
        );

        Flux<InsurancePlanResponse> plansFlux = Flux.just(planNotEligibleByAge);
        when(insuranceCatalogProxy.getActivePlans()).thenReturn(plansFlux);

        // Act
        Flux<UserProspectResponse> result = quoteCalculatorService.createQuoteForUser(youngRequest);

        // Assert
        StepVerifier.create(result)
                .verifyError(PlanNotFoudForUser.class);

        verify(insuranceCatalogProxy, times(1)).getActivePlans();
    }

    @Test
    @DisplayName("Debe filtrar planes donde el usuario es mayor que la edad máxima")
    void testCreateQuoteForUserAboveMaximumAge() {
        // Arrange
        UserProspectRequest elderlyRequest = new UserProspectRequest(
                "99999999Z",
                "María García",
                "Jubilada",
                35000.00,
                false,
                72  // Mayor que maxAge de planEligible1
        );

        Flux<InsurancePlanResponse> plansFlux = Flux.just(planEligible1);
        when(insuranceCatalogProxy.getActivePlans()).thenReturn(plansFlux);

        // Act
        Flux<UserProspectResponse> result = quoteCalculatorService.createQuoteForUser(elderlyRequest);

        // Assert
        StepVerifier.create(result)
                .verifyError(PlanNotFoudForUser.class);

        verify(insuranceCatalogProxy, times(1)).getActivePlans();
    }

    @Test
    @DisplayName("Debe filtrar planes cuando el ingreso anual es menor o igual a 30000")
    void testCreateQuoteForUserWithInsufficientIncome() {
        // Arrange
        UserProspectRequest lowIncomeRequest = new UserProspectRequest(
                "22222222B",
                "Pedro Sánchez",
                "Empleado",
                25000.00,  // Menor que 30000
                false,
                35
        );

        Flux<InsurancePlanResponse> plansFlux = Flux.just(planEligible1);
        when(insuranceCatalogProxy.getActivePlans()).thenReturn(plansFlux);

        // Act
        Flux<UserProspectResponse> result = quoteCalculatorService.createQuoteForUser(lowIncomeRequest);

        // Assert
        StepVerifier.create(result)
                .verifyError(PlanNotFoudForUser.class);

        verify(insuranceCatalogProxy, times(1)).getActivePlans();
    }

    @Test
    @DisplayName("Debe retornar una única cotización cuando solo un plan es elegible")
    void testCreateQuoteForUserWithSingleEligiblePlan() {
        // Arrange
        Flux<InsurancePlanResponse> plansFlux = Flux.just(planEligible1);
        when(insuranceCatalogProxy.getActivePlans()).thenReturn(plansFlux);

        // Act
        Flux<UserProspectResponse> result = quoteCalculatorService.createQuoteForUser(userProspectRequest);

        // Assert
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.fullname()).isEqualTo("Daniel Chanduvi");
                    assertThat(response.planName()).isEqualTo("Seguro Básico");
                    assertThat(response.price()).isEqualTo(100.00);
                })
                .verifyComplete();

        verify(insuranceCatalogProxy, times(1)).getActivePlans();
    }

    @Test
    @DisplayName("Debe lanzar PlanNotFoudForUser cuando no hay planes disponibles")
    void testCreateQuoteForUserWithNoPlans() {
        // Arrange
        Flux<InsurancePlanResponse> emptyFlux = Flux.empty();
        when(insuranceCatalogProxy.getActivePlans()).thenReturn(emptyFlux);

        // Act
        Flux<UserProspectResponse> result = quoteCalculatorService.createQuoteForUser(userProspectRequest);

        // Assert
        StepVerifier.create(result)
                .verifyError(PlanNotFoudForUser.class);

        verify(insuranceCatalogProxy, times(1)).getActivePlans();
    }

    @Test
    @DisplayName("Debe lanzar PlanNotFoudForUser cuando ningún plan cumple los criterios")
    void testCreateQuoteForUserWithNoPlansMeetingCriteria() {
        // Arrange: Usuario muy joven (10 años) e ingreso muy bajo (5000)
        // Planes disponibles: minAge=50, maxAge=70 y minAge=25, maxAge=40
        // Resultado: Usuario NO cumple con ningún criterio de edad ni ingreso
        UserProspectRequest ineligibleUser = new UserProspectRequest(
                "66666666G",
                "Lucas Moreno",
                "Estudiante",
                5000.00,   // Ingreso MUCHO menor que 30000
                false,
                10         // Edad MUCHO menor que minAge de 25
        );

        InsurancePlanResponse planHighAge = new InsurancePlanResponse(
                100L,
                "Seguro Mayor",
                200.00,
                50,
                70,
                true
        );

        InsurancePlanResponse planMediumAge = new InsurancePlanResponse(
                101L,
                "Seguro Adulto",
                150.00,
                25,
                40,
                true
        );

        Flux<InsurancePlanResponse> plansFlux = Flux.just(planHighAge, planMediumAge);
        when(insuranceCatalogProxy.getActivePlans()).thenReturn(plansFlux);

        // Act
        Flux<UserProspectResponse> result = quoteCalculatorService.createQuoteForUser(ineligibleUser);

        // Assert
        StepVerifier.create(result)
                .verifyError(PlanNotFoudForUser.class);

        verify(insuranceCatalogProxy, times(1)).getActivePlans();
    }

    @Test
    @DisplayName("Debe retornar error cuando el proxy lanza una excepción")
    void testCreateQuoteForUserWhenProxyThrowsException() {
        // Arrange
        Flux<InsurancePlanResponse> errorFlux = Flux.error(
                new RuntimeException("Error al conectar con el servicio de catálogo")
        );
        when(insuranceCatalogProxy.getActivePlans()).thenReturn(errorFlux);

        // Act
        Flux<UserProspectResponse> result = quoteCalculatorService.createQuoteForUser(userProspectRequest);

        // Assert
        StepVerifier.create(result)
                .verifyError(RuntimeException.class);

        verify(insuranceCatalogProxy, times(1)).getActivePlans();
    }

    @Test
    @DisplayName("Debe retornar cotizaciones con nombres correctamente mapeados")
    void testCreateQuoteForUserMapsFullnameCorrectly() {
        // Arrange
        UserProspectRequest specialNameRequest = new UserProspectRequest(
                "88888888C",
                "José María López Rodríguez",
                "Abogado",
                75000.00,
                false,
                30  // Edad dentro del rango 25-40
        );

        Flux<InsurancePlanResponse> plansFlux = Flux.just(planEligible1);
        when(insuranceCatalogProxy.getActivePlans()).thenReturn(plansFlux);

        // Act
        Flux<UserProspectResponse> result = quoteCalculatorService.createQuoteForUser(specialNameRequest);

        // Assert
        StepVerifier.create(result)
                .assertNext(response ->
                        assertThat(response.fullname()).isEqualTo("José María López Rodríguez")
                )
                .verifyComplete();

        verify(insuranceCatalogProxy, times(1)).getActivePlans();
    }

    @Test
    @DisplayName("Debe retornar cotización con plan name correcto")
    void testCreateQuoteForUserMapsPlanNameCorrectly() {
        // Arrange
        InsurancePlanResponse customPlan = new InsurancePlanResponse(
                5L,
                "Seguro Super Premium Plus",
                500.00,
                20,
                60,
                true
        );

        Flux<InsurancePlanResponse> plansFlux = Flux.just(customPlan);
        when(insuranceCatalogProxy.getActivePlans()).thenReturn(plansFlux);

        // Act
        Flux<UserProspectResponse> result = quoteCalculatorService.createQuoteForUser(userProspectRequest);

        // Assert
        StepVerifier.create(result)
                .assertNext(response ->
                        assertThat(response.planName()).isEqualTo("Seguro Super Premium Plus")
                )
                .verifyComplete();

        verify(insuranceCatalogProxy, times(1)).getActivePlans();
    }

    @Test
    @DisplayName("Debe aplicar recargo de 20% correctamente en diferentes precios")
    void testCreateQuoteForSmokerWithVariousPrices() {
        // Arrange
        UserProspectRequest smokerRequest = new UserProspectRequest(
                "33333333D",
                "Rosa Martínez",
                "Doctora",
                80000.00,
                true,  // Es fumador
                40
        );

        InsurancePlanResponse expensivePlan = new InsurancePlanResponse(
                6L,
                "Seguro Lujo",
                500.00,
                30,
                50,
                true
        );

        Flux<InsurancePlanResponse> plansFlux = Flux.just(expensivePlan);
        when(insuranceCatalogProxy.getActivePlans()).thenReturn(plansFlux);

        // Act
        Flux<UserProspectResponse> result = quoteCalculatorService.createQuoteForUser(smokerRequest);

        // Assert
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.price()).isEqualTo(600.00); // 500 * 1.2
                    assertThat(response.fullname()).isEqualTo("Rosa Martínez");
                })
                .verifyComplete();

        verify(insuranceCatalogProxy, times(1)).getActivePlans();
    }

    @Test
    @DisplayName("Debe mantener el orden de planes en el flujo")
    void testCreateQuoteForUserPreservesOrderOfPlans() {
        // Arrange
        InsurancePlanResponse plan1 = new InsurancePlanResponse(1L, "Plan A", 100.0, 20, 60, true);
        InsurancePlanResponse plan2 = new InsurancePlanResponse(2L, "Plan B", 150.0, 20, 60, true);
        InsurancePlanResponse plan3 = new InsurancePlanResponse(3L, "Plan C", 200.0, 20, 60, true);

        Flux<InsurancePlanResponse> plansFlux = Flux.just(plan1, plan2, plan3);
        when(insuranceCatalogProxy.getActivePlans()).thenReturn(plansFlux);

        // Act
        Flux<UserProspectResponse> result = quoteCalculatorService.createQuoteForUser(userProspectRequest);

        // Assert
        StepVerifier.create(result)
                .assertNext(response -> assertThat(response.planName()).isEqualTo("Plan A"))
                .assertNext(response -> assertThat(response.planName()).isEqualTo("Plan B"))
                .assertNext(response -> assertThat(response.planName()).isEqualTo("Plan C"))
                .verifyComplete();

        verify(insuranceCatalogProxy, times(1)).getActivePlans();
    }

    @Test
    @DisplayName("Debe filtrar correctamente planes con ingreso exactamente igual a 30000")
    void testCreateQuoteForUserWithIncomeExactly30000() {
        // Arrange
        UserProspectRequest borderlineRequest = new UserProspectRequest(
                "44444444E",
                "Francisco Torres",
                "Técnico",
                30000.00,  // Exactamente 30000 (no mayor, debe fallar)
                false,
                35
        );

        Flux<InsurancePlanResponse> plansFlux = Flux.just(planEligible1);
        when(insuranceCatalogProxy.getActivePlans()).thenReturn(plansFlux);

        // Act
        Flux<UserProspectResponse> result = quoteCalculatorService.createQuoteForUser(borderlineRequest);

        // Assert
        StepVerifier.create(result)
                .verifyError(PlanNotFoudForUser.class);

        verify(insuranceCatalogProxy, times(1)).getActivePlans();
    }

    @Test
    @DisplayName("Debe filtrar correctamente planes con ingreso justo por encima de 30000")
    void testCreateQuoteForUserWithIncomeAbove30000() {
        // Arrange
        UserProspectRequest borderlineRequest = new UserProspectRequest(
                "55555555F",
                "Elena Ruiz",
                "Consultora",
                30000.01,  // Apenas por encima de 30000
                false,
                30
        );

        Flux<InsurancePlanResponse> plansFlux = Flux.just(planEligible1);
        when(insuranceCatalogProxy.getActivePlans()).thenReturn(plansFlux);

        // Act
        Flux<UserProspectResponse> result = quoteCalculatorService.createQuoteForUser(borderlineRequest);

        // Assert
        StepVerifier.create(result)
                .assertNext(response -> {
                    assertThat(response.fullname()).isEqualTo("Elena Ruiz");
                    assertThat(response.price()).isEqualTo(100.00);
                })
                .verifyComplete();

        verify(insuranceCatalogProxy, times(1)).getActivePlans();
    }
}
