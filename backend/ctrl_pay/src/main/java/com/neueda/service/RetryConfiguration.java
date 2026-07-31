package com.neueda.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for payment retry logic.
 * Loads from application.properties:
 * - payment.retry.max-attempts=3
 * - payment.retry.initial-delay-ms=1000
 */
@Component
@ConfigurationProperties(prefix = "payment.retry")
public class RetryConfiguration {
    
    private int maxAttempts = 3;
    private long initialDelayMs = 1000;
    
    public int getMaxAttempts() {
        return maxAttempts;
    }
    
    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }
    
    public long getInitialDelayMs() {
        return initialDelayMs;
    }
    
    public void setInitialDelayMs(long initialDelayMs) {
        this.initialDelayMs = initialDelayMs;
    }
}

