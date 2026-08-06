package com.neueda.scheduler;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for bulk batch scheduler.
 * 
 * Properties:
 * scheduler.bulk.enabled - Enable/disable bulk batch processing (default: true)
 * scheduler.bulk.interval-ms - Interval between scheduler runs (default: 10000 ms)
 * scheduler.bulk.initial-delay-ms - Initial delay before first run (default: 5000 ms)
 * scheduler.bulk.batch-size - Number of batches to process per run (default: 10)
 */
@Component
@ConfigurationProperties(prefix = "scheduler.bulk")
public class BulkBatchSchedulerProperties {
    
    private int intervalMs = 10000;
    private int initialDelayMs = 5000;
    private int batchSize = 10;
    
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
    
    public int getBatchSize() {
        return batchSize;
    }
    
    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }
}

