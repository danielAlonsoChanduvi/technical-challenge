package com.challenge.quote_calculator_service.controller;

import com.challenge.quote_calculator_service.dto.UserProspectRequest;
import com.challenge.quote_calculator_service.dto.UserProspectResponse;
import com.challenge.quote_calculator_service.service.QuoteCalculatorService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas unitarias de QuoteCalculatorController")
public class QuoteCalculatorControllerTest {

    @Mock
    private QuoteCalculatorService quoteCalculatorService;

    @InjectMocks
    private QuoteCalculatorController quoteCalculatorController;

    private UserProspectRequest userProspectRequest;
    private UserProspectResponse userProspectResponse1;
    private UserProspectResponse userProspectResponse2;

    @BeforeEach
    void setUp() {
        // Arrange: Preparar datos de prueba
        userProspectRequest = new UserProspectRequest(
                "71195649",
                "Daniel Chanduvi",
                "Backend Developer",
                50000.00,
                false,
                29
        );

        userProspectResponse1 = new UserProspectResponse(
                "Daniel Chanduvi",
                "Seguro Básico",
                100.00
        );

        userProspectResponse2 = new UserProspectResponse(
                "Daniel Chanduvi",
                "Seguro Premium",
                150.00
        );
    }

    @Test
    @DisplayName("Debe retornar Flux con múltiples cotizaciones cuando el servicio devuelve datos válidos")
    void testCreateQuoteForUserWithMultipleResponses() {
        // Arrange
        Flux<UserProspectResponse> expectedFlux = Flux.just(userProspectResponse1, userProspectResponse2);
        when(quoteCalculatorService.createQuoteForUser(any(UserProspectRequest.class)))
                .thenReturn(expectedFlux);

        // Act
        Flux<UserProspectResponse> actualFlux = quoteCalculatorController.createQuoteForUser(userProspectRequest);

        // Assert
        StepVerifier.create(actualFlux)
                .assertNext(response -> {
                    assertThat(response).isNotNull();
                    assertThat(response.fullname()).isEqualTo("Daniel Chanduvi");
                    assertThat(response.planName()).isEqualTo("Seguro Básico");
                    assertThat(response.price()).isEqualTo(100.00);
                })
                .assertNext(response -> {
                    assertThat(response).isNotNull();
                    assertThat(response.fullname()).isEqualTo("Daniel Chanduvi");
                    assertThat(response.planName()).isEqualTo("Seguro Premium");
                    assertThat(response.price()).isEqualTo(150.00);
                })
                .verifyComplete();

        verify(quoteCalculatorService, times(1)).createQuoteForUser(userProspectRequest);
    }

    @Test
    @DisplayName("Debe retornar Flux vacío cuando el servicio devuelve Flux vacío")
    void testCreateQuoteForUserWithEmptyFlux() {
        // Arrange
        Flux<UserProspectResponse> emptyFlux = Flux.empty();
        when(quoteCalculatorService.createQuoteForUser(any(UserProspectRequest.class)))
                .thenReturn(emptyFlux);

        // Act
        Flux<UserProspectResponse> actualFlux = quoteCalculatorController.createQuoteForUser(userProspectRequest);

        // Assert
        StepVerifier.create(actualFlux)
                .verifyComplete();

        verify(quoteCalculatorService, times(1)).createQuoteForUser(userProspectRequest);
    }

    @Test
    @DisplayName("Debe retornar una única cotización cuando el servicio devuelve un solo resultado")
    void testCreateQuoteForUserWithSingleResponse() {
        // Arrange
        Flux<UserProspectResponse> singleFlux = Flux.just(userProspectResponse1);
        when(quoteCalculatorService.createQuoteForUser(any(UserProspectRequest.class)))
                .thenReturn(singleFlux);

        // Act
        Flux<UserProspectResponse> actualFlux = quoteCalculatorController.createQuoteForUser(userProspectRequest);

        // Assert
        StepVerifier.create(actualFlux)
                .assertNext(response -> {
                    assertThat(response).isNotNull();
                    assertThat(response.fullname()).isEqualTo("Daniel Chanduvi");
                    assertThat(response.planName()).isEqualTo("Seguro Básico");
                    assertThat(response.price()).isEqualTo(100.00);
                })
                .verifyComplete();

        verify(quoteCalculatorService, times(1)).createQuoteForUser(userProspectRequest);
    }

    @Test
    @DisplayName("Debe propagar error cuando el servicio lanza una excepción")
    void testCreateQuoteForUserWithException() {
        // Arrange
        RuntimeException exception = new RuntimeException("Error al obtener planes");
        Flux<UserProspectResponse> errorFlux = Flux.error(exception);
        when(quoteCalculatorService.createQuoteForUser(any(UserProspectRequest.class)))
                .thenReturn(errorFlux);

        // Act
        Flux<UserProspectResponse> actualFlux = quoteCalculatorController.createQuoteForUser(userProspectRequest);

        // Assert
        StepVerifier.create(actualFlux)
                .verifyError(RuntimeException.class);

        verify(quoteCalculatorService, times(1)).createQuoteForUser(userProspectRequest);
    }

