package com.challenge.quote_calculator_service.controller;

import com.challenge.quote_calculator_service.dto.UserProspectRequest;
import com.challenge.quote_calculator_service.dto.UserProspectResponse;
import com.challenge.quote_calculator_service.service.QuoteCalculatorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * Controlador REST para la gestión de cálculo de cotizaciones de seguros.
 *
 * <p>Este controlador proporciona endpoints para procesar solicitudes de cotización
 * de seguros para prospectos de usuarios. Utiliza programación reactiva con Project Reactor
 * para manejar flujos de datos asincronos y procesamiento en streaming.</p>
 *
 * <p>Todas las operaciones utilizan Server-Sent Events (SSE) para transmitir resultados
 * en tiempo real al cliente mediante la producción de {@link reactor.core.publisher.Flux}.</p>
 *
 * @author Challenge Team
 * @version 1.0
 */
@RestController
@RequestMapping("api/quote-calculator")
@Slf4j
public class QuoteCalculatorController {

    private final QuoteCalculatorService quoteCalculatorService;

    /**
     * Constructor que inyecta la dependencia del servicio de cálculo de cotizaciones.
     *
     * @param quoteCalculatorService el servicio responsable de la lógica de cálculo
     *                                 de cotizaciones. No puede ser nulo.
     */
    public QuoteCalculatorController(QuoteCalculatorService quoteCalculatorService) {
        this.quoteCalculatorService = quoteCalculatorService;
    }

    /**
     * Crea una cotización de seguros para un prospecto de usuario.
     *
     * <p>Este endpoint procesa una solicitud POST que recibe datos del prospecto
     * en formato JSON y devuelve un flujo reactivo de respuestas de cotización
     * mediante Server-Sent Events (SSE).</p>
     *
     * <p><strong>Características:</strong></p>
     * <ul>
     *   <li>Acepta solicitudes JSON con datos del prospecto</li>
     *   <li>Devuelve un flujo de eventos en streaming</li>
     *   <li>Utiliza programación reactiva para manejo asincrónico</li>
     * </ul>
     *
     * @param userProspectRequest la solicitud con los datos del prospecto de usuario.
     *                             Contiene información requerida para calcular la cotización.
     * @return un {@link Flux} de {@link UserProspectResponse} que emite las respuestas
     *         de cotización de seguros en streaming. El flujo puede emitir múltiples
     *         elementos o completarse según el resultado del procesamiento.
     *
     * @see UserProspectRequest
     * @see UserProspectResponse
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<UserProspectResponse> createQuoteForUser(@RequestBody UserProspectRequest userProspectRequest) {

        return quoteCalculatorService.createQuoteForUser(userProspectRequest);
    }

}
