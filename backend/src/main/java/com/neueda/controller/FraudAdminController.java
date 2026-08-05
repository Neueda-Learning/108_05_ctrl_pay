package com.neueda.controller;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.neueda.domain.AccountRecord;
import com.neueda.domain.FraudAssessmentRecord;
import com.neueda.domain.FraudDecision;
import com.neueda.domain.PaymentRecord;
import com.neueda.domain.PaymentStatus;
import com.neueda.domain.PaymentStatusHistoryRecord;
import com.neueda.dto.AdminFraudDecisionRequest;
import com.neueda.repository.AccountRepository;
import com.neueda.repository.FraudAssessmentRepository;
import com.neueda.repository.PaymentRepository;
import com.neueda.repository.PaymentStatusHistoryRepository;
import com.neueda.service.FraudRiskService;

/**
 * Admin Fraud Dashboard Controller
 * Provides complete fraud management APIs:
 *   GET  /api/admin/fraud/transactions        — list suspicious transactions
 *   GET  /api/admin/fraud/payment/{id}        — full fraud investigation details
 *   POST /api/admin/fraud/payment/{id}/approve — approve suspicious payment
 *   POST /api/admin/fraud/payment/{id}/reject  — reject suspicious payment
 *   GET  /api/admin/fraud/stats               — overview statistics
 *   POST /api/admin/fraud/account/{number}/refresh-risk — refresh account risk status
 */
@RestController
@RequestMapping("/api/admin/fraud")
public class FraudAdminController {

    private static final Logger logger = LoggerFactory.getLogger(FraudAdminController.class);

    private final FraudAssessmentRepository assessmentRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentStatusHistoryRepository statusHistoryRepository;
    private final AccountRepository accountRepository;
    private final FraudRiskService fraudRiskService;

    public FraudAdminController(
        FraudAssessmentRepository assessmentRepository,
        PaymentRepository paymentRepository,
        PaymentStatusHistoryRepository statusHistoryRepository,
        AccountRepository accountRepository,
        FraudRiskService fraudRiskService
    ) {
        this.assessmentRepository = assessmentRepository;
        this.paymentRepository = paymentRepository;
        this.statusHistoryRepository = statusHistoryRepository;
        this.accountRepository = accountRepository;
        this.fraudRiskService = fraudRiskService;
    }

