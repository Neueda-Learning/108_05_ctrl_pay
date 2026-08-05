package com.neueda.controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.neueda.domain.FraudAssessmentRecord;
import com.neueda.domain.FraudDecision;
import com.neueda.domain.PaymentRecord;
import com.neueda.domain.PaymentStatus;
import com.neueda.dto.AdminFraudDecisionRequest;
import com.neueda.repository.FraudAssessmentRepository;
import com.neueda.repository.PaymentRepository;
import com.neueda.repository.PaymentStatusHistoryRepository;
import com.neueda.domain.PaymentStatusHistoryRecord;

/**
 * Admin Fraud Assessment Controller
 * REST API for managing and reviewing fraudulent payment assessments
 */
@RestController
@RequestMapping("/api/admin/fraud-assessments")
public class FraudAssessmentAdminController {
    
    private final FraudAssessmentRepository fraudAssessmentRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentStatusHistoryRepository statusHistoryRepository;

    public FraudAssessmentAdminController(
        FraudAssessmentRepository fraudAssessmentRepository,
        PaymentRepository paymentRepository,
        PaymentStatusHistoryRepository statusHistoryRepository
    ) {
        this.fraudAssessmentRepository = fraudAssessmentRepository;
        this.paymentRepository = paymentRepository;
        this.statusHistoryRepository = statusHistoryRepository;
    }

