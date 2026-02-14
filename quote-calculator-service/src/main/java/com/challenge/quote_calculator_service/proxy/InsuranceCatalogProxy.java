package com.challenge.quote_calculator_service.proxy;

import com.challenge.quote_calculator_service.dto.InsurancePlanResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.function.Function;

@Component
@Slf4j
public class InsuranceCatalogProxy {

    private final WebClient webClient;

    public InsuranceCatalogProxy(WebClient.Builder webClientBuilder,
                                 @Value("${insurance-catalog-service.url}") String insuranceCatalogUrl) {
        this.webClient = webClientBuilder.baseUrl(insuranceCatalogUrl).build();
    }


    public Flux<InsurancePlanResponse> getActivePlans() {

        Function<WebClientResponseException, Flux<InsurancePlanResponse>> errorHandler = ex -> {
            if (ex.getStatusCode().value() == 404) {
                log.error("No hay planes activos");
                return Flux.empty();
            }

            return Flux.error(new RuntimeException("Error al consumir el servicio de catalogo de planes"));
        };

        return webClient.get()
                .uri("/api/insurance-catalog/active")
                .retrieve()
                .bodyToFlux(InsurancePlanResponse.class)
                .timeout(Duration.ofSeconds(10))
                .onErrorResume(WebClientResponseException.class, errorHandler);// Manejo de errores reactivo

    }
}