    @Test
    @DisplayName("Debe invocar al servicio exactamente una vez con el UserProspectRequest correcto")
    void testCreateQuoteForUserInvokesServiceOnce() {
        // Arrange
        Flux<UserProspectResponse> expectedFlux = Flux.just(userProspectResponse1);
        when(quoteCalculatorService.createQuoteForUser(any(UserProspectRequest.class)))
                .thenReturn(expectedFlux);

        // Act
        quoteCalculatorController.createQuoteForUser(userProspectRequest);

        // Assert
        verify(quoteCalculatorService, times(1)).createQuoteForUser(userProspectRequest);
        verifyNoMoreInteractions(quoteCalculatorService);
    }

    @Test
    @DisplayName("Debe retornar cotización con precio ajustado para fumador")
    void testCreateQuoteForUserWithSmoker() {
        // Arrange
        UserProspectRequest smokerRequest = new UserProspectRequest(
                "12345678B",
                "Juan Pérez",
                "Ingeniero",
                60000.00,
                true,
                35
        );

        UserProspectResponse smokerResponse = new UserProspectResponse(
                "Juan Pérez",
                "Seguro Estándar",
                120.00  // Precio con recargo por ser fumador
        );

        Flux<UserProspectResponse> smokerFlux = Flux.just(smokerResponse);
        when(quoteCalculatorService.createQuoteForUser(smokerRequest))
                .thenReturn(smokerFlux);

        // Act
        Flux<UserProspectResponse> actualFlux = quoteCalculatorController.createQuoteForUser(smokerRequest);

        // Assert
        StepVerifier.create(actualFlux)
                .assertNext(response -> {
                    assertThat(response.price()).isEqualTo(120.00);
                    assertThat(response.fullname()).isEqualTo("Juan Pérez");
                })
                .verifyComplete();

        verify(quoteCalculatorService, times(1)).createQuoteForUser(smokerRequest);
    }

    @Test
    @DisplayName("Debe manejar solicitudes con diferentes edades correctamente")
    void testCreateQuoteForUserWithDifferentAges() {
        // Arrange
        UserProspectRequest youngRequest = new UserProspectRequest(
                "11111111A",
                "Carlos López",
                "Estudiante",
                20000.00,
                false,
                22
        );

        UserProspectResponse youngResponse = new UserProspectResponse(
                "Carlos López",
                "Seguro Joven",
                80.00
        );

        Flux<UserProspectResponse> youngFlux = Flux.just(youngResponse);
        when(quoteCalculatorService.createQuoteForUser(youngRequest))
                .thenReturn(youngFlux);

        // Act
        Flux<UserProspectResponse> actualFlux = quoteCalculatorController.createQuoteForUser(youngRequest);

        // Assert
        StepVerifier.create(actualFlux)
                .assertNext(response -> {
                    assertThat(response.fullname()).isEqualTo("Carlos López");
                    assertThat(response.price()).isEqualTo(80.00);
                })
                .verifyComplete();

        verify(quoteCalculatorService, times(1)).createQuoteForUser(youngRequest);
    }

    @Test
    @DisplayName("Debe retornar Flux con múltiples cotizaciones en orden correcto")
    void testCreateQuoteForUserPreservesOrder() {
        // Arrange
        UserProspectResponse response1 = new UserProspectResponse("Daniel", "Plan A", 100.0);
        UserProspectResponse response2 = new UserProspectResponse("Daniel", "Plan B", 150.0);
        UserProspectResponse response3 = new UserProspectResponse("Daniel", "Plan C", 200.0);

        Flux<UserProspectResponse> orderedFlux = Flux.just(response1, response2, response3);
        when(quoteCalculatorService.createQuoteForUser(any(UserProspectRequest.class)))
                .thenReturn(orderedFlux);

        // Act
        Flux<UserProspectResponse> actualFlux = quoteCalculatorController.createQuoteForUser(userProspectRequest);

        // Assert
        StepVerifier.create(actualFlux)
                .assertNext(response -> assertThat(response.planName()).isEqualTo("Plan A"))
                .assertNext(response -> assertThat(response.planName()).isEqualTo("Plan B"))
                .assertNext(response -> assertThat(response.planName()).isEqualTo("Plan C"))
                .verifyComplete();

        verify(quoteCalculatorService, times(1)).createQuoteForUser(userProspectRequest);
    }

    @Test
    @DisplayName("Debe retornar cotizaciones con todos los campos correctamente mapeados")
    void testCreateQuoteForUserMapsAllFieldsCorrectly() {
        // Arrange
        UserProspectResponse expectedResponse = new UserProspectResponse(
                "Maria García",
                "Seguro Integral",
                250.50
        );

        Flux<UserProspectResponse> responseFlux = Flux.just(expectedResponse);
        when(quoteCalculatorService.createQuoteForUser(any(UserProspectRequest.class)))
                .thenReturn(responseFlux);

        // Act
        Flux<UserProspectResponse> actualFlux = quoteCalculatorController.createQuoteForUser(userProspectRequest);

        // Assert
        StepVerifier.create(actualFlux)
                .assertNext(response -> assertThat(response)
                        .hasFieldOrPropertyWithValue("fullname", "Maria García")
                        .hasFieldOrPropertyWithValue("planName", "Seguro Integral")
                        .hasFieldOrPropertyWithValue("price", 250.50))
                .verifyComplete();

        verify(quoteCalculatorService, times(1)).createQuoteForUser(userProspectRequest);
    }
}
