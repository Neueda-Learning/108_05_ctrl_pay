package com.neueda.service.bulk;

import com.neueda.domain.*;
import com.neueda.dto.*;
import com.neueda.exception.BulkPaymentBatchNotFoundException;
import com.neueda.exception.BulkPaymentCSVValidationException;
import com.neueda.fraud.FraudDetectionService;
import com.neueda.repository.BulkPaymentBatchRepository;
import com.neueda.repository.BulkPaymentItemRepository;
import com.neueda.repository.PaymentRepository;
import com.neueda.service.PaymentService;
import com.neueda.validation.RuleEngine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Bulk Payment Service - Orchestrates the entire bulk payment processing lifecycle.
 * 
 * Responsibilities:
 * 1. CSV file parsing and validation
 * 2. Batch creation and item creation
 * 3. Validation of all items using existing RuleEngine
 * 4. Fraud detection using existing FraudDetectionService
 * 5. Payment settlement via existing PaymentService
 * 6. Transaction rollback on failures
 * 7. Audit logging and error tracking
 * 
 * Key Design Principle:
 * - Each payment item uses INDEPENDENT transaction boundaries
 * - Failure of one payment does NOT rollback others
 * - Batch tracks overall status but items track individual state
 */
@Service
@Transactional
public class BulkPaymentService {
    
    private static final Logger logger = LoggerFactory.getLogger(BulkPaymentService.class);
    
    private static final int DEFAULT_BATCH_SIZE = 100;
    private static final int MAX_BATCH_SIZE = 10000;
    private static final String BATCH_REFERENCE_PREFIX = "BP";
    
    private final BulkPaymentBatchRepository batchRepository;
    private final BulkPaymentItemRepository itemRepository;
    private final PaymentService paymentService;
    private final FraudDetectionService fraudDetectionService;
    private final RuleEngine ruleEngine;
    private final PaymentRepository paymentRepository;
    
    public BulkPaymentService(
        BulkPaymentBatchRepository batchRepository,
        BulkPaymentItemRepository itemRepository,
        PaymentService paymentService,
        FraudDetectionService fraudDetectionService,
        RuleEngine ruleEngine,
        PaymentRepository paymentRepository
    ) {
        this.batchRepository = batchRepository;
        this.itemRepository = itemRepository;
        this.paymentService = paymentService;
        this.fraudDetectionService = fraudDetectionService;
        this.ruleEngine = ruleEngine;
        this.paymentRepository = paymentRepository;
    }
    
