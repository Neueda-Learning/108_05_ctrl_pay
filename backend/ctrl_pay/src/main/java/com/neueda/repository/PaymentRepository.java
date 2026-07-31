package com.neueda.repository;

import java.util.List;
import java.util.Optional;

import com.neueda.domain.PaymentRecord;
import com.neueda.domain.PaymentStatus;

/**
 * Repository interface for Payment data access operations.
 * Uses Spring JdbcTemplate for SQL query execution.
 */
public interface PaymentRepository {
    
    /**
     * Insert a new payment record into the database.
     * 
     * @param payment payment record to insert
     * @return payment record with generated ID
     */
    PaymentRecord save(PaymentRecord payment);
    
    /**
     * Update an existing payment record.
     * 
     * @param payment payment record with updated values
     * @return updated payment record
     */
    PaymentRecord update(PaymentRecord payment);
    
    /**
     * Retrieve a payment by its ID.
     * 
     * @param id payment ID
     * @return payment record, or empty if not found
     */
    Optional<PaymentRecord> findById(Long id);
    
    /**
     * Retrieve a payment by its idempotency key.
     * Used for duplicate prevention and idempotency handling.
     * 
     * @param idempotencyKey idempotency key
     * @return payment record, or empty if not found
     */
    Optional<PaymentRecord> findByIdempotencyKey(String idempotencyKey);
    
    /**
     * Retrieve all payments with optional filtering and pagination.
     * 
     * @param status filter by payment status (null = no filter)
     * @param limit maximum number of results
     * @param offset number of results to skip
     * @return list of payment records
     */
    List<PaymentRecord> findAll(PaymentStatus status, int limit, int offset);
    
    /**
     * Retrieve all payments without pagination.
     * 
     * @return list of all payment records
     */
    List<PaymentRecord> findAll();
    
    /**
     * Count total number of payments.
     * 
     * @return count of payments
     */
    long count();
    
    /**
     * Count payments filtered by status.
     * 
     * @param status payment status filter
     * @return count of payments with given status
     */
    long countByStatus(PaymentStatus status);
    
    /**
     * Delete a payment by its ID.
     * 
     * @param id payment ID
     * @return true if payment was deleted, false if not found
     */
    boolean deleteById(Long id);
}

