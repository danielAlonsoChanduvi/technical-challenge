package com.challenge.quote_calculator_service.exception;

import com.challenge.quote_calculator_service.dto.QuoteErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(PlanNotFoudForUser.class)
    public Mono<ResponseEntity<QuoteErrorResponse>> handlePlansNotFound(PlanNotFoudForUser exception,
                                                                        ServerWebExchange exchange) {

        QuoteErrorResponse error = new QuoteErrorResponse(exception.getMessage());

        return Mono.just(ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(error));
    }
}
