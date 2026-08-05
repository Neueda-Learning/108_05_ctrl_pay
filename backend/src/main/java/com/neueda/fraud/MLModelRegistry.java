package com.neueda.fraud;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.neueda.domain.MLModelRecord;
import com.neueda.repository.MLModelRepository;

/**
 * ML Model Registry
 * Manages the currently active ML model in memory for fast access
 * Supports hot-swapping between model versions without application restart
 */
@Component
public class MLModelRegistry {
    private static final Logger logger = LoggerFactory.getLogger(MLModelRegistry.class);
    
    private final MLModelRepository repo;
    private volatile MLModelRecord activeModel;  // Volatile for thread-safe updates
    private volatile long lastRefreshTime = 0;
    private static final long CACHE_TTL_MS = 5 * 60 * 1000;  // 5 minute cache
    
    public MLModelRegistry(MLModelRepository mlModelRepository) {
        this.repo = mlModelRepository;
        this.initializeActiveModel();
    }
    
    /**
     * Initialize active model on startup — graceful failure if table doesn't exist yet.
     */
    private void initializeActiveModel() {
        try {
            Optional<MLModelRecord> model = repo.findActiveModel();
            if (model.isPresent()) {
                this.activeModel = model.get();
                this.lastRefreshTime = System.currentTimeMillis();
                logger.info("✅ ML Model Registry initialized: {} v{} | Accuracy: {}%",
                    model.get().modelName(), model.get().modelVersion(), model.get().accuracyScore());
            } else {
                logger.info("ℹ️ No active ML model in database. Fraud detection will use rules-only scoring.");
                this.activeModel = null;
            }
        } catch (Exception e) {
            // ml_models table may not exist yet — graceful startup
            logger.warn("⚠️ ML Model Registry could not load model (table may not exist): {}", e.getMessage());
            this.activeModel = null;
        }
    }
    
    /**
     * Get currently active model, with optional refresh from database
     */
    public Optional<MLModelRecord> getActiveModel() {
        // Refresh from database if cache expired or model null
        if (shouldRefreshCache()) {
            refreshModel();
        }
        return Optional.ofNullable(activeModel);
    }
    
    /**
     * Get model version for logging/audit
     */
    public String getActiveModelVersion() {
        return activeModel != null 
            ? activeModel.modelVersion() 
            : "NONE";
    }
    
    /**
     * Check if active model is available
     */
    public boolean hasActiveModel() {
        return getActiveModel().isPresent();
    }
    
    /**
     * Activate a new model version (typically during deployment)
     */
    public synchronized void activateModel(Long modelId) {
        Optional<MLModelRecord> newModel = repo.findById(modelId);
        if (newModel.isEmpty()) {
            logger.error("❌ Cannot activate model: ID not found: {}", modelId);
            return;
        }
        
        MLModelRecord model = newModel.get();
        if (!model.isActive()) {
            logger.warn("⚠️ Cannot activate model: not marked as active in database yet");
            return;
        }
        
        MLModelRecord oldModel = this.activeModel;
        this.activeModel = model;
        this.lastRefreshTime = System.currentTimeMillis();
        
        logger.info("✅ ML Model activated: {} v{} (replaced v{})",
            model.modelName(),
            model.modelVersion(),
            oldModel != null ? oldModel.modelVersion() : "NONE"
        );
    }
    
    /**
     * Manually refresh model from database
     */
    public synchronized void refreshModel() {
        try {
            Optional<MLModelRecord> latest = repo.findActiveModel();
            if (latest.isPresent()) {
                MLModelRecord model = latest.get();
                if (activeModel == null || !activeModel.id().equals(model.id())) {
                    logger.info("🔄 ML Model refreshed: {} v{}", model.modelName(), model.modelVersion());
                    this.activeModel = model;
                }
            } else {
                this.activeModel = null;
            }
        } catch (Exception e) {
            logger.warn("⚠️ ML Model refresh failed: {}", e.getMessage());
        }
        this.lastRefreshTime = System.currentTimeMillis();
    }
    
    /**
     * Check if cache needs refresh
     */
    private boolean shouldRefreshCache() {
        return activeModel == null || 
               (System.currentTimeMillis() - lastRefreshTime > CACHE_TTL_MS);
    }
    
    /**
     * Get model performance metrics for display/dashboard
     */
    public ModelPerformanceMetrics getPerformanceMetrics() {
        if (activeModel == null) {
            return ModelPerformanceMetrics.NONE;
        }
        return new ModelPerformanceMetrics(
            activeModel.modelName(),
            activeModel.modelVersion(),
            activeModel.deploymentContext(),
            activeModel.accuracyScore().doubleValue(),
            activeModel.precisionScore().doubleValue(),
            activeModel.recallScore().doubleValue(),
            activeModel.f1Score().doubleValue(),
            activeModel.aucScore().doubleValue(),
            activeModel.deploymentDate()
        );
    }
    
    /**
     * Model performance metrics for monitoring
     */
    public record ModelPerformanceMetrics(
        String modelName,
        String modelVersion,
        String deploymentContext,
        double accuracy,
        double precision,
        double recall,
        double f1,
        double auc,
        java.time.LocalDateTime deploymentDate
    ) {
        public static final ModelPerformanceMetrics NONE = 
            new ModelPerformanceMetrics("NONE", "N/A", "NONE", 0, 0, 0, 0, 0, null);
    }
}

