package com.challenge.insurance_catalog_service.service.impl;

import com.challenge.insurance_catalog_service.dto.InsurancePlanResponse;
import com.challenge.insurance_catalog_service.entity.InsurancePlan;
import com.challenge.insurance_catalog_service.exception.PlanNotFoundException;
import com.challenge.insurance_catalog_service.repository.InsurancePlanRepository;
import com.challenge.insurance_catalog_service.service.InsuranceCatalogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Supplier;

/**
 * Implementación del servicio de catálogo de seguros.
 * <p>
 * Proporciona la lógica de negocio para gestionar la consulta de planes de seguros
 * del catálogo. Utiliza programación reactiva (Project Reactor) para operaciones
 * asincrónicas y no bloqueantes.
 * </p>
 *
 * @author Challenge Insurance Catalog Service
 * @version 1.0
 * @since 2026-02-14
 * @see InsuranceCatalogService
 */
@Service
@Slf4j
public class InsuranceCatalogServiceImpl implements InsuranceCatalogService {

    private final InsurancePlanRepository insurancePlanRepository;

    /**
     * Constructor para inyectar la dependencia del repositorio de planes de seguros.
     *
     * @param insurancePlanRepository el repositorio encargado de la persistencia
     *                                 y consulta de planes de seguros en la base de datos
     */
    public InsuranceCatalogServiceImpl(InsurancePlanRepository insurancePlanRepository) {
        this.insurancePlanRepository = insurancePlanRepository;
    }

    /**
     * Obtiene todos los planes de seguros activos disponibles en el catálogo.
     * <p>
     * Consulta el repositorio para obtener planes con estado activo. Si no encuentra
     * ningún plan activo, emite una excepción {@link PlanNotFoundException}.
     * Las operaciones se ejecutan de forma asincrónica y reactiva.
     * </p>
     *
     * @return un {@link Flux} que emite cada {@link InsurancePlan} con estado activo.
     *         Si no hay planes activos, emite un error del tipo {@link PlanNotFoundException}
     * @throws PlanNotFoundException cuando no existen planes de seguros activos
     */
    @Override
    public Flux<InsurancePlan> getActivePlans() {

        Supplier<PlanNotFoundException> notFoundException =
                () -> new PlanNotFoundException("there are not plan active");

        return insurancePlanRepository.findAllActive()
                .switchIfEmpty(Mono.error(notFoundException));
    }

    /**
     * Obtiene todos los planes de seguros disponibles en el catálogo.
     * <p>
     * Consulta el repositorio para obtener la totalidad de planes registrados,
     * sin importar su estado activo. Transforma cada {@link InsurancePlan} a su
     * representación {@link InsurancePlanResponse} mediante mapping. Registra un
     * log informativo para cada plan encontrado y un log de error en caso de fallo.
     * </p>
     *
     * @return un {@link Flux} que emite cada {@link InsurancePlanResponse} disponible.
     *         Si ocurre un error durante la consulta, se registra en los logs
     */
    @Override
    public Flux<InsurancePlanResponse> getAllPlans() {

        return insurancePlanRepository.findAll()
                .map(InsurancePlanResponse::fromInsurancePlan)
                .doOnNext(response -> log.info("Found plan: {}", response.namePlan()))
                .doOnError(throwable -> log.error("Error getting plans"));
    }
}
