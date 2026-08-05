package com.neueda.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for Compliance Dashboard statistics.
 */
public record ComplianceDashboardDTO(
    Long validationFailures,
    Long fraudRuleChanges,
    Long validationRuleChanges,
    Long adminActions,
    Long auditEvents,
    Long manualFraudReviews,
    Long pendingReviews,
    List<RecentAuditEventDTO> recentAuditEvents,
    LocalDateTime timestamp
) {
    
    public record RecentAuditEventDTO(
        Long eventId,
        String eventType,
        String description,
        String performedBy,
        LocalDateTime timestamp
    ) {}
}