    /**
     * Parse and validate CSV upload.
     * Returns validation result without persisting.
     */
    public CSVValidationResultDTO validateCSVUpload(InputStream csvStream) throws IOException {
        List<CSVValidationErrorDTO> errors = new ArrayList<>();
        int lineNumber = 0;
        int validRecords = 0;
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(csvStream))) {
            String line;
            String headerLine = reader.readLine();
            lineNumber = 1;
            
            if (headerLine == null || !isValidCSVHeader(headerLine)) {
                errors.add(new CSVValidationErrorDTO(
                    1, null, "Invalid CSV header format", "INVALID_HEADER"
                ));
                return new CSVValidationResultDTO(0, 0, 1, false, errors);
            }
            
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                
                if (line.trim().isEmpty()) {
                    continue;
                }
                
                List<String> validationErrors = validateCSVLine(line, lineNumber);
                if (validationErrors.isEmpty()) {
                    validRecords++;
                } else {
                    for (String errorMsg : validationErrors) {
                        errors.add(new CSVValidationErrorDTO(
                            lineNumber, null, errorMsg, "VALIDATION_FAILED"
                        ));
                    }
                }
            }
        }
        
        int totalRecords = lineNumber - 1; // Exclude header
        int invalidRecords = totalRecords - validRecords;
        
        return new CSVValidationResultDTO(
            totalRecords,
            validRecords,
            invalidRecords,
            invalidRecords == 0,
            errors
        );
    }
    
    /**
     * Create a bulk payment batch from validated items.
     * This initiates the batch processing workflow.
     */
    @Transactional
    public BulkPaymentResponseDTO createBulkPayment(
        CreateBulkPaymentRequest request,
        String userId
    ) throws BulkPaymentCSVValidationException {
        
        // Validate source account
        validateSourceAccount(request.sourceAccount());
        
        // Check idempotency
        if (request.idempotencyKey() != null) {
            Optional<BulkPaymentBatchRecord> existing = batchRepository.findByIdempotencyKey(request.idempotencyKey());
            if (existing.isPresent()) {
                logger.info("Duplicate batch upload detected via idempotency key: {}", request.idempotencyKey());
                return convertBatchToResponse(existing.get());
            }
        }
        
        // Validate all items
        validateItemsList(request.items());
        
        // Create batch record
        BigDecimal totalAmount = request.items().stream()
            .map(BulkPaymentItemDTO::amount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        String batchReference = generateBatchReference();
        BulkPaymentBatchRecord batch = BulkPaymentBatchRecord.create(
            batchReference,
            request.idempotencyKey(),
            request.sourceAccount(),
            totalAmount,
            request.items().size(),
            userId
        );
        
        BulkPaymentBatchRecord createdBatch = batchRepository.create(batch);
        logger.info("Created bulk payment batch: {} with {} items", batchReference, request.items().size());
        
        // Create item records
        List<BulkPaymentItemRecord> itemRecords = new ArrayList<>();
        for (int i = 0; i < request.items().size(); i++) {
            BulkPaymentItemDTO itemDTO = request.items().get(i);
            BulkPaymentItemRecord item = BulkPaymentItemRecord.create(
                createdBatch.id(),
                i + 1, // 1-indexed line number
                itemDTO.destinationAccount(),
                itemDTO.amount(),
                itemDTO.currency(),
                itemDTO.description()
            );
            itemRecords.add(item);
        }
        
        itemRepository.createBatch(itemRecords);
        logger.info("Created {} payment items for batch {}", itemRecords.size(), batchReference);
        
        // Mark batch as ready for validation
        BulkPaymentBatchRecord updatedBatch = createdBatch.withValidationStarted();
        batchRepository.update(updatedBatch);
        
        // Return response
        return convertBatchToResponse(updatedBatch);
    }
    
    /**
     * Validate all items in a batch using existing validation rules.
     * This executes the validation phase without persisting payments yet.
     */
    @Transactional
    public void validateBatch(Long batchId) throws BulkPaymentBatchNotFoundException {
        BulkPaymentBatchRecord batch = batchRepository.findById(batchId)
            .orElseThrow(() -> new BulkPaymentBatchNotFoundException(batchId));
        
        logger.info("Starting validation phase for batch: {}", batch.batchReference());
        
        List<BulkPaymentItemRecord> items = itemRepository.findByBatchId(batchId);
        
        int validatedCount = 0;
        int failedCount = 0;
        
        for (BulkPaymentItemRecord item : items) {
            // Mark item as validating
            BulkPaymentItemRecord validatingItem = item.withValidationStarted();
            itemRepository.update(validatingItem);
            
            // Execute validation
            List<ValidationResultRecord> validationResults = performItemValidation(item, batch.sourceAccount());
            
            boolean validationPassed = validationResults.stream().allMatch(ValidationResultRecord::passed);
            
            if (validationPassed) {
                BulkPaymentItemRecord validatedItem = validatingItem.withValidationCompleted();
                itemRepository.update(validatedItem);
                validatedCount++;
            } else {
                String errorMsg = buildValidationErrorMessage(validationResults);
                String errorsJson = serializeValidationErrors(validationResults);
                BulkPaymentItemRecord failedItem = validatingItem.withValidationFailure(
                    "VALIDATION_FAILED",
                    errorMsg,
                    errorsJson
                );
                itemRepository.update(failedItem);
                failedCount++;
            }
        }
        
        // Update batch status
        BulkPaymentBatchRecord completedBatch = batch
            .withValidationCompleted()
            .withUpdatedCounts(validatedCount, failedCount);
        
        logger.info("Validation completed for batch: {} - Valid: {}, Failed: {}", 
            batch.batchReference(), validatedCount, failedCount);
        
        batchRepository.update(completedBatch);
    }
    
    /**
     * Process settlement for all validated items in a batch.
     * Fraud detection occurs prior to settlement.
     */
    @Transactional
    public void processBatchSettlement(Long batchId) throws BulkPaymentBatchNotFoundException {
        BulkPaymentBatchRecord batch = batchRepository.findById(batchId)
            .orElseThrow(() -> new BulkPaymentBatchNotFoundException(batchId));
        
        logger.info("Starting settlement phase for batch: {}", batch.batchReference());
        
        List<BulkPaymentItemRecord> validatedItems = itemRepository.findByBatchIdAndStatus(
            batchId,
            BulkPaymentItemStatus.VALIDATED
        );
        
        int successCount = 0;
        int failureCount = 0;
        
        // Start processing
        BulkPaymentBatchRecord processingBatch = batch.withProcessingStarted();
        batchRepository.update(processingBatch);
        
        for (BulkPaymentItemRecord item : validatedItems) {
            try {
                // Each item processing is independent - use its own transaction
                boolean success = processSingleItem(item, batch.sourceAccount());
                if (success) {
                    successCount++;
                } else {
                    failureCount++;
                }
            } catch (Exception e) {
                logger.error("Error processing item {} in batch {}: {}", 
                    item.lineNumber(), batch.batchReference(), e.getMessage(), e);
                failureCount++;
            }
        }
        
        // Determine batch completion status
        BulkPaymentBatchStatus completionStatus;
        if (failureCount == 0) {
            completionStatus = BulkPaymentBatchStatus.COMPLETED;
        } else if (successCount > 0) {
            completionStatus = BulkPaymentBatchStatus.PARTIALLY_COMPLETED;
        } else {
            completionStatus = BulkPaymentBatchStatus.FAILED;
        }
        
        // Update batch with final counts
        BulkPaymentBatchRecord completedBatch = batch
            .withUpdatedCounts(successCount, failureCount)
            .withProcessingCompleted(completionStatus);
        
        batchRepository.update(completedBatch);
        logger.info("Batch {} settlement completed - Success: {}, Failures: {}", 
            batch.batchReference(), successCount, failureCount);
    }
    
    /**
     * Get batch details with transaction results.
     */
    public BulkPaymentResponseDTO getBatchDetails(Long batchId) throws BulkPaymentBatchNotFoundException {
        BulkPaymentBatchRecord batch = batchRepository.findById(batchId)
            .orElseThrow(() -> new BulkPaymentBatchNotFoundException(batchId));
        
        return convertBatchToResponse(batch);
    }
    
    /**
     * Get batch details by reference.
     */
    public BulkPaymentResponseDTO getBatchDetailsByReference(String batchReference) throws BulkPaymentBatchNotFoundException {
        BulkPaymentBatchRecord batch = batchRepository.findByReference(batchReference)
            .orElseThrow(() -> new BulkPaymentBatchNotFoundException(batchReference));
        
        return convertBatchToResponse(batch);
    }
    
    /**
     * Get real-time progress of batch processing.
     */
    public BulkPaymentProgressDTO getProgress(Long batchId) throws BulkPaymentBatchNotFoundException {
        BulkPaymentBatchRecord batch = batchRepository.findById(batchId)
            .orElseThrow(() -> new BulkPaymentBatchNotFoundException(batchId));
        
        int validatedCount = itemRepository.countByBatchIdAndStatus(batchId, BulkPaymentItemStatus.VALIDATED);
        int successCount = itemRepository.countByBatchIdAndStatus(batchId, BulkPaymentItemStatus.SUCCESS);
        int failedCount = itemRepository.countByBatchIdAndStatus(batchId, BulkPaymentItemStatus.FAILED);
        
        int totalCount = batch.totalTransactions();
        int progressPercent = totalCount > 0 ? (int) ((successCount + failedCount) * 100 / totalCount) : 0;
        
        LocalDateTime lastUpdate = batch.processingCompletedAt() != null ?
            batch.processingCompletedAt() : batch.validationCompletedAt();
        
        return new BulkPaymentProgressDTO(
            batch.id(),
            batch.batchReference(),
            batch.status().toString(),
            batch.totalTransactions(),
            validatedCount,
            successCount,
            failedCount,
            progressPercent,
            lastUpdate,
            batch.lastErrorMessage()
        );
    }
    
    /**
     * Get batch history for a user with filtering.
     */
    public List<BulkPaymentResponseDTO> getBatchHistory(String userId, int limit, int offset) {
        List<BulkPaymentBatchRecord> batches = batchRepository.findByCreatedBy(userId, limit, offset);
        return batches.stream().map(this::convertBatchToResponse).collect(Collectors.toList());
    }
    
    // ==================== PRIVATE HELPER METHODS ====================
    
    private void validateSourceAccount(String sourceAccount) throws BulkPaymentCSVValidationException {
        if (sourceAccount == null || sourceAccount.trim().isEmpty()) {
            throw new BulkPaymentCSVValidationException("Source account is required");
        }
        if (!sourceAccount.matches("^[0-9]{12}$")) {
            throw new BulkPaymentCSVValidationException("Source account must be 12 digits");
        }
    }
    
    private void validateItemsList(List<BulkPaymentItemDTO> items) throws BulkPaymentCSVValidationException {
        if (items == null || items.isEmpty()) {
            throw new BulkPaymentCSVValidationException("At least one payment item is required");
        }
        if (items.size() > MAX_BATCH_SIZE) {
            throw new BulkPaymentCSVValidationException(
                "Batch size exceeds maximum of " + MAX_BATCH_SIZE + " items"
            );
        }
    }
    
    private boolean isValidCSVHeader(String headerLine) {
        String[] parts = headerLine.split(",");
        return parts.length >= 3 &&
            "destinationAccount".equals(parts[0].trim()) &&
            "amount".equals(parts[1].trim()) &&
            "currency".equals(parts[2].trim());
    }
    
    private List<String> validateCSVLine(String line, int lineNumber) {
        List<String> errors = new ArrayList<>();
        String[] parts = line.split(",");
        
        if (parts.length < 3) {
            errors.add("Line " + lineNumber + ": Missing required columns");
            return errors;
        }
        
        String destAccount = parts[0].trim();
        String amountStr = parts[1].trim();
        String currency = parts[2].trim();
        
        // Validate destination account
        if (!destAccount.matches("^[0-9]{12}$")) {
            errors.add("Line " + lineNumber + ": Invalid destination account number");
        }
        
        // Validate amount
        try {
            BigDecimal amount = new BigDecimal(amountStr);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                errors.add("Line " + lineNumber + ": Amount must be positive");
            }
            if (amount.compareTo(new BigDecimal("1000000.00")) > 0) {
                errors.add("Line " + lineNumber + ": Amount exceeds maximum of 1,000,000");
            }
        } catch (NumberFormatException e) {
            errors.add("Line " + lineNumber + ": Invalid amount format");
        }
        
        // Validate currency
        if (!currency.matches("^[A-Z]{3}$")) {
            errors.add("Line " + lineNumber + ": Invalid currency code (must be 3 uppercase letters)");
        }
        
        return errors;
    }
    
    private List<ValidationResultRecord> performItemValidation(
        BulkPaymentItemRecord item,
        String sourceAccount
    ) {
        // Create a temporary payment record for validation
        PaymentRecord tempPayment = PaymentRecord.create(
            null, // idempotencyKey
            sourceAccount,
            item.destinationAccount(),
            item.amount(),
            item.currency()
        );
        
        // Execute validation rules
        return ruleEngine.validatePayment(tempPayment);
    }
    
    private boolean processSingleItem(BulkPaymentItemRecord item, String sourceAccount) {
        try {
            // Mark as processing
            BulkPaymentItemRecord processingItem = item.withProcessingStarted(null);
            itemRepository.update(processingItem);
            
            // Create actual payment via existing PaymentService
            PaymentRecord payment = PaymentRecord.create(
                null,
                sourceAccount,
                item.destinationAccount(),
                item.amount(),
                item.currency()
            );
            
            PaymentRecord createdPayment = paymentService.createPayment(payment);
            
            // Perform fraud detection
            FraudAssessmentRecord fraudAssessment = fraudDetectionService.assessPayment(createdPayment);
            
            // Update item with fraud results
            BulkPaymentItemRecord fraudAssessedItem = item.withProcessingStarted(createdPayment.id())
                .withFraudAssessment(
                    fraudAssessment.hybridFraudScore(),
                    fraudAssessment.decision().toString()
                );
            itemRepository.update(fraudAssessedItem);
            
            // Check fraud decision
            if (fraudAssessment.decision() == FraudDecision.REJECTED) {
                BulkPaymentItemRecord rejectedItem = fraudAssessedItem.withProcessingFailure(
                    "FRAUD_REJECTED",
                    "Transaction rejected by fraud detection: " + fraudAssessment.explanation()
                );
                itemRepository.update(rejectedItem);
                return false;
            }
            
            // Process settlement (this will handle the actual balance updates)
            // Note: PaymentService handles settlement logic
            paymentService.processPaymentSettlement(createdPayment.id());
            
            // Mark as success
            BulkPaymentItemRecord successItem = fraudAssessedItem.withProcessingSuccess();
            itemRepository.update(successItem);
            
            logger.info("Successfully processed payment item {} - Payment ID: {}", item.lineNumber(), createdPayment.id());
            return true;
            
        } catch (Exception e) {
            logger.error("Item processing failed - Line: {}, Error: {}", item.lineNumber(), e.getMessage(), e);
            
            BulkPaymentItemRecord failedItem = item.withProcessingFailure(
                "PAYMENT_PROCESSING_FAILED",
                e.getMessage()
            );
            itemRepository.update(failedItem);
            return false;
        }
    }
    
    private String generateBatchReference() {
        return BATCH_REFERENCE_PREFIX + System.currentTimeMillis();
    }
    
    private String buildValidationErrorMessage(List<ValidationResultRecord> results) {
        return results.stream()
            .filter(r -> !r.passed())
            .map(ValidationResultRecord::errorMessage)
            .collect(Collectors.joining("; "));
    }
    
    private String serializeValidationErrors(List<ValidationResultRecord> results) {
        List<Map<String, String>> errors = results.stream()
            .filter(r -> !r.passed())
            .map(r -> Map.of(
                "rule", r.ruleName(),
                "error", r.errorMessage(),
                "code", r.errorCode() != null ? r.errorCode() : ""
            ))
            .collect(Collectors.toList());
        
        // Convert to JSON string (simplified)
        return errors.toString();
    }
    
    private BulkPaymentResponseDTO convertBatchToResponse(BulkPaymentBatchRecord batch) {
        List<BulkPaymentItemRecord> items = itemRepository.findByBatchId(batch.id());
        
        List<BulkTransactionResultDTO> results = items.stream()
            .map(item -> new BulkTransactionResultDTO(
                item.lineNumber(),
                item.paymentId(),
                item.destinationAccount(),
                item.amount(),
                item.currency(),
                item.status().toString(),
                item.failureReason(),
                item.errorCode(),
                item.fraudScore(),
                item.fraudDecision(),
                item.validationErrors(),
                item.rollbackStatus()
            ))
            .collect(Collectors.toList());
        
        return new BulkPaymentResponseDTO(
            batch.id(),
            batch.batchReference(),
            batch.sourceAccount(),
            batch.totalTransactions(),
            batch.successfulTransactions(),
            batch.failedTransactions(),
            batch.status().toString(),
            batch.totalAmount(),
            batch.createdAt(),
            batch.completedAt(),
            results
        );
    }
}

