package com.neueda.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import com.neueda.domain.CustomerStatus;

/**
 * DTO for complete customer profile information.
 */
public record CustomerProfileDTO(
    Long customerId,
    String name,
    LocalDate dob,
    String phoneNumber,
    String panNumber,
    String country,
    CustomerStatus status,
    LocalDateTime profileCreated,
    LocalDateTime lastUpdated
) {}

