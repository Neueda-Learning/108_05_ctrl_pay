package com.neueda.dto;

import java.time.LocalDateTime;

/**
 * DTO for customer risk and security information.
 */
public record CustomerRiskDTO(
    String riskLevel,
    Long fraudFlags,
    Long rejectedTransactions,
    LocalDateTime lastSuspiciousActivity
) {}

