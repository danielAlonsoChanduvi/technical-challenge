package com.challenge.quote_calculator_service.service.impl;

import com.challenge.quote_calculator_service.dto.InsurancePlanResponse;
import com.challenge.quote_calculator_service.dto.UserProspectRequest;
import com.challenge.quote_calculator_service.dto.UserProspectResponse;
import com.challenge.quote_calculator_service.exception.PlanNotFoudForUser;
import com.challenge.quote_calculator_service.proxy.InsuranceCatalogProxy;
import com.challenge.quote_calculator_service.service.QuoteCalculatorService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Predicate;

/**
 * Implementación del servicio de cálculo de cotizaciones de seguros.
 *
 * <p>Esta clase implementa la lógica de negocio para generar cotizaciones de seguros
 * personalizadas basadas en el perfil del prospecto de usuario. Utiliza programación
 * reactiva con Project Reactor para procesar planes de seguros activos de forma asincrónica.</p>
 *
 * <p><strong>Criterios de elegibilidad:</strong></p>
 * <ul>
 *   <li>La edad del usuario debe estar dentro del rango permitido del plan</li>
 *   <li>El ingreso anual del usuario debe ser mayor a 30,000</li>
 *   <li>Se aplica un recargo del 20% en la prima base si el usuario es fumador</li>
 * </ul>
 *
 * @author Challenge Team
 * @version 1.0
 * @see QuoteCalculatorService
 * @see InsuranceCatalogProxy
 */
@Service
public class QuoteCalculatorServiceImpl implements QuoteCalculatorService {

    private final InsuranceCatalogProxy insuranceCatalogProxy;

    /**
     * Constructor que inyecta la dependencia del proxy del catálogo de seguros.
     *
     * @param insuranceCatalogProxy proxy responsable de obtener los planes de seguros
     *                              activos del catálogo. No puede ser nulo.
     */
    public QuoteCalculatorServiceImpl(InsuranceCatalogProxy insuranceCatalogProxy) {
        this.insuranceCatalogProxy = insuranceCatalogProxy;
    }

    /**
     * Crea una cotización de seguros personalizada para un prospecto de usuario.
     *
     * <p>Este método realiza el siguiente proceso:</p>
     * <ol>
     *   <li>Obtiene todos los planes de seguros activos del catálogo</li>
     *   <li>Filtra los planes según la edad del usuario (debe estar dentro del rango permitido)</li>
     *   <li>Valida que el ingreso anual sea suficiente (mayor a 30,000)</li>
     *   <li>Aplica los recargos correspondientes (ej: fumador = +20%)</li>
     *   <li>Retorna un flujo con todas las opciones de seguros elegibles</li>
     * </ol>
     *
     * <p><strong>Comportamiento en error:</strong></p>
     * <p>Si no hay planes que cumplan los criterios de elegibilidad, el flujo emite un error
     * de tipo {@link PlanNotFoudForUser} con el mensaje "No hay seguros para su perfil".</p>
     *
     * @param userProspectRequest la solicitud con los datos del prospecto incluyendo nombre,
     *                             edad, ingreso anual y estado de fumador. No puede ser nulo.
     * @return un {@link Flux} de {@link UserProspectResponse} que emite todas las cotizaciones
     *         de seguros elegibles para el usuario. Si no hay planes disponibles, el flujo
     *         contiene un error.
     *
     * @throws PlanNotFoudForUser si no existen planes de seguros que cumplan con los
     *                             criterios de elegibilidad del prospecto.
     *
     * @see UserProspectRequest
     * @see UserProspectResponse
     * @see InsurancePlanResponse
     */
    @Override
    public Flux<UserProspectResponse> createQuoteForUser(UserProspectRequest userProspectRequest) {


        Predicate<InsurancePlanResponse> isAgeEligible = plan ->
            userProspectRequest.age() >= plan.minAge() && userProspectRequest.age() <= plan.maxAge();

        Predicate<UserProspectRequest> hasMinimumIncome = user -> user.annualIncome() > 30000;

        return insuranceCatalogProxy.getActivePlans()
                .filter(isAgeEligible)
                .filter(plan -> hasMinimumIncome.test(userProspectRequest))
                .map(plan -> applyRiskSurcharge(plan, userProspectRequest))
                .switchIfEmpty(Mono.error(new PlanNotFoudForUser("No hay seguros para su perfil")));


    }

    /**
     * Aplica recargos de riesgo al precio base del plan según el perfil del usuario.
     *
     * <p>Este método calcula el precio final de la cotización considerando los factores
     * de riesgo del usuario. Actualmente, el único factor considerado es si el usuario
     * es fumador, lo cual incrementa la prima en un 20%.</p>
     *
     * <p><strong>Fórmula de cálculo:</strong></p>
     * <pre>
     * precio_final = usuario.isSmoker() ? plan.basePrice() * 1.2 : plan.basePrice()
     * </pre>
     *
     * @param plan el plan de seguros con el precio base a considerar. No puede ser nulo.
     * @param user el prospecto de usuario cuyos factores de riesgo se evaluarán. No puede ser nulo.
     * @return una {@link UserProspectResponse} con el nombre del usuario, nombre del plan
     *         y precio final calculado incluyendo todos los recargos aplicables.
     *
     * @see UserProspectResponse
     * @see InsurancePlanResponse
     */
    private UserProspectResponse applyRiskSurcharge(InsurancePlanResponse plan, UserProspectRequest user) {
        // Lógica de cálculo: Si es fumador, la prima sube un 20%
        double finalPrice = user.isSmoker() ? plan.basePrice() * 1.2 : plan.basePrice();
        return new UserProspectResponse(user.fullname(), plan.name(),finalPrice);
    }
}
