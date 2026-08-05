package com.neueda.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO for Fraud & Risk Dashboard statistics.
 */
public record FraudDashboardDTO(
    Long totalFraudChecks,
    Long fraudDetectedCount,
    Double fraudPreventionPercentage,
    Long suspiciousTransactions,
    Long rejectedTransactions,
    Map<String, Long> fraudScoreDistribution,
    Map<String, Long> riskLevelDistribution,
    List<MostTriggeredRuleDTO> mostTriggeredRules,
    LocalDateTime timestamp
) {

    public record MostTriggeredRuleDTO(
        Long ruleId,
        String ruleName,
        Long executionCount,
        Double effectivenessPercentage
    ) {}
}

