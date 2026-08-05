package com.neueda.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO for fraud rules
 */
public record FraudRuleResponse(
    Long id,
    String ruleName,
    String ruleType,
    String description,
    Boolean isActive,
    String severity,
    Integer orderOfExecution,
    BigDecimal weight,
    String ruleDefinitionJson,
    String triggeringConditionsJson,
    Integer mockScore,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    String createdBy,
    String updatedBy
) {}