    /**
     * List fraud assessments with filtering and pagination
     */
    @GetMapping
    public ResponseEntity<?> listAssessments(
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String riskLevel,
        @RequestParam(required = false) String dateFrom,
        @RequestParam(required = false) String dateTo,
        @RequestParam(defaultValue = "50") int limit,
        @RequestParam(defaultValue = "0") int offset
    ) {
        try {
            FraudDecision decision = null;
            if (status != null && !status.isEmpty()) {
                decision = FraudDecision.valueOf(status.toUpperCase());
            }
            
            LocalDateTime from = null, to = null;
            DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
            if (dateFrom != null) {
                from = LocalDateTime.parse(dateFrom, formatter);
            }
            if (dateTo != null) {
                to = LocalDateTime.parse(dateTo, formatter);
            }
            
            List<FraudAssessmentRecord> assessments = fraudAssessmentRepository.findAll(
                decision, riskLevel, from, to, limit, offset
            );
            
            long total = assessments.size();
            
            Map<String, Object> response = new HashMap<>();
            response.put("assessments", assessments);
            response.put("total", total);
            response.put("limit", limit);
            response.put("offset", offset);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get pending assessments requiring admin review
     */
    @GetMapping("/pending")
    public ResponseEntity<?> getPendingReviews(
        @RequestParam(defaultValue = "50") int limit,
        @RequestParam(defaultValue = "0") int offset
    ) {
        try {
            long totalPending = fraudAssessmentRepository.countPendingReview();
            List<FraudAssessmentRecord> pending = fraudAssessmentRepository.findPendingReview(limit, offset);
            
            Map<String, Object> response = new HashMap<>();
            response.put("assessments", pending);
            response.put("total", totalPending);
            response.put("limit", limit);
            response.put("offset", offset);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get fraud assessment details
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getAssessmentDetails(@PathVariable Long id) {
        try {
            FraudAssessmentRecord assessment = fraudAssessmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Assessment not found: " + id));
            
            PaymentRecord payment = paymentRepository.findById(assessment.paymentId())
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));
            
            Map<String, Object> response = new HashMap<>();
            response.put("assessment", assessment);
            response.put("payment", payment);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Admin approves a suspicious payment
     * Transitions payment from SUSPICIOUS to VALIDATED
     */
    @PostMapping("/{id}/approve")
    public ResponseEntity<?> approveAssessment(
        @PathVariable Long id,
        @RequestBody AdminFraudDecisionRequest request
    ) {
        try {
            FraudAssessmentRecord assessment = fraudAssessmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Assessment not found: " + id));
            
            PaymentRecord payment = paymentRepository.findById(assessment.paymentId())
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));
            
            // Update assessment with admin review
            String adminUser = request.getReviewedBy() != null ? request.getReviewedBy() : "SYSTEM";
            FraudAssessmentRecord updatedAssessment = assessment.withAdminReview(
                adminUser,
                FraudDecision.APPROVED,
                request.getNotes() != null ? request.getNotes() : ""
            );
            
            fraudAssessmentRepository.update(updatedAssessment);
            
            // Update payment status to VALIDATED (move to next step in workflow)
            if (payment.status() == PaymentStatus.SUSPICIOUS) {
                PaymentRecord updatedPayment = payment.withStatus(PaymentStatus.VALIDATED);
                paymentRepository.update(updatedPayment);
                
                // Log status transition
                statusHistoryRepository.save(
                    PaymentStatusHistoryRecord.transition(
                        payment.id(),
                        PaymentStatus.SUSPICIOUS,
                        PaymentStatus.VALIDATED,
                        adminUser
                    )
                );
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "approved");
            response.put("assessment", updatedAssessment);
            response.put("payment", payment.withStatus(PaymentStatus.VALIDATED));
            response.put("message", "Payment approved by admin. Status transitioned to VALIDATED.");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Admin rejects a suspicious payment
     * Transitions payment from SUSPICIOUS to FAILED
     */
    @PostMapping("/{id}/reject")
    public ResponseEntity<?> rejectAssessment(
        @PathVariable Long id,
        @RequestBody AdminFraudDecisionRequest request
    ) {
        try {
            FraudAssessmentRecord assessment = fraudAssessmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Assessment not found: " + id));
            
            PaymentRecord payment = paymentRepository.findById(assessment.paymentId())
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));
            
            // Update assessment with admin review
            String adminUser = request.getReviewedBy() != null ? request.getReviewedBy() : "SYSTEM";
            FraudAssessmentRecord updatedAssessment = assessment.withAdminReview(
                adminUser,
                FraudDecision.REJECTED,
                request.getNotes() != null ? request.getNotes() : ""
            );
            
            fraudAssessmentRepository.update(updatedAssessment);
            
            // Update payment status to FAILED
            if (payment.status() == PaymentStatus.SUSPICIOUS) {
                PaymentRecord failedPayment = payment.withFailure(
                    "FRAUD_DETECTED",
                    "Payment rejected by admin due to fraud assessment: " + 
                        (request.getNotes() != null ? request.getNotes() : "High fraud risk")
                );
                paymentRepository.update(failedPayment);
                
                // Log status transition
                statusHistoryRepository.save(
                    PaymentStatusHistoryRecord.failure(
                        payment.id(),
                        PaymentStatus.SUSPICIOUS,
                        "FRAUD_DETECTED",
                        request.getNotes() != null ? request.getNotes() : "Admin rejected - fraud confirmed",
                        adminUser
                    )
                );
                
                Map<String, Object> response = new HashMap<>();
                response.put("status", "rejected");
                response.put("assessment", updatedAssessment);
                response.put("payment", failedPayment);
                response.put("message", "Payment rejected by admin. Status transitioned to FAILED.");
                
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Payment is not in SUSPICIOUS status"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Get fraud statistics
     */
    @GetMapping("/stats/overview")
    public ResponseEntity<?> getOverview() {
        try {
            long totalApproved = fraudAssessmentRepository.countByDecision(FraudDecision.APPROVED);
            long totalSuspicious = fraudAssessmentRepository.countByDecision(FraudDecision.SUSPICIOUS);
            long totalRejected = fraudAssessmentRepository.countByDecision(FraudDecision.REJECTED);
            long pendingReview = fraudAssessmentRepository.countPendingReview();
            
            Map<String, Object> response = new HashMap<>();
            response.put("totalApproved", totalApproved);
            response.put("totalSuspicious", totalSuspicious);
            response.put("totalRejected", totalRejected);
            response.put("pendingReview", pendingReview);
            response.put("totalAssessed", totalApproved + totalSuspicious + totalRejected);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }
}



