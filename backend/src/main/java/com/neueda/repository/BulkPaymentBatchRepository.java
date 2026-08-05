package com.neueda.repository;

import com.neueda.domain.BulkPaymentBatchRecord;
import com.neueda.domain.BulkPaymentBatchStatus;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for bulk payment batch persistence.
 */
public interface BulkPaymentBatchRepository {
    
    /**
     * Create a new bulk payment batch record.
     */
    BulkPaymentBatchRecord create(BulkPaymentBatchRecord batch);
    
    /**
     * Retrieve a batch by ID.
     */
    Optional<BulkPaymentBatchRecord> findById(Long batchId);
    
    /**
     * Retrieve a batch by batch reference.
     */
    Optional<BulkPaymentBatchRecord> findByReference(String batchReference);
    
    /**
     * Retrieve a batch by idempotency key.
     */
    Optional<BulkPaymentBatchRecord> findByIdempotencyKey(String idempotencyKey);
    
    /**
     * Update an existing batch record.
     */
    void update(BulkPaymentBatchRecord batch);
    
    /**
     * List all batches for a user with pagination.
     */
    List<BulkPaymentBatchRecord> findByCreatedBy(String userId, int limit, int offset);
    
    /**
     * List batches by status.
     */
    List<BulkPaymentBatchRecord> findByStatus(BulkPaymentBatchStatus status, int limit, int offset);
    
    /**
     * List batches by source account.
     */
    List<BulkPaymentBatchRecord> findBySourceAccount(String sourceAccount, int limit, int offset);
    
    /**
     * Find all batches in a given status for processing.
     */
    List<BulkPaymentBatchRecord> findByStatusForProcessing(BulkPaymentBatchStatus status);
    
    /**
     * Count batches by status.
     */
    int countByStatus(BulkPaymentBatchStatus status);
}

