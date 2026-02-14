package com.challenge.insurance_catalog_service.repository;

import com.challenge.insurance_catalog_service.entity.InsurancePlan;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface InsurancePlanRepository extends ReactiveCrudRepository<InsurancePlan, Long> {

    @Query("SELECT * FROM insuranceplan WHERE  active = true")
    Flux<InsurancePlan> findAllActive();
}
