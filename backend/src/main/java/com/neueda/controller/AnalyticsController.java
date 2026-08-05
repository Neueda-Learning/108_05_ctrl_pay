package com.neueda.controller;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.neueda.dto.BulkPaymentAnalyticsDashboardDTO;
import com.neueda.dto.ComplianceDashboardDTO;
import com.neueda.dto.CustomerAnalyticsDashboardDTO;
import com.neueda.dto.DashboardOverviewDTO;
import com.neueda.dto.FraudDashboardDTO;
import com.neueda.dto.MLModelDashboardDTO;
import com.neueda.dto.TransactionDashboardDTO;
import com.neueda.exception.PaymentProcessingException;
import com.neueda.service.AnalyticsService;

/**
 * REST Controller for Analytics & Reporting.
 * 
 * Endpoints:
 * - GET /api/dashboard/overview - Platform overview statistics
 * - GET /api/dashboard/transactions - Transaction dashboard
 * - GET /api/dashboard/fraud - Fraud & risk dashboard
 * - GET /api/dashboard/customers - Customer analytics
 * - GET /api/dashboard/bulk-payments - Bulk payment analytics
 * - GET /api/dashboard/ml - ML model dashboard
 * - GET /api/dashboard/compliance - Compliance dashboard
 * - GET /api/analytics/success-rate - Overall success rate (legacy)
 * - GET /api/analytics/status-distribution - Count by status (legacy)
 * - GET /api/analytics/volume - Payment volume statistics (legacy)
 * - GET /api/analytics/trends - Historical trend data (legacy)
 */
@RestController
@RequestMapping("/api")
public class AnalyticsController {
    
