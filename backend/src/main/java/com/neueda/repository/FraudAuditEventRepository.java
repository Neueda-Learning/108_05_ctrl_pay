package com.neueda.repository;

import java.util.List;
import java.util.Optional;

import com.neueda.domain.FraudAuditEventRecord;

/**
 * Repository for fraud audit events.
 * Stores complete audit trail of fraud assessment workflow for compliance and forensics.
 */
public interface FraudAuditEventRepository {
    
    /**
     * Save a new audit event
     */
    FraudAuditEventRecord save(FraudAuditEventRecord event);
    
    /**
     * Find event by ID
     */
    Optional<FraudAuditEventRecord> findById(Long id);
    
    /**
     * Get all events for an assessment (ordered by timestamp)
     */
    List<FraudAuditEventRecord> findByAssessmentId(Long assessmentId);
    
    /**
     * Get events of a specific type
     */
    List<FraudAuditEventRecord> findByEventType(String eventType);
    
    /**
     * Get all events triggered by user
     */
    List<FraudAuditEventRecord> findByTriggeredByUser(String userId);
    
    /**
     * Count events by type for analytics
     */
    long countByEventType(String eventType);
}

