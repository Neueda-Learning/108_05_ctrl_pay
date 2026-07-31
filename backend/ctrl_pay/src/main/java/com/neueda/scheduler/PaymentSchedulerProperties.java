package com.neueda.scheduler;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for payment scheduler.
 * Loads from application.properties:
 * - scheduler.interval-ms=5000
 * - scheduler.initial-delay-ms=2000
 * - scheduler.failure-rate=0.1
 */
@Component
@ConfigurationProperties(prefix = "scheduler")
public class PaymentSchedulerProperties {
    
    private int intervalMs = 5000;
    private int initialDelayMs = 2000;
    private double failureRate = 0.1;
    
    public int getIntervalMs() {
        return intervalMs;
    }
    
    public void setIntervalMs(int intervalMs) {
        this.intervalMs = intervalMs;
    }
    
    public int getInitialDelayMs() {
        return initialDelayMs;
    }
    
    public void setInitialDelayMs(int initialDelayMs) {
        this.initialDelayMs = initialDelayMs;
    }
    
    public double getFailureRate() {
        return failureRate;
    }
    
    public void setFailureRate(double failureRate) {
        this.failureRate = failureRate;
    }
}