    private final AnalyticsService analyticsService;
    
    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }
    
    /**
     * Get platform overview dashboard.
     * 
     * Request: GET /api/dashboard/overview
     * 
     * Response:
     * - 200 OK: Platform overview statistics
     * - 500 Internal Server Error: Server error
     * 
     * @return platform overview metrics
     */
    @GetMapping("/dashboard/overview")
    public ResponseEntity<DashboardOverviewDTO> getDashboardOverview() {
        try {
            DashboardOverviewDTO overview = analyticsService.getDashboardOverview();
            return ResponseEntity.ok(overview);
        } catch (Exception e) {
            throw new PaymentProcessingException("Error retrieving dashboard overview: " + e.getMessage());
        }
    }
    
    /**
     * Get transaction dashboard.
     * 
     * Request: GET /api/dashboard/transactions
     * 
     * Response:
     * - 200 OK: Transaction dashboard statistics
     * - 500 Internal Server Error: Server error
     * 
     * @return transaction metrics
     */
    @GetMapping("/dashboard/transactions")
    public ResponseEntity<TransactionDashboardDTO> getTransactionDashboard() {
        try {
            TransactionDashboardDTO dashboard = analyticsService.getTransactionDashboard();
            return ResponseEntity.ok(dashboard);
        } catch (Exception e) {
            throw new PaymentProcessingException("Error retrieving transaction dashboard: " + e.getMessage());
        }
    }
    
    /**
     * Get fraud and risk dashboard.
     * 
     * Request: GET /api/dashboard/fraud
     * 
     * Response:
     * - 200 OK: Fraud and risk statistics
     * - 500 Internal Server Error: Server error
     * 
     * @return fraud metrics
     */
    @GetMapping("/dashboard/fraud")
    public ResponseEntity<FraudDashboardDTO> getFraudDashboard() {
        try {
            FraudDashboardDTO dashboard = analyticsService.getFraudDashboard();
            return ResponseEntity.ok(dashboard);
        } catch (Exception e) {
            throw new PaymentProcessingException("Error retrieving fraud dashboard: " + e.getMessage());
        }
    }
    
    /**
     * Get customer analytics dashboard.
     * 
     * Request: GET /api/dashboard/customers
     * 
     * Response:
     * - 200 OK: Customer analytics statistics
     * - 500 Internal Server Error: Server error
     * 
     * @return customer metrics
     */
    @GetMapping("/dashboard/customers")
    public ResponseEntity<CustomerAnalyticsDashboardDTO> getCustomerAnalyticsDashboard() {
        try {
            CustomerAnalyticsDashboardDTO dashboard = analyticsService.getCustomerAnalyticsDashboard();
            return ResponseEntity.ok(dashboard);
        } catch (Exception e) {
            throw new PaymentProcessingException("Error retrieving customer analytics dashboard: " + e.getMessage());
        }
    }
    
    /**
     * Get bulk payment analytics dashboard.
     * 
     * Request: GET /api/dashboard/bulk-payments
     * 
     * Response:
     * - 200 OK: Bulk payment analytics statistics
     * - 500 Internal Server Error: Server error
     * 
     * @return bulk payment metrics
     */
    @GetMapping("/dashboard/bulk-payments")
    public ResponseEntity<BulkPaymentAnalyticsDashboardDTO> getBulkPaymentAnalyticsDashboard() {
        try {
            BulkPaymentAnalyticsDashboardDTO dashboard = analyticsService.getBulkPaymentAnalyticsDashboard();
            return ResponseEntity.ok(dashboard);
        } catch (Exception e) {
            throw new PaymentProcessingException("Error retrieving bulk payment analytics dashboard: " + e.getMessage());
        }
    }
    
    /**
     * Get ML model dashboard.
     * 
     * Request: GET /api/dashboard/ml
     * 
     * Response:
     * - 200 OK: ML model statistics
     * - 500 Internal Server Error: Server error
     * 
     * @return ML model metrics
     */
    @GetMapping("/dashboard/ml")
    public ResponseEntity<MLModelDashboardDTO> getMLModelDashboard() {
        try {
            MLModelDashboardDTO dashboard = analyticsService.getMLModelDashboard();
            return ResponseEntity.ok(dashboard);
        } catch (Exception e) {
            throw new PaymentProcessingException("Error retrieving ML model dashboard: " + e.getMessage());
        }
    }
    
    /**
     * Get compliance dashboard.
     * 
     * Request: GET /api/dashboard/compliance
     * 
     * Response:
     * - 200 OK: Compliance statistics
     * - 500 Internal Server Error: Server error
     * 
     * @return compliance metrics
     */
    @GetMapping("/dashboard/compliance")
    public ResponseEntity<ComplianceDashboardDTO> getComplianceDashboard() {
        try {
            ComplianceDashboardDTO dashboard = analyticsService.getComplianceDashboard();
            return ResponseEntity.ok(dashboard);
        } catch (Exception e) {
            throw new PaymentProcessingException("Error retrieving compliance dashboard: " + e.getMessage());
        }
    }
    
    // Legacy endpoints for backward compatibility
    
    /**
     * Get payment success rate.
     * 
     * Request: GET /api/analytics/success-rate
     * 
     * Response:
     * - 200 OK: Success rate data
     * - 500 Internal Server Error: Server error
     * 
     * @return success rate metrics
     */
    @GetMapping("/analytics/success-rate")
    public ResponseEntity<Map<String, Object>> getSuccessRate() {
        try {
            Map<String, Object> metrics = analyticsService.getSuccessRate();
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            throw new PaymentProcessingException("Error retrieving success rate: " + e.getMessage());
        }
    }
    
    /**
     * Get payment status distribution.
     * 
     * Request: GET /api/analytics/status-distribution
     * 
     * Response:
     * - 200 OK: Status distribution by count
     * - 500 Internal Server Error: Server error
     * 
     * @return status distribution metrics
     */
    @GetMapping("/analytics/status-distribution")
    public ResponseEntity<Map<String, Object>> getStatusDistribution() {
        try {
            Map<String, Object> metrics = analyticsService.getStatusDistribution();
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            throw new PaymentProcessingException("Error retrieving status distribution: " + e.getMessage());
        }
    }
    
    /**
     * Get payment volume statistics.
     * 
     * Request: GET /api/analytics/volume
     * 
     * Response:
     * - 200 OK: Volume statistics
     * - 500 Internal Server Error: Server error
     * 
     * @return volume metrics
     */
    @GetMapping("/analytics/volume")
    public ResponseEntity<Map<String, Object>> getVolumeStatistics() {
        try {
            Map<String, Object> metrics = analyticsService.getVolumeStatistics();
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            throw new PaymentProcessingException("Error retrieving volume statistics: " + e.getMessage());
        }
    }
    
    /**
     * Get payment trend data.
     * 
     * Request: GET /api/analytics/trends
     * 
     * Response:
     * - 200 OK: Trend metrics
     * - 500 Internal Server Error: Server error
     * 
     * @return trend metrics
     */
    @GetMapping("/analytics/trends")
    public ResponseEntity<Map<String, Object>> getTrendData() {
        try {
            Map<String, Object> metrics = analyticsService.getTrendData();
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            throw new PaymentProcessingException("Error retrieving trend data: " + e.getMessage());
        }
    }
}

