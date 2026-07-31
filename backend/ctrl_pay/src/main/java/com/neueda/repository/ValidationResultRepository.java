package com.neueda.repository;

import java.util.List;

import com.neueda.domain.ValidationResultRecord;

/**
 * Repository interface for ValidationResult data access operations.
 * Validation results are immutable audit logs of rule execution.
 * Uses Spring JdbcTemplate for SQL query execution.
 */
public interface ValidationResultRepository {
    
    /**
     * Insert a new validation result record into the database.
     * Results are append-only and should never be updated.
     * 
     * @param result validation result record to insert
     * @return validation result record with generated ID
     */
    ValidationResultRecord save(ValidationResultRecord result);
    
    /**
     * Retrieve all validation results for a specific payment.
     * Results are ordered by creation timestamp (chronological order).
     * 
     * @param paymentId payment ID
     * @return list of validation result records for the payment, ordered by created_at
     */
    List<ValidationResultRecord> findByPaymentId(Long paymentId);
    
    /**
     * Retrieve validation results for a specific payment within a time range.
     * 
     * @param paymentId payment ID
     * @param limit maximum number of results
     * @param offset number of results to skip
     * @return list of validation result records, ordered by created_at DESC
     */
    List<ValidationResultRecord> findByPaymentId(Long paymentId, int limit, int offset);
    
    /**
     * Retrieve validation results for a specific validation rule across all payments.
     * Used for compliance queries: "Which payments failed rule X?"
     * 
     * @param validationRuleId validation rule ID
     * @return list of validation result records for the rule
     */
    List<ValidationResultRecord> findByValidationRuleId(Long validationRuleId);
    
    /**
     * Retrieve failed validation results for a specific rule across all payments.
     * 
     * @param validationRuleId validation rule ID
     * @return list of failed validation results for the rule
     */
    List<ValidationResultRecord> findFailedByValidationRuleId(Long validationRuleId);
    
    /**
     * Count total validation results for a specific payment.
     * 
     * @param paymentId payment ID
     * @return count of results
     */
    long countByPaymentId(Long paymentId);
    
    /**
     * Count validation results for a specific rule across all payments.
     * 
     * @param validationRuleId validation rule ID
     * @return count of results
     */
    long countByValidationRuleId(Long validationRuleId);
    
    /**
     * Count failed validation results for a specific rule.
     * 
     * @param validationRuleId validation rule ID
     * @return count of failed results
     */
    long countFailedByValidationRuleId(Long validationRuleId);
    
    /**
     * Delete all validation results for a specific payment (cascade delete scenario).
     * 
     * @param paymentId payment ID
     * @return number of rows deleted
     */
    int deleteByPaymentId(Long paymentId);
}

