package com.neueda.controller;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.neueda.exception.PaymentProcessingException;
import com.neueda.service.AnalyticsService;

/**
 * REST Controller for Analytics & Reporting.
 * 
 * Endpoints:
 * - GET /api/analytics/success-rate - Overall success rate
 * - GET /api/analytics/status-distribution - Count by status
 * - GET /api/analytics/volume - Payment volume statistics
 * - GET /api/analytics/trends - Historical trend data
 */
@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {
    
    private final AnalyticsService analyticsService;
    
    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }
    
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
    @GetMapping("/success-rate")
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
    @GetMapping("/status-distribution")
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
    @GetMapping("/volume")
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
    @GetMapping("/trends")
    public ResponseEntity<Map<String, Object>> getTrendData() {
        try {
            Map<String, Object> metrics = analyticsService.getTrendData();
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            throw new PaymentProcessingException("Error retrieving trend data: " + e.getMessage());
        }
    }
}

