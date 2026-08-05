package com.neueda.fraud;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.neueda.domain.MLModelRecord;
import com.neueda.repository.MLModelPredictionRepository;
import com.neueda.repository.MLModelRepository;

/**
 * ML Model Service
 * Manages ML model lifecycle: registration, deployment, retirement, and performance tracking
 */
@Service
@Transactional
public class MLModelService {
    private static final Logger logger = LoggerFactory.getLogger(MLModelService.class);
    
    private final MLModelRepository modelRepository;
    private final MLModelPredictionRepository predictionRepository;
    private final MLModelRegistry modelRegistry;
    
    public MLModelService(
        MLModelRepository modelRepository,
        MLModelPredictionRepository predictionRepository,
        MLModelRegistry modelRegistry
    ) {
        this.modelRepository = modelRepository;
        this.predictionRepository = predictionRepository;
        this.modelRegistry = modelRegistry;
    }
    
    /**
     * Register a new trained model (without deploying it yet)
     */
    public MLModelRecord registerModel(
        String modelName,
        String modelVersion,
        String description,
        String modelPath,
        LocalDateTime trainingDate,
        String datasetName,
        Integer datasetSize,
        BigDecimal accuracyScore,
        BigDecimal precisionScore,
        BigDecimal recallScore,
        BigDecimal f1Score,
        BigDecimal aucScore,
        String createdBy
    ) {
        MLModelRecord model = MLModelRecord.create(
            modelName,
            modelVersion,
            description,
            "XGBOOST",  // Default to XGBoost, can be parameterized
            modelPath,
            trainingDate,
            datasetName,
            datasetSize,
            accuracyScore,
            precisionScore,
            recallScore,
            f1Score,
            aucScore,
            "STAGING",  // Register to staging first
            createdBy
        );
        
        MLModelRecord saved = modelRepository.save(model);
        logger.info("✅ Model registered: {} v{} | F1: {}% | Path: {}",
            modelName, modelVersion, f1Score, modelPath
        );
        return saved;
    }
    
    /**
     * Deploy (activate) a model to production
     */
    public MLModelRecord deployModel(Long modelId) {
        Optional<MLModelRecord> model = modelRepository.findById(modelId);
        if (model.isEmpty()) {
            throw new IllegalArgumentException("Model not found: " + modelId);
        }
        
        // Retire current active model (if any)
        List<MLModelRecord> activeModels = modelRepository.findAllActiveModels();
        for (MLModelRecord active : activeModels) {
            MLModelRecord retired = active.withRetirement();
            modelRepository.update(retired);
            logger.info("👋 Model retired: {} v{}", active.modelName(), active.modelVersion());
        }
        
        // Activate new model
        MLModelRecord deployingModel = model.get().withDeployment();
        MLModelRecord deployed = modelRepository.update(deployingModel);
        modelRegistry.activateModel(deployed.id());
        
        logger.info("🚀 Model deployed to production: {} v{} | Accuracy: {}%",
            deployed.modelName(), deployed.modelVersion(), deployed.accuracyScore()
        );
        
        return deployed;
    }
    
    /**
     * Rollback to previous model version (for emergency failover)
     */
    public MLModelRecord rollbackToPreviousModel(String modelName) {
        List<MLModelRecord> allModels = modelRepository.findByModelName(modelName);
        if (allModels.size() < 2) {
            throw new IllegalStateException("Cannot rollback: no previous model version available");
        }
        
        // Get second-most recent (previous) active model
        MLModelRecord previousModel = allModels.stream()
            .filter(m -> !m.isActive())
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No previous model to rollback to"));
        
        return deployModel(previousModel.id());
    }
    
    /**
     * Get currently active model
     */
    public Optional<MLModelRecord> getActiveModel() {
        return modelRegistry.getActiveModel();
    }
    
    /**
     * Get all deployed models for a given model name
     */
    public List<MLModelRecord> getDeploymentHistory(String modelName) {
        return modelRepository.findByModelName(modelName);
    }
    
    /**
     * Get model performance metrics
     */
    public MLModelRegistry.ModelPerformanceMetrics getModelPerformance() {
        return modelRegistry.getPerformanceMetrics();
    }
    
    /**
     * Record a model prediction (called during fraud assessment)
     * Note: Ground truth will be updated later through feedback loop
     */
    public void recordPrediction(
        Long mlModelId,
        Long paymentId,
        Long assessmentId,
        BigDecimal likelihood,
        BigDecimal confidence,
        Integer latencyMs
    ) {
        // This is done asynchronously to not block payment processing
        // Could be queued for async execution
        try {
            predictionRepository.save(
                com.neueda.domain.MLModelPredictionRecord.create(
                    mlModelId,
                    paymentId,
                    assessmentId,
                    likelihood,
                    confidence,
                    latencyMs
                )
            );
        } catch (Exception e) {
            logger.warn("⚠️ Failed to record ML prediction: {}", e.getMessage());
            // Don't fail payment processing due to logging failure
        }
    }
    
    /**
     * Calculate model accuracy from recorded predictions
     */
    public ModelAccuracyReport getAccuracyReport(Long mlModelId) {
        long total = predictionRepository.countByModelId(mlModelId);
        long correct = predictionRepository.countCorrectPredictions(mlModelId);
        double accuracy = total > 0 ? (100.0 * correct / total) : 0;
        
        var metrics = predictionRepository.getAccuracyMetrics(mlModelId);
        
        return new ModelAccuracyReport(
            mlModelId,
            total,
            correct,
            metrics.accuracy(),
            metrics.precision(),
            metrics.recall(),
            metrics.f1Score(),
            LocalDateTime.now()
        );
    }
    
    /**
     * Model accuracy report immutable record
     */
    public record ModelAccuracyReport(
        Long modelId,
        long totalPredictions,
        long correctPredictions,
        double accuracy,
        double precision,
        double recall,
        double f1,
        LocalDateTime reportedAt
    ) {}
}

