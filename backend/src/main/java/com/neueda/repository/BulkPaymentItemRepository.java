package com.neueda.repository;

import com.neueda.domain.BulkPaymentItemRecord;
import com.neueda.domain.BulkPaymentItemStatus;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for bulk payment item persistence.
 */
public interface BulkPaymentItemRepository {
    
    /**
     * Create a new bulk payment item record.
     */
    BulkPaymentItemRecord create(BulkPaymentItemRecord item);
    
    /**
     * Create multiple items in batch (for CSV bulk insert).
     */
    void createBatch(List<BulkPaymentItemRecord> items);
    
    /**
     * Retrieve an item by ID.
     */
    Optional<BulkPaymentItemRecord> findById(Long itemId);
    
    /**
     * Retrieve all items in a batch.
     */
    List<BulkPaymentItemRecord> findByBatchId(Long batchId);
    
    /**
     * Retrieve items in a batch with specific status.
     */
    List<BulkPaymentItemRecord> findByBatchIdAndStatus(Long batchId, BulkPaymentItemStatus status);
    
    /**
     * Update an existing item record.
     */
    void update(BulkPaymentItemRecord item);
    
    /**
     * Count items in a batch by status.
     */
    int countByBatchIdAndStatus(Long batchId, BulkPaymentItemStatus status);
    
    /**
     * Delete an item (if needed for rollback).
     */
    void delete(Long itemId);
}

