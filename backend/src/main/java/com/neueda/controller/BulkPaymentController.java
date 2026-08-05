package com.neueda.controller;

import com.neueda.dto.*;
import com.neueda.exception.BulkPaymentBatchNotFoundException;
import com.neueda.exception.BulkPaymentCSVValidationException;
import com.neueda.service.bulk.BulkPaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * REST Controller for Bulk Payment operations.
 * 
 * Endpoints:
 * POST /api/bulk-payments/validate-csv - Validate CSV format before submission
 * POST  /api/bulk-payments - Create and start processing bulk payment batch
 * GET /api/bulk-payments/{batchId} - Retrieve batch details with results
 * GET /api/bulk-payments/{batchReference}/by-ref - Retrieve batch by reference
 * GET /api/bulk-payments/{batchId}/progress - Get real-time progress of batch processing
 * GET /api/bulk-payments/history - Get batch history for authenticated user
 */
@RestController
@RequestMapping("/api/bulk-payments")
public class BulkPaymentController {
    
    private static final Logger logger = LoggerFactory.getLogger(BulkPaymentController.class);
    
    private final BulkPaymentService bulkPaymentService;
    
    public BulkPaymentController(BulkPaymentService bulkPaymentService) {
        this.bulkPaymentService = bulkPaymentService;
    }
    
    /**
     * Validate CSV file format before submission.
     * This performs client-side validation on the CSV content without persisting.
     * 
     * @param file uploaded CSV file
     * @return validation result with error list
     */
    @PostMapping("/validate-csv")
    public ResponseEntity<CSVValidationResultDTO> validateCSV(@RequestParam("file") MultipartFile file) {
        logger.info("Validating CSV file: {}", file.getOriginalFilename());
        
        try {
            CSVValidationResultDTO result = bulkPaymentService.validateCSVUpload(file.getInputStream());
            logger.info("CSV validation completed: {} valid, {} invalid", result.validRecords(), result.invalidRecords());
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            logger.error("Error reading CSV file: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                .body(new CSVValidationResultDTO(
                    0, 0, 1, false,
                    List.of(new CSVValidationErrorDTO(0, null, "Failed to read file: " + e.getMessage(), "FILE_READ_ERROR"))
                ));
        }
    }
    
    /**
     * Create a bulk payment batch from CSV upload or manual entry.
     * Initiates the batch processing workflow.
     * 
     * @param request bulk payment request with items
     * @return response with batch details and status
     */
    @PostMapping
    public ResponseEntity<BulkPaymentResponseDTO> createBulkPayment(
        @RequestBody CreateBulkPaymentRequest request
    ) {
        logger.info("Creating bulk payment batch with {} items from account: {}", 
            request.items().size(), request.sourceAccount());
        
        try {
            // TODO: Get authenticated user ID from security context
            String userId = "DEMO_USER"; // Placeholder - replace with actual auth
            
            BulkPaymentResponseDTO response = bulkPaymentService.createBulkPayment(request, userId);
            logger.info("Bulk payment batch created: {}", response.batchReference());
            return ResponseEntity.accepted().body(response);
        } catch (BulkPaymentCSVValidationException e) {
            logger.error("Validation error creating bulk payment: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Retrieve batch details by ID.
     * 
     * @param batchId batch ID
     * @return batch details with transaction results
     */
    @GetMapping("/{batchId}")
    public ResponseEntity<BulkPaymentResponseDTO> getBatchDetails(
        @PathVariable Long batch Id
    ) {
        logger.info("Retrieving bulk payment batch: {}", batchId);
        
        try {
            BulkPaymentResponseDTO response = bulkPaymentService.getBatchDetails(batchId);
            return ResponseEntity.ok(response);
        } catch (BulkPaymentBatchNotFoundException e) {
            logger.warn("Batch not found: {}", batchId);
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Retrieve batch details by batch reference.
     * 
     * @param batchReference user-facing batch reference
     * @return batch details with transaction results
     */
    @GetMapping("/by-reference/{batchReference}")
    public ResponseEntity<BulkPaymentResponseDTO> getBatchByReference(
        @PathVariable String batchReference
    ) {
        logger.info("Retrieving bulk payment batch by reference: {}", batchReference);
        
        try {
            BulkPaymentResponseDTO response = bulkPaymentService.getBatchDetailsByReference(batchReference);
            return ResponseEntity.ok(response);
        } catch (BulkPaymentBatchNotFoundException e) {
            logger.warn("Batch reference not found: {}", batchReference);
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Get real-time progress of batch processing.
     * Used for UI polling during batch execution.
     * 
     * @param batchId batch ID
     * @return progress information
     */
    @GetMapping("/{batchId}/progress")
    public ResponseEntity<BulkPaymentProgressDTO> getProgress(
        @PathVariable Long batchId
    ) {
        logger.debug("Getting progress for batch: {}", batchId);
        
        try {
            BulkPaymentProgressDTO progress = bulkPaymentService.getProgress(batchId);
            return ResponseEntity.ok(progress);
        } catch (BulkPaymentBatchNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Get batch history for authenticated user.
     * Supports pagination.
     * 
     * @param limit max results
     * @param offset pagination offset
     * @return list of batch summaries
     */
    @GetMapping("/history")
    public ResponseEntity<List<BulkPaymentResponseDTO>> getHistory(
        @RequestParam(defaultValue = "20") int limit,
        @RequestParam(defaultValue = "0") int offset
    ) {
        // TODO: Get authenticated user ID from security context
        String userId = "DEMO_USER"; // Placeholder
        
        logger.info("Retrieving batch history for user: {} (limit: {}, offset: {})", userId, limit, offset);
        List<BulkPaymentResponseDTO> history = bulkPaymentService.getBatchHistory(userId, limit, offset);
        return ResponseEntity.ok(history);
    }
}

