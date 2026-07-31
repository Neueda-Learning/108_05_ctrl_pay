package com.neueda.repository;

import java.util.List;

import com.neueda.domain.PaymentStatusHistoryRecord;
import com.neueda.domain.PaymentStatus;

/**
 * Repository interface for PaymentStatusHistory data access operations.
 * Status history records are immutable audit trails of payment status transitions.
 * Uses Spring JdbcTemplate for SQL query execution.
 */
public interface PaymentStatusHistoryRepository {
    
    /**
     * Insert a new status history record into the database.
     * Records are append-only and should never be updated or deleted.
     * 
     * @param history status history record to insert
     * @return status history record with generated ID
     */
    PaymentStatusHistoryRecord save(PaymentStatusHistoryRecord history);
    
    /**
     * Retrieve the complete status history for a specific payment.
     * Records are ordered chronologically by creation timestamp.
     * 
     * @param paymentId payment ID
     * @return list of status history records for the payment, ordered by created_at ASC
     */
    List<PaymentStatusHistoryRecord> findByPaymentId(Long paymentId);
    
    /**
     * Retrieve status history for a payment with pagination.
     * 
     * @param paymentId payment ID
     * @param limit maximum number of results
     * @param offset number of results to skip
     * @return list of status history records, ordered by created_at DESC
     */
    List<PaymentStatusHistoryRecord> findByPaymentId(Long paymentId, int limit, int offset);
    
    /**
     * Get the most recent status transition for a payment.
     * 
     * @param paymentId payment ID
     * @return the most recent status history record, or empty if no transitions exist
     */
    PaymentStatusHistoryRecord findLatestByPaymentId(Long paymentId);
    
    /**
     * Retrieve all status transitions to a specific status across all payments.
     * Useful for analytics: "How many payments reached COMPLETED status?"
     * 
     * @param status target status
     * @return list of history records for transitions to that status
     */
    List<PaymentStatusHistoryRecord> findByNewStatus(PaymentStatus status);
    
    /**
     * Retrieve all status transitions from a specific status across all payments.
     * 
     * @param status source status
     * @return list of history records for transitions from that status
     */
    List<PaymentStatusHistoryRecord> findByOldStatus(PaymentStatus status);
    
    /**
     * Count status history records for a specific payment.
     * 
     * @param paymentId payment ID
     * @return count of transitions for the payment
     */
    long countByPaymentId(Long paymentId);
    
    /**
     * Delete all status history records for a specific payment (cascade delete scenario).
     * 
     * @param paymentId payment ID
     * @return number of rows deleted
     */
    int deleteByPaymentId(Long paymentId);
    
    /**
     * Count total status history records across all payments.
     * 
     * @return count of all history records
     */
    long count();
}

