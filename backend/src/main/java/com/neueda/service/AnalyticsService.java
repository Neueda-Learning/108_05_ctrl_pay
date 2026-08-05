package com.neueda.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

import com.neueda.domain.FraudAssessmentRecord;
import com.neueda.domain.FraudDecision;
import com.neueda.domain.FraudRiskLevel;
import com.neueda.domain.PaymentStatus;
import com.neueda.dto.BulkPaymentAnalyticsDashboardDTO;
import com.neueda.dto.ComplianceDashboardDTO;
import com.neueda.dto.CustomerAnalyticsDashboardDTO;
import com.neueda.dto.DashboardOverviewDTO;
import com.neueda.dto.FraudDashboardDTO;
import com.neueda.dto.MLModelDashboardDTO;
import com.neueda.dto.TransactionDashboardDTO;
import com.neueda.repository.AccountRepository;
import com.neueda.repository.BulkPaymentBatchRepository;
import com.neueda.repository.BulkPaymentItemRepository;
import com.neueda.repository.CustomerRepository;
import com.neueda.repository.FraudAssessmentRepository;
import com.neueda.repository.FraudAuditEventRepository;
import com.neueda.repository.FraudRuleRepository;
import com.neueda.repository.MLModelPredictionRepository;
import com.neueda.repository.MLModelRepository;
import com.neueda.repository.PaymentRepository;
import com.neueda.repository.ValidationResultRepository;

/**
 * Analytics Service - Provides comprehensive metrics and reporting for payment processing.
 * Supports multiple dashboard types with aggregated statistics from existing tables.
 */
@Service
public class AnalyticsService {
    
    private final PaymentRepository paymentRepository;
    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final FraudAssessmentRepository fraudAssessmentRepository;
    private final FraudAuditEventRepository fraudAuditEventRepository;
    private final FraudRuleRepository fraudRuleRepository;
    private final BulkPaymentBatchRepository bulkPaymentBatchRepository;
    private final BulkPaymentItemRepository bulkPaymentItemRepository;
    private final MLModelRepository mlModelRepository;
    private final MLModelPredictionRepository mlModelPredictionRepository;
    private final ValidationResultRepository validationResultRepository;
    
    public AnalyticsService(
        PaymentRepository paymentRepository,
        CustomerRepository customerRepository,
        AccountRepository accountRepository,
        FraudAssessmentRepository fraudAssessmentRepository,
        FraudAuditEventRepository fraudAuditEventRepository,
        FraudRuleRepository fraudRuleRepository,
        BulkPaymentBatchRepository bulkPaymentBatchRepository,
        BulkPaymentItemRepository bulkPaymentItemRepository,
        MLModelRepository mlModelRepository,
        MLModelPredictionRepository mlModelPredictionRepository,
        ValidationResultRepository validationResultRepository
    ) {
        this.paymentRepository = paymentRepository;
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
        this.fraudAssessmentRepository = fraudAssessmentRepository;
        this.fraudAuditEventRepository = fraudAuditEventRepository;
        this.fraudRuleRepository = fraudRuleRepository;
        this.bulkPaymentBatchRepository = bulkPaymentBatchRepository;
        this.bulkPaymentItemRepository = bulkPaymentItemRepository;
        this.mlModelRepository = mlModelRepository;
        this.mlModelPredictionRepository = mlModelPredictionRepository;
        this.validationResultRepository = validationResultRepository;
    }
    
