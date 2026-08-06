package com.neueda.controller;

import com.neueda.dto.*;
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
     * Errors are handled by GlobalExceptionHandler which provides user-friendly messages.
     * 
     * @param file uploaded CSV file
     * @return validation result with error list
     * @throws IOException if file cannot be read
     * @throws BulkPaymentCSVValidationException if CSV format is invalid
     */
    @PostMapping("/validate-csv")
    public ResponseEntity<CSVValidationResultDTO> validateCSV(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new BulkPaymentCSVValidationException("File is empty. Please select a valid CSV file.");
        }
        
        logger.info("Validating CSV file: {}", file.getOriginalFilename());
        
        try {
            CSVValidationResultDTO result = bulkPaymentService.validateCSVUpload(file.getInputStream());
            logger.info("CSV validation completed: {} valid, {} invalid", result.validRecords(), result.invalidRecords());
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            logger.error("Error reading CSV file: {}", e.getMessage(), e);
            throw new BulkPaymentCSVValidationException(
                "Unable to read CSV file. Please ensure the file is not corrupted and try again."
            );
        }
    }
    
    /**
     * Create a bulk payment batch from CSV upload or manual entry.
     * Initiates the batch processing workflow.
     * 
     * Errors are handled by GlobalExceptionHandler which provides user-friendly messages.
     * 
     * @param request bulk payment request with items
     * @return response with batch details and status
     * @throws BulkPaymentCSVValidationException if validation fails
     * @throws IllegalArgumentException if request is invalid
     */
    @PostMapping
    public ResponseEntity<BulkPaymentResponseDTO> createBulkPayment(
        @RequestBody CreateBulkPaymentRequest request
    ) {
        logger.info("Creating bulk payment batch with {} items from account: {}", 
            request.items().size(), request.sourceAccount());
        
        if (request.sourceAccount() == null || request.sourceAccount().isEmpty()) {
            throw new IllegalArgumentException("Source account is required.");
        }
        
        if (request.pin() == null || request.pin().isEmpty()) {
            throw new IllegalArgumentException("PIN is required for bulk payments.");
        }

        if (!request.pin().matches("^[0-9]{4}$")) {
            throw new IllegalArgumentException("PIN must be exactly 4 digits.");
        }
        
        if (request.items() == null || request.items().isEmpty()) {
            throw new IllegalArgumentException("At least one payment item is required.");
        }
        
        // TODO: Get authenticated user ID from security context
        String userId = "DEMO_USER"; // Placeholder - replace with actual auth
        
        BulkPaymentResponseDTO response = bulkPaymentService.createBulkPayment(request, userId);
        logger.info("Bulk payment batch created: {}", response.batchReference());
        return ResponseEntity.accepted().body(response);
    }
    
    /**
     * Retrieve batch details by ID.
     * 
     * Errors are handled by GlobalExceptionHandler which provides user-friendly messages.
     * 
     * @param batchId batch ID
     * @return batch details with transaction results
     * @throws BulkPaymentBatchNotFoundException if batch is not found
     */
    @GetMapping("/{batchId}")
    public ResponseEntity<BulkPaymentResponseDTO> getBatchDetails(
        @PathVariable Long batchId
    ) {
        if (batchId == null || batchId <= 0) {
            throw new IllegalArgumentException("Valid batch ID is required.");
        }
        
        logger.info("Retrieving bulk payment batch: {}", batchId);
        BulkPaymentResponseDTO response = bulkPaymentService.getBatchDetails(batchId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Retrieve batch details by batch reference.
     * 
     * Errors are handled by GlobalExceptionHandler which provides user-friendly messages.
     * 
     * @param batchReference user-facing batch reference
     * @return batch details with transaction results
     * @throws BulkPaymentBatchNotFoundException if batch is not found
     */
    @GetMapping("/by-reference/{batchReference}")
    public ResponseEntity<BulkPaymentResponseDTO> getBatchByReference(
        @PathVariable String batchReference
    ) {
        if (batchReference == null || batchReference.isEmpty()) {
            throw new IllegalArgumentException("Valid batch reference is required.");
        }
        
        logger.info("Retrieving bulk payment batch by reference: {}", batchReference);
        BulkPaymentResponseDTO response = bulkPaymentService.getBatchDetailsByReference(batchReference);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get real-time progress of batch processing.
     * Used for UI polling during batch execution.
     * 
     * Errors are handled by GlobalExceptionHandler which provides user-friendly messages.
     * 
     * @param batchId batch ID
     * @return progress information
     * @throws BulkPaymentBatchNotFoundException if batch is not found
     */
    @GetMapping("/{batchId}/progress")
    public ResponseEntity<BulkPaymentProgressDTO> getProgress(
        @PathVariable Long batchId
    ) {
        if (batchId == null || batchId <= 0) {
            throw new IllegalArgumentException("Valid batch ID is required.");
        }
        
        logger.debug("Getting progress for batch: {}", batchId);
        BulkPaymentProgressDTO progress = bulkPaymentService.getProgress(batchId);
        return ResponseEntity.ok(progress);
    }
    
    /**
     * Get batch history for authenticated user.
     * Supports pagination.
     * 
     * Errors are handled by GlobalExceptionHandler which provides user-friendly messages.
     * 
     * @param limit max results
     * @param offset pagination offset
     * @return list of batch summaries
     * @throws IllegalArgumentException if parameters are invalid
     */
    @GetMapping("/history")
    public ResponseEntity<List<BulkPaymentResponseDTO>> getHistory(
        @RequestParam(defaultValue = "20") int limit,
        @RequestParam(defaultValue = "0") int offset
    ) {
        if (limit <= 0 || limit > 1000) {
            throw new IllegalArgumentException("Limit must be between 1 and 1000.");
        }
        if (offset < 0) {
            throw new IllegalArgumentException("Offset cannot be negative.");
        }
        
        // TODO: Get authenticated user ID from security context
        String userId = "DEMO_USER"; // Placeholder
        
        logger.info("Retrieving batch history for user: {} (limit: {}, offset: {})", userId, limit, offset);
        List<BulkPaymentResponseDTO> history = bulkPaymentService.getBatchHistory(userId, limit, offset);
        return ResponseEntity.ok(history);
    }
}


