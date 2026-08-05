package com.neueda.repository;

import java.util.List;
import java.util.Optional;

import com.neueda.domain.MLModelPredictionRecord;

/**
 * Repository for ML model predictions.
 * Tracks individual predictions for performance monitoring and accuracy analysis.
 */
public interface MLModelPredictionRepository {
    
    /**
     * Save a new prediction record
     */
    MLModelPredictionRecord save(MLModelPredictionRecord prediction);
    
    /**
     * Update a prediction record (e.g., with ground truth)
     */
    MLModelPredictionRecord update(MLModelPredictionRecord prediction);
    
    /**
     * Find prediction by ID
     */
    Optional<MLModelPredictionRecord> findById(Long id);
    
    /**
     * Find predictions for a specific payment
     */
    List<MLModelPredictionRecord> findByPaymentId(Long paymentId);
    
    /**
     * Find predictions by ML model
     */
    List<MLModelPredictionRecord> findByMLModelId(Long mlModelId);
    
    /**
     * Find predictions by assessment
     */
    List<MLModelPredictionRecord> findByAssessmentId(Long assessmentId);
    
    /**
     * Get prediction accuracy metrics for a model
     * Returns count of correct/incorrect predictions
     */
    PredictionAccuracyMetrics getAccuracyMetrics(Long mlModelId);
    
    /**
     * Get precision metric (true positives / (true positives + false positives))
     */
    Double getPrecision(Long mlModelId);
    
    /**
     * Get recall metric (true positives / (true positives + false negatives))
     */
    Double getRecall(Long mlModelId);
    
    /**
     * Count total predictions for a model
     */
    long countByModelId(Long mlModelId);
    
    /**
     * Count correct predictions for a model
     */
    long countCorrectPredictions(Long mlModelId);
    
    /**
     * Accuracy metrics immutable record
     */
    record PredictionAccuracyMetrics(
        long totalPredictions,
        long correctPredictions,
        long falsePositives,
        long falseNegatives,
        long truePositives,
        long trueNegatives,
        double accuracy,
        double precision,
        double recall,
        double f1Score
    ) {}
}

