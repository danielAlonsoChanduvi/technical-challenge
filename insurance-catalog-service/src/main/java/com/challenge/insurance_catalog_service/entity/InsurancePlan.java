package com.challenge.insurance_catalog_service.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("insuranceplan")
public record InsurancePlan(
        @Id Long id,
        String name,
        Double basePrice,
        Integer minAge,
        Integer maxAge,
        Boolean active
) {
}
