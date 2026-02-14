package com.challenge.quote_calculator_service.service;

import com.challenge.quote_calculator_service.dto.UserProspectRequest;
import com.challenge.quote_calculator_service.dto.UserProspectResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface QuoteCalculatorService {

    Flux<UserProspectResponse> createQuoteForUser(UserProspectRequest userProspectRequest);

}