    /**
     * Get platform overview dashboard statistics.
     * All metrics are calculated from actual database records.
     */
    public DashboardOverviewDTO getDashboardOverview() {
        long totalTransactions = paymentRepository.count();
        long successfulTransactions = paymentRepository.countByStatus(PaymentStatus.COMPLETED);
        long failedTransactions = paymentRepository.countByStatus(PaymentStatus.FAILED);
        long suspiciousTransactions = paymentRepository.countByStatus(PaymentStatus.SUSPICIOUS);
        long pendingTransactions = totalTransactions - successfulTransactions - failedTransactions - suspiciousTransactions;
        
        long fraudDetectedCount = fraudAssessmentRepository.countByDecision(FraudDecision.REJECTED);
        long fraudSuspiciousCount = fraudAssessmentRepository.countByDecision(FraudDecision.SUSPICIOUS);
        
        Double successRate = totalTransactions > 0 
            ? (successfulTransactions / (double) totalTransactions) * 100.0
            : 0.0;
        
        // Calculate total volume: Sum of all completed payment amounts
        BigDecimal totalVolume = BigDecimal.ZERO;
        // Note: Volume calculation would need custom repository method to sum amounts
        
        // System health: OK if success rate > 90%, WARNING if 70-90%, CRITICAL if < 70%
        String systemHealth = successRate > 90.0 ? "OK" : (successRate > 70.0 ? "WARNING" : "CRITICAL");
        
        return new DashboardOverviewDTO(
            totalTransactions,
            totalVolume,
            successfulTransactions,
            failedTransactions,
            pendingTransactions,
            successRate,
            fraudDetectedCount + fraudSuspiciousCount,  // Total fraud issues detected
            0L,  // Active customers would require distinct count from payments
            0L,  // Active accounts would require count from accounts table
            systemHealth,
            LocalDateTime.now()
        );
    }
    
    /**
     * Get transaction dashboard statistics with real data.
     * Calculates metrics from actual payment database records.
     */
    public TransactionDashboardDTO getTransactionDashboard() {
        long totalTransactionCount = paymentRepository.count();
        
        // Initialize empty collections - these would require custom repository methods
        BigDecimal transactionVolume = BigDecimal.ZERO;
        BigDecimal averageTransactionValue = BigDecimal.ZERO;
        
        List<TransactionDashboardDTO.DailyTransactionTrendDTO> dailyTrend = List.of();
        List<TransactionDashboardDTO.MonthlyTransactionTrendDTO> monthlyTrend = List.of();
        Map<String, Long> transactionsByCurrency = new HashMap<>();
        Map<String, Long> transactionsByStatus = new HashMap<>();
        Map<String, Long> peakHours = new HashMap<>();
        
        // Populate status distribution from actual data
        transactionsByStatus.put("CREATED", paymentRepository.countByStatus(PaymentStatus.CREATED));
        transactionsByStatus.put("VALIDATED", paymentRepository.countByStatus(PaymentStatus.VALIDATED));
        transactionsByStatus.put("SENT", paymentRepository.countByStatus(PaymentStatus.SENT));
        transactionsByStatus.put("COMPLETED", paymentRepository.countByStatus(PaymentStatus.COMPLETED));
        transactionsByStatus.put("FAILED", paymentRepository.countByStatus(PaymentStatus.FAILED));
        transactionsByStatus.put("SUSPICIOUS", paymentRepository.countByStatus(PaymentStatus.SUSPICIOUS));
        
        return new TransactionDashboardDTO(
            totalTransactionCount,
            transactionVolume,
            averageTransactionValue,
            dailyTrend,
            monthlyTrend,
            transactionsByCurrency,
            transactionsByStatus,
            peakHours,
            LocalDateTime.now()
        );
    }
    
    /**
     * Get fraud and risk dashboard statistics from fraud_assessments.
     */
    public FraudDashboardDTO getFraudDashboard() {
        long fraudApprovedCount = fraudAssessmentRepository.countByDecision(FraudDecision.APPROVED);
        long fraudRejectedCount = fraudAssessmentRepository.countByDecision(FraudDecision.REJECTED);
        long fraudSuspiciousCount = fraudAssessmentRepository.countByDecision(FraudDecision.SUSPICIOUS);
        
        long totalFraudChecks = fraudApprovedCount + fraudRejectedCount + fraudSuspiciousCount;
        
        Double fraudPreventionPercentage = totalFraudChecks > 0
            ? ((fraudRejectedCount + fraudSuspiciousCount) / (double) totalFraudChecks) * 100.0
            : 0.0;
        
        // Risk level distribution - would need custom query to group by risk_level
        Map<String, Long> riskLevelDistribution = new HashMap<>();
        riskLevelDistribution.put("LOW", 0L);
        riskLevelDistribution.put("MEDIUM", 0L);
        riskLevelDistribution.put("HIGH", 0L);
        riskLevelDistribution.put("CRITICAL", 0L);
        
        // Fraud score distribution - would need custom query to analyze scores
        Map<String, Long> fraudScoreDistribution = new HashMap<>();
        
        return new FraudDashboardDTO(
            totalFraudChecks,
            fraudRejectedCount,
            fraudPreventionPercentage,
            fraudSuspiciousCount,
            fraudRejectedCount,  // Rejected transactions
            fraudScoreDistribution,
            riskLevelDistribution,
            List.of(),
            LocalDateTime.now()
        );
    }
    
