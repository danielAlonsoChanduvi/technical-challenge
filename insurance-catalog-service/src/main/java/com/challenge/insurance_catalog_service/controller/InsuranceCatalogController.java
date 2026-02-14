package com.challenge.insurance_catalog_service.controller;

import com.challenge.insurance_catalog_service.dto.InsurancePlanResponse;
import com.challenge.insurance_catalog_service.entity.InsurancePlan;
import com.challenge.insurance_catalog_service.service.InsuranceCatalogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * Controlador REST para gestionar el catálogo de seguros.
 * <p>
 * Proporciona endpoints reactivos para consultar planes de seguros del catálogo.
 * Utiliza Server-Sent Events (SSE) para el streaming de datos en tiempo real.
 * </p>
 *
 * @author Challenge Insurance Catalog Service
 * @version 1.0
 * @since 2026-02-14
 */
@RestController
@RequestMapping("api/insurance-catalog")
@Slf4j
public class InsuranceCatalogController {

    private final InsuranceCatalogService insuranceCatalogService;

    /**
     * Constructor para inyectar la dependencia del servicio de catálogo de seguros.
     *
     * @param insuranceCatalogService el servicio que proporciona la lógica de negocio
     *                                 para gestionar planes de seguros
     */
    public InsuranceCatalogController(InsuranceCatalogService insuranceCatalogService) {
        this.insuranceCatalogService = insuranceCatalogService;
    }


    /**
     * Obtiene todos los planes de seguros disponibles en el catálogo.
     * <p>
     * Este endpoint retorna un flujo reactivo de {@link InsurancePlanResponse}
     * en formato de eventos de servidor (SSE). Los datos se envían progresivamente
     * al cliente en tiempo real.
     * </p>
     *
     * @return un {@link Flux} que emite {@link InsurancePlanResponse} para cada
     *         plan de seguros disponible
     */
    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<InsurancePlanResponse> getAllPlans() {
        return insuranceCatalogService.getAllPlans();
    }

    /**
     * Obtiene todos los planes de seguros activos del catálogo.
     * <p>
     * Este endpoint retorna un flujo reactivo de {@link InsurancePlan} que incluye
     * únicamente los planes con estado activo. Los datos se envían mediante
     * Server-Sent Events (SSE) de forma asincrónica.
     * </p>
     *
     * @return un {@link Flux} que emite {@link InsurancePlan} para cada plan
     *         de seguros con estado activo
     */
    @GetMapping(value = "/active", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<InsurancePlan> getActivePlans() {
        return insuranceCatalogService.getActivePlans();
    }


}
