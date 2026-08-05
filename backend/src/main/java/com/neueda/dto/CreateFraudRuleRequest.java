package com.neueda.dto;

/**
 * Request DTO for creating/updating fraud rules
 */
public record CreateFraudRuleRequest(
    String ruleName,
    String ruleType,
    String description,
    String severity,
    Integer orderOfExecution,
    java.math.BigDecimal weight,
    String ruleDefinitionJson,
    String triggeringConditionsJson
) {}