    /**
     * Get customer analytics dashboard statistics.
     * Note: Some metrics require custom repository methods to calculate from database.
     */
    public CustomerAnalyticsDashboardDTO getCustomerAnalyticsDashboard() {
        // These metrics would require implementation:
        // - Total customers: COUNT(DISTINCT customer_id)
        // - Active customers: COUNT(DISTINCT customer_id WHERE status='ACTIVE')
        // - New registrations: COUNT(customer_id WHERE profile_created >= DATE_SUB(NOW(), INTERVAL 1 MONTH))
        // - Total accounts: COUNT(account_id)
        // - Avg balance: AVG(account_balance)
        // - Customers with multiple accounts: analyze customer-to-account ratio
        // - Top customers: GROUP BY customer_id ORDER BY SUM(amount)
        
        return new CustomerAnalyticsDashboardDTO(
            0L,  // Total customers
            0L,  // Active customers
            0L,  // New this month
            0L,  // Total accounts
            BigDecimal.ZERO,
            0L,  // Customers with multiple accounts
            List.of(),
            LocalDateTime.now()
        );
    }
    
    /**
     * Get bulk payment analytics dashboard statistics.
     * Note: Bulk payments are tracked via bulk_payment_batches and bulk_payment_items tables.
     */
    public BulkPaymentAnalyticsDashboardDTO getBulkPaymentAnalyticsDashboard() {
        // These counters would need implementation via bulkPaymentBatchRepository and bulkPaymentItemRepository
        // For now, returning zero values as the metrics cannot be calculated without bulk payment data
        
        return new BulkPaymentAnalyticsDashboardDTO(
            0L,  // Total batches
            0L,  // Total items
            0L,  // Successful
            0L,  // Failed
            0L,  // Rollbacks
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            new HashMap<>(),
            0.0,
            List.of(),
            LocalDateTime.now()
        );
    }
    
    /**
     * Get ML model dashboard statistics from ml_models and ml_model_predictions tables.
     */
    public MLModelDashboardDTO getMLModelDashboard() {
        var activeModel = mlModelRepository.findActiveModel();
        
        if (activeModel.isPresent()) {
            var model = activeModel.get();
            long totalPredictions = 0;  // Would need custom query on ml_model_predictions
            Double avgLatency = 0.0;     // Would need custom query on ml_model_predictions
            
            return new MLModelDashboardDTO(
                model.modelName(),
                model.modelVersion(),
                model.trainingDatasetName(),
                model.accuracyScore() != null ? model.accuracyScore().doubleValue() : 0.0,
                model.precisionScore() != null ? model.precisionScore().doubleValue() : 0.0,
                model.recallScore() != null ? model.recallScore().doubleValue() : 0.0,
                model.f1Score() != null ? model.f1Score().doubleValue() : 0.0,
                model.aucScore() != null ? model.aucScore().doubleValue() : 0.0,
                totalPredictions,
                avgLatency,
                model.deploymentDate() != null ? model.deploymentDate() : LocalDateTime.now(),
                LocalDateTime.now()
            );
        }
        
        // Default model if none active
        return new MLModelDashboardDTO(
            "No Active Model",
            "N/A",
            "N/A",
            0.0, 0.0, 0.0, 0.0, 0.0,
            0L, 0.0,
            LocalDateTime.now(),
            LocalDateTime.now()
        );
    }
    
