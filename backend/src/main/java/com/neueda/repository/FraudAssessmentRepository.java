package com.neueda.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.neueda.domain.FraudAssessmentRecord;
import com.neueda.domain.FraudDecision;

/**
 * Repository interface for Fraud Assessment data access operations.
 */
public interface FraudAssessmentRepository {
    
    /**
     * Insert a new fraud assessment record.
     */
    FraudAssessmentRecord save(FraudAssessmentRecord assessment);
    
    /**
     * Update an existing fraud assessment record.
     */
    FraudAssessmentRecord update(FraudAssessmentRecord assessment);
    
    /**
     * Retrieve a fraud assessment by ID.
     */
    Optional<FraudAssessmentRecord> findById(Long id);
    
    /**
     * Retrieve a fraud assessment by payment ID.
     */
    Optional<FraudAssessmentRecord> findByPaymentId(Long paymentId);
    
    /**
     * Retrieve all fraud assessments with filtering and pagination.
     */
    List<FraudAssessmentRecord> findAll(
        FraudDecision decision,
        String riskLevel,
        LocalDateTime dateFrom,
        LocalDateTime dateTo,
        int limit,
        int offset
    );
    
    /**
     * Count fraud assessments by status and risk level.
     */
    long countByDecision(FraudDecision decision);
    
    /**
     * Count assessments with pending review (SUSPICIOUS decision, no reviewer).
     */
    long countPendingReview();
    
    /**
     * Retrieve assessments pending admin review.
     */
    List<FraudAssessmentRecord> findPendingReview(int limit, int offset);
    
    /**
     * Get recent assessments ordered by creation date descending.
     */
    List<FraudAssessmentRecord> findRecent(int limit);
}

