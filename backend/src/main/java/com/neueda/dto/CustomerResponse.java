package com.neueda.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.neueda.domain.CustomerStatus;

/**
 * Response DTO for customer profile details.
 */
public record CustomerResponse(
    Long customerId,
    String name,
    LocalDate dob,
    String phoneNumber,
    String panNumber,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime profileCreated,
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    LocalDateTime lastUpdated,
    String country,
    CustomerStatus customerAccountStatus
) {
}