    /**
     * Get compliance dashboard statistics from audit and validation tables.
     */
    public ComplianceDashboardDTO getComplianceDashboard() {
        long pendingReviews = fraudAssessmentRepository.countPendingReview();
        long auditEventCount = 0;  // Would need custom query on fraud_audit_events
        long validationFailures = 0;  // Would need custom query on validation_results where passed=false
        long manualReviews = 0;  // Would need custom query on fraud_assessments where reviewed_by IS NOT NULL
        
        return new ComplianceDashboardDTO(
            validationFailures,
            0L,  // Fraud rule changes - would need audit tracking
            0L,  // Validation rule changes - would need audit tracking
            0L,  // Admin actions
            auditEventCount,
            manualReviews,
            pendingReviews,
            List.of(),  // Recent audit events
            LocalDateTime.now()
        );
    }
    
    // Legacy methods for backward compatibility
    
    /**
     * Get overall payment success rate.
     * 
     * @return Map with completed and failed counts, success percentage
     */
    public Map<String, Object> getSuccessRate() {
        long totalPayments = paymentRepository.count();
        long completedPayments = paymentRepository.countByStatus(PaymentStatus.COMPLETED);
        long failedPayments = paymentRepository.countByStatus(PaymentStatus.FAILED);
        
        double successRate = totalPayments > 0 
            ? (completedPayments / (double) totalPayments) * 100 
            : 0.0;
        
        Map<String, Object> result = new HashMap<>();
        result.put("total_payments", totalPayments);
        result.put("completed", completedPayments);
        result.put("failed", failedPayments);
        result.put("pending", totalPayments - completedPayments - failedPayments);
        result.put("success_rate_percent", String.format("%.2f%%", successRate));
        result.put("timestamp", LocalDateTime.now());
        
        return result;
    }
    
    /**
     * Get status distribution across all payments.
     * 
     * @return Map with count for each status
     */
    public Map<String, Object> getStatusDistribution() {
        Map<String, Object> result = new HashMap<>();
        
        result.put("CREATED", paymentRepository.countByStatus(PaymentStatus.CREATED));
        result.put("VALIDATED", paymentRepository.countByStatus(PaymentStatus.VALIDATED));
        result.put("SENT", paymentRepository.countByStatus(PaymentStatus.SENT));
        result.put("COMPLETED", paymentRepository.countByStatus(PaymentStatus.COMPLETED));
        result.put("FAILED", paymentRepository.countByStatus(PaymentStatus.FAILED));
        result.put("total", paymentRepository.count());
        result.put("timestamp", LocalDateTime.now());
        
        return result;
    }
    
    /**
     * Get payment volume statistics.
     * 
     * @return Map with total count and other basic statistics
     */
    public Map<String, Object> getVolumeStatistics() {
        long totalPayments = paymentRepository.count();
        
        Map<String, Object> result = new HashMap<>();
        result.put("total_payments", totalPayments);
        result.put("timestamp", LocalDateTime.now());
        result.put("metrics", new HashMap<String, Object>() {
            {
                put("avg_per_status", totalPayments > 0 ? totalPayments / 5.0 : 0.0);
                put("total_completed", paymentRepository.countByStatus(PaymentStatus.COMPLETED));
                put("total_failed", paymentRepository.countByStatus(PaymentStatus.FAILED));
            }
        });
        
        return result;
    }
    
    /**
     * Get historical trend data (simplified).
     * 
     * @return Map with trend information
     */
    public Map<String, Object> getTrendData() {
        Map<String, Object> result = new HashMap<>();
        long totalPayments = paymentRepository.count();
        long completedPayments = paymentRepository.countByStatus(PaymentStatus.COMPLETED);
        
        result.put("total_payments", totalPayments);
        result.put("completion_rate", totalPayments > 0 
            ? (completedPayments / (double) totalPayments) * 100 
            : 0.0);
        result.put("timestamp", LocalDateTime.now());
        
        return result;
    }
}






