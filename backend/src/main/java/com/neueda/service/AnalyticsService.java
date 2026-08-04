package com.neueda.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

import com.neueda.domain.PaymentStatus;
import com.neueda.repository.PaymentRepository;

/**
 * Analytics Service - Provides metrics and reporting for payment processing.
 * 
 * Metrics:
 * - Daily volume (payments per day for last N days)
 * - Success rate (% completed vs failed)
 * - Processing time statistics
 * - Error summary (top error codes)
 */
@Service
public class AnalyticsService {
    
    private final PaymentRepository paymentRepository;
    
    public AnalyticsService(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }
    
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






