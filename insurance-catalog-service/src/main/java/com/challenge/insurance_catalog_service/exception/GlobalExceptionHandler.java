package com.challenge.insurance_catalog_service.exception;

import com.challenge.insurance_catalog_service.dto.ErrorResponse;
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

    @ExceptionHandler(PlanNotFoundException.class)
    public Mono<ResponseEntity<ErrorResponse>> handlePlansNotFound(PlanNotFoundException exception,
                                                                   ServerWebExchange exchange) {

        ErrorResponse error = new ErrorResponse(exception.getMessage());

        return Mono.just(ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(error));
    }

}
