package com.neueda.controller;

import com.neueda.domain.FraudRuleRecord;
import com.neueda.dto.CreateFraudRuleRequest;
import com.neueda.dto.FraudRuleResponse;
import com.neueda.exception.PaymentProcessingException;
import com.neueda.service.FraudRuleService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for Fraud Rule Management
 * 
 * Endpoints:
 * - GET /api/admin/fraud-rules - List all fraud rules
 * - POST /api/admin/fraud-rules - Create new fraud rule
 * - GET /api/admin/fraud-rules/{id} - Get fraud rule details
 * - PUT /api/admin/fraud-rules/{id} - Update fraud rule
 * - PATCH /api/admin/fraud-rules/{id}/toggle - Toggle rule active/inactive
 * - DELETE /api/admin/fraud-rules/{id} - Delete fraud rule
 */
@RestController
@RequestMapping("/api/admin/fraud-rules")
public class FraudRuleAdminController {
    
    private static final Logger logger = LoggerFactory.getLogger(FraudRuleAdminController.class);
    private final FraudRuleService fraudRuleService;
    
    public FraudRuleAdminController(FraudRuleService fraudRuleService) {
        this.fraudRuleService = fraudRuleService;
    }
    
    /**
     * List all fraud rules
     * GET /api/admin/fraud-rules
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> listRules() {
        try {
            logger.debug("Fetching all fraud rules");
            List<FraudRuleRecord> rules = fraudRuleService.getAllRules();
            logger.debug("Found {} fraud rules", rules.size());
            
            List<FraudRuleResponse> responses = rules.stream()
                .map(this::toResponse)
                .toList();
            
            return ResponseEntity.ok(Map.of(
                "rules", responses,
                "total", rules.size(),
                "activeCount", fraudRuleService.getActiveRuleCount()
            ));
        } catch (Exception e) {
            logger.error("Error listing fraud rules", e);
            throw new PaymentProcessingException("Error listing fraud rules: " + e.getMessage());
        }
    }
    
    /**
     * Get fraud rule details
     * GET /api/admin/fraud-rules/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<FraudRuleResponse> getRule(@PathVariable Long id) {
        try {
            FraudRuleRecord rule = fraudRuleService.getRule(id)
                .orElseThrow(() -> new IllegalArgumentException("Fraud rule not found: " + id));
            
            return ResponseEntity.ok(toResponse(rule));
        } catch (IllegalArgumentException e) {
            throw new PaymentProcessingException(e.getMessage());
        }
    }
    
    /**
     * Create new fraud rule
     * POST /api/admin/fraud-rules
     */
    @PostMapping
    public ResponseEntity<FraudRuleResponse> createRule(@Valid @RequestBody CreateFraudRuleRequest request) {
        try {
            FraudRuleRecord rule = FraudRuleRecord.create(
                request.ruleName(),
                request.ruleType(),
                request.description(),
                request.severity() != null ? request.severity() : "MEDIUM",
                request.orderOfExecution() != null ? request.orderOfExecution() : 100,
                request.weight() != null ? request.weight() : BigDecimal.ONE,
                request.ruleDefinitionJson(),
                request.triggeringConditionsJson()
            );
            
            FraudRuleRecord created = fraudRuleService.createRule(rule);
            return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
        } catch (IllegalArgumentException e) {
            throw new PaymentProcessingException("Invalid rule: " + e.getMessage());
        }
    }
    
    /**
     * Update fraud rule
     * PUT /api/admin/fraud-rules/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<FraudRuleResponse> updateRule(
        @PathVariable Long id,
        @Valid @RequestBody CreateFraudRuleRequest request
    ) {
        try {
            FraudRuleRecord ruleUpdate = new FraudRuleRecord(
                id,
                request.ruleName(),
                request.ruleType(),
                request.description(),
                null,
                request.severity(),
                request.orderOfExecution(),
                request.weight(),
                request.ruleDefinitionJson(),
                request.triggeringConditionsJson(),
                0,
                null,
                null,
                null,
                null
            );
            
            FraudRuleRecord updated = fraudRuleService.updateRule(id, ruleUpdate);
            return ResponseEntity.ok(toResponse(updated));
        } catch (IllegalArgumentException e) {
            throw new PaymentProcessingException("Error updating rule: " + e.getMessage());
        }
    }
    
    /**
     * Toggle rule active/inactive
     * PATCH /api/admin/fraud-rules/{id}/toggle
     */
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<Map<String, Object>> toggleRule(@PathVariable Long id) {
        try {
            fraudRuleService.toggleRuleStatus(id);
            FraudRuleRecord updated = fraudRuleService.getRule(id)
                .orElseThrow(() -> new IllegalArgumentException("Rule not found after toggle"));
            
            return ResponseEntity.ok(Map.of(
                "id", id,
                "isActive", updated.isActive(),
                "message", "Rule toggled successfully"
            ));
        } catch (IllegalArgumentException e) {
            throw new PaymentProcessingException("Error toggling rule: " + e.getMessage());
        }
    }
    
    /**
     * Delete fraud rule
     * DELETE /api/admin/fraud-rules/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteRule(@PathVariable Long id) {
        try {
            fraudRuleService.deleteRule(id);
            return ResponseEntity.ok(Map.of(
                "id", id.toString(),
                "message", "Fraud rule deleted successfully"
            ));
        } catch (IllegalArgumentException e) {
            throw new PaymentProcessingException("Error deleting rule: " + e.getMessage());
        }
    }
    
    /**
     * Convert FraudRuleRecord to FraudRuleResponse
     */
    private FraudRuleResponse toResponse(FraudRuleRecord rule) {
        return new FraudRuleResponse(
            rule.id(),
            rule.ruleName(),
            rule.ruleType(),
            rule.description(),
            rule.isActive(),
            rule.severity(),
            rule.orderOfExecution(),
            rule.weight(),
            rule.ruleDefinitionJson(),
            rule.triggeringConditionsJson(),
            rule.mockScore(),
            rule.createdAt(),
            rule.updatedAt(),
            rule.createdBy(),
            rule.updatedBy()
        );
    }
}