    // =========================================================================
    // GET /api/admin/fraud/transactions
    // Returns payments in SUSPICIOUS status with their fraud assessments
    // =========================================================================
    @GetMapping("/transactions")
    public ResponseEntity<?> getSuspiciousTransactions(
        @RequestParam(defaultValue = "50") int limit,
        @RequestParam(defaultValue = "0") int offset,
        @RequestParam(required = false) String riskLevel
    ) {
        try {
            // Get all assessments with SUSPICIOUS decision and no reviewer yet (pending review)
            List<FraudAssessmentRecord> assessments = assessmentRepository.findAll(
                FraudDecision.SUSPICIOUS, riskLevel, null, null, limit, offset);

            List<Map<String, Object>> results = new ArrayList<>();
            for (FraudAssessmentRecord assessment : assessments) {
                Optional<PaymentRecord> paymentOpt = paymentRepository.findById(assessment.paymentId());
                if (paymentOpt.isEmpty()) continue;
                PaymentRecord payment = paymentOpt.get();

                Map<String, Object> item = new HashMap<>();
                item.put("paymentId", payment.id());
                item.put("sourceAccount", payment.sourceAccount());
                item.put("destinationAccount", payment.destinationAccount());
                item.put("amount", payment.amount());
                item.put("currency", payment.currency());
                item.put("paymentStatus", payment.status());
                item.put("createdAt", payment.createdAt());
                item.put("fraudScore", assessment.hybridFraudScore());
                item.put("ruleEngineScore", assessment.ruleEngineScore());
                item.put("mlFraudProbability", assessment.mlFraudProbability());
                item.put("riskLevel", assessment.riskLevel());
                item.put("triggeredRules", assessment.triggeredRulesJson());
                item.put("decision", assessment.decision());
                item.put("reviewedBy", assessment.reviewedBy());
                item.put("reviewedAt", assessment.reviewedAt());
                item.put("explanation", assessment.explanation());
                item.put("assessmentId", assessment.id());
                results.add(item);
            }

            long totalPending = assessmentRepository.countPendingReview();

            Map<String, Object> response = new HashMap<>();
            response.put("transactions", results);
            response.put("total", totalPending);
            response.put("limit", limit);
            response.put("offset", offset);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error fetching suspicious transactions: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    // =========================================================================
    // GET /api/admin/fraud/payment/{id}
    // Full fraud investigation details for a payment
    // =========================================================================
    @GetMapping("/payment/{id}")
    public ResponseEntity<?> getFraudInvestigationDetails(@PathVariable Long id) {
        try {
            PaymentRecord payment = paymentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + id));

            Optional<FraudAssessmentRecord> assessmentOpt = assessmentRepository.findByPaymentId(id);

            Optional<AccountRecord> sourceAccountOpt = accountRepository.findByAccountNumber(payment.sourceAccount());
            Optional<AccountRecord> destAccountOpt = accountRepository.findByAccountNumber(payment.destinationAccount());

            List<PaymentStatusHistoryRecord> history = statusHistoryRepository.findByPaymentId(id);

            Map<String, Object> response = new HashMap<>();
            response.put("payment", payment);
            response.put("fraudAssessment", assessmentOpt.orElse(null));
            response.put("sourceAccount", sourceAccountOpt.orElse(null));
            response.put("destinationAccount", destAccountOpt.orElse(null));
            response.put("statusHistory", history);

            // Build rule explanations if assessment exists
            if (assessmentOpt.isPresent()) {
                FraudAssessmentRecord assessment = assessmentOpt.get();
                Map<String, Object> ruleBreakdown = new HashMap<>();
                ruleBreakdown.put("ruleEngineScore", assessment.ruleEngineScore());
                ruleBreakdown.put("mlScore", assessment.mlFraudProbability());
                ruleBreakdown.put("hybridScore", assessment.hybridFraudScore());
                ruleBreakdown.put("triggeredRules", assessment.triggeredRulesJson());
                ruleBreakdown.put("ruleScores", assessment.ruleScoresJson());
                ruleBreakdown.put("explanation", assessment.explanation());
                response.put("ruleAnalysis", ruleBreakdown);
            }

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error fetching fraud details for payment {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    // =========================================================================
    // POST /api/admin/fraud/payment/{id}/approve
    // Admin approves a SUSPICIOUS payment → transitions to VALIDATED
    // =========================================================================
    @PostMapping("/payment/{id}/approve")
    public ResponseEntity<?> approveSuspiciousPayment(
        @PathVariable Long id,
        @RequestBody AdminFraudDecisionRequest request
    ) {
        try {
            PaymentRecord payment = paymentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + id));

            if (payment.status() != PaymentStatus.SUSPICIOUS) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Payment is not in SUSPICIOUS status (current: " + payment.status() + ")"));
            }

            String adminUser = request.getReviewedBy() != null ? request.getReviewedBy() : "ADMIN";

            // Update fraud assessment with approval
            assessmentRepository.findByPaymentId(id).ifPresent(assessment -> {
                FraudAssessmentRecord updated = assessment.withAdminReview(adminUser, FraudDecision.APPROVED,
                    request.getNotes() != null ? request.getNotes() : "Admin approved after review");
                assessmentRepository.update(updated);
            });

            // Transition payment: SUSPICIOUS → VALIDATED
            PaymentRecord validatedPayment = payment.withStatus(PaymentStatus.VALIDATED);
            paymentRepository.update(validatedPayment);

            statusHistoryRepository.save(PaymentStatusHistoryRecord.transition(
                id, PaymentStatus.SUSPICIOUS, PaymentStatus.VALIDATED, adminUser));

            logger.info("Admin {} approved payment {} — transitioning to VALIDATED", adminUser, id);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "approved");
            response.put("paymentId", id);
            response.put("newStatus", PaymentStatus.VALIDATED);
            response.put("reviewedBy", adminUser);
            response.put("message", "Payment approved. Transitioning to VALIDATED for scheduler processing.");

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error approving payment {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    // =========================================================================
    // POST /api/admin/fraud/payment/{id}/reject
    // Admin rejects a SUSPICIOUS payment → transitions to FAILED
    // =========================================================================
    @PostMapping("/payment/{id}/reject")
    public ResponseEntity<?> rejectSuspiciousPayment(
        @PathVariable Long id,
        @RequestBody AdminFraudDecisionRequest request
    ) {
        try {
            PaymentRecord payment = paymentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + id));

            if (payment.status() != PaymentStatus.SUSPICIOUS) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Payment is not in SUSPICIOUS status (current: " + payment.status() + ")"));
            }

            String adminUser = request.getReviewedBy() != null ? request.getReviewedBy() : "ADMIN";
            String rejectionReason = request.getNotes() != null ? request.getNotes() : "Rejected by admin — high fraud risk confirmed";

            // Update fraud assessment with rejection
            assessmentRepository.findByPaymentId(id).ifPresent(assessment -> {
                FraudAssessmentRecord updated = assessment.withAdminReview(adminUser, FraudDecision.REJECTED, rejectionReason);
                assessmentRepository.update(updated);
            });

            // Transition payment: SUSPICIOUS → FAILED
            PaymentRecord failedPayment = payment.withFailure("FRAUD_DETECTED", rejectionReason);
            paymentRepository.update(failedPayment);

            statusHistoryRepository.save(PaymentStatusHistoryRecord.failure(
                id, PaymentStatus.SUSPICIOUS, "FRAUD_DETECTED", rejectionReason, adminUser));

            logger.info("Admin {} rejected payment {} as FRAUD — transitioning to FAILED", adminUser, id);

            // Check if source account should be marked SUSPICIOUS (>5 rejections in 30 days)
            try {
                FraudRiskService.AccountRiskSummary riskSummary =
                    fraudRiskService.refreshAccountRiskStatus(payment.sourceAccount());
                if (riskSummary.suspicious()) {
                    logger.warn("Account {} flagged SUSPICIOUS after {} fraud rejections in 30 days",
                        payment.sourceAccount(), riskSummary.highRiskTransactionsLast30Days());
                }
            } catch (Exception ignored) {}

            Map<String, Object> response = new HashMap<>();
            response.put("status", "rejected");
            response.put("paymentId", id);
            response.put("newStatus", PaymentStatus.FAILED);
            response.put("reviewedBy", adminUser);
            response.put("rejectionReason", rejectionReason);
            response.put("message", "Payment rejected. Transitioning to FAILED.");

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error rejecting payment {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    // =========================================================================
    // GET /api/admin/fraud/stats
    // Overview statistics for the fraud dashboard
    // =========================================================================
    @GetMapping("/stats")
    public ResponseEntity<?> getFraudStats() {
        try {
            long totalApproved = assessmentRepository.countByDecision(FraudDecision.APPROVED);
            long totalSuspicious = assessmentRepository.countByDecision(FraudDecision.SUSPICIOUS);
            long totalRejected = assessmentRepository.countByDecision(FraudDecision.REJECTED);
            long pendingReview = assessmentRepository.countPendingReview();

            long totalSuspiciousPayments = paymentRepository.countByStatus(PaymentStatus.SUSPICIOUS);

            Map<String, Object> response = new HashMap<>();
            response.put("totalAssessed", totalApproved + totalSuspicious + totalRejected);
            response.put("totalApproved", totalApproved);
            response.put("totalSuspicious", totalSuspicious);
            response.put("totalRejected", totalRejected);
            response.put("pendingReview", pendingReview);
            response.put("paymentsAwaitingReview", totalSuspiciousPayments);
            response.put("fraudRate", totalApproved + totalRejected > 0
                ? String.format("%.1f%%", totalRejected * 100.0 / (totalApproved + totalRejected)) : "N/A");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    // =========================================================================
    // POST /api/admin/fraud/account/{accountNumber}/refresh-risk
    // Refresh account risk status based on recent fraud-rejected payments
    // =========================================================================
    @PostMapping("/account/{accountNumber}/refresh-risk")
    public ResponseEntity<?> refreshAccountRisk(@PathVariable String accountNumber) {
        try {
            FraudRiskService.AccountRiskSummary summary =
                fraudRiskService.refreshAccountRiskStatus(accountNumber);

            Map<String, Object> response = new HashMap<>();
            response.put("accountNumber", accountNumber);
            response.put("fraudRejectedLast30Days", summary.highRiskTransactionsLast30Days());
            response.put("accountFlaggedSuspicious", summary.suspicious());
            response.put("message", summary.suspicious()
                ? "Account has been flagged SUSPICIOUS due to repeated fraud rejections"
                : "Account risk level is normal");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    // =========================================================================
    // GET /api/admin/fraud/pending
    // Get payments currently awaiting admin review (status = SUSPICIOUS)
    // =========================================================================
    @GetMapping("/pending")
    public ResponseEntity<?> getPendingReview(
        @RequestParam(defaultValue = "50") int limit,
        @RequestParam(defaultValue = "0") int offset
    ) {
        try {
            // Return payments currently in SUSPICIOUS state
            List<PaymentRecord> suspiciousPayments = paymentRepository.findAll(PaymentStatus.SUSPICIOUS, limit, offset);
            long total = paymentRepository.countByStatus(PaymentStatus.SUSPICIOUS);

            List<Map<String, Object>> enriched = new ArrayList<>();
            for (PaymentRecord payment : suspiciousPayments) {
                Map<String, Object> item = new HashMap<>();
                item.put("payment", payment);
                assessmentRepository.findByPaymentId(payment.id()).ifPresent(a -> {
                    item.put("fraudScore", a.hybridFraudScore());
                    item.put("riskLevel", a.riskLevel());
                    item.put("explanation", a.explanation());
                    item.put("assessmentId", a.id());
                });
                enriched.add(item);
            }

            return ResponseEntity.ok(Map.of("payments", enriched, "total", total));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }
}

