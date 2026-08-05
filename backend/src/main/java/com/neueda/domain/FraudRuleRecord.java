package com.neueda.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Fraud Rule Record - Represents a configurable fraud detection rule
 * 
 * Records are immutable - use the constructor to create; use copyWith() for modifications
 */
public record FraudRuleRecord(
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
) {
    
    public FraudRuleRecord {
        // Validation
        if (ruleName == null || ruleName.isBlank()) {
            throw new IllegalArgumentException("Rule name cannot be null or blank");
        }
        if (ruleType == null || ruleType.isBlank()) {
            throw new IllegalArgumentException("Rule type cannot be null or blank");
        }
    }
    
    /**
     * Create a new fraud rule (for insertion)
     */
    public static FraudRuleRecord create(
        String ruleName,
        String ruleType,
        String description,
        String severity,
        Integer orderOfExecution,
        BigDecimal weight,
        String ruleDefinitionJson,
        String triggeringConditionsJson
    ) {
        return new FraudRuleRecord(
            null,
            ruleName,
            ruleType,
            description,
            true,
            severity,
            orderOfExecution,
            weight,
            ruleDefinitionJson,
            triggeringConditionsJson,
            0,
            LocalDateTime.now(),
            LocalDateTime.now(),
            null,
            null
        );
    }
    
    /**
     * Create a copy with modifications
     */
    public FraudRuleRecord withIsActive(Boolean active) {
        return new FraudRuleRecord(
            this.id, this.ruleName, this.ruleType, this.description, active,
            this.severity, this.orderOfExecution, this.weight, 
            this.ruleDefinitionJson, this.triggeringConditionsJson, this.mockScore,
            this.createdAt, LocalDateTime.now(), this.createdBy, "SYSTEM"
        );
    }
    
    public FraudRuleRecord withUpdatedData(
        String description,
        String ruleType,
        String severity,
        Integer orderOfExecution,
        BigDecimal weight,
        String ruleDefinitionJson,
        String triggeringConditionsJson
    ) {
        return new FraudRuleRecord(
            this.id,
            this.ruleName,
            ruleType,
            description,
            this.isActive,
            severity,
            orderOfExecution,
            weight,
            ruleDefinitionJson,
            triggeringConditionsJson,
            this.mockScore,
            this.createdAt,
            LocalDateTime.now(),
            this.createdBy,
            "SYSTEM"
        );
    }
}

