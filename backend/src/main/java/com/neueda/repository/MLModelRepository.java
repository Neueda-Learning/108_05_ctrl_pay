package com.neueda.repository;

import java.util.List;
import java.util.Optional;

import com.neueda.domain.MLModelRecord;

/**
 * Repository for ML model management.
 * Handles persistence of model versions, deployment status, and performance metrics.
 */
public interface MLModelRepository {
    
    /**
     * Save a new ML model record
     */
    MLModelRecord save(MLModelRecord model);
    
    /**
     * Update an existing model record
     */
    MLModelRecord update(MLModelRecord model);
    
    /**
     * Find model by ID
     */
    Optional<MLModelRecord> findById(Long id);
    
    /**
     * Find currently active model
     * Returns the latest deployed model
     */
    Optional<MLModelRecord> findActiveModel();
    
    /**
     * Find all active models (may be multiple during rollover)
     */
    List<MLModelRecord> findAllActiveModels();
    
    /**
     * Find all models by name
     */
    List<MLModelRecord> findByModelName(String modelName);
    
    /**
     * Find specific model version
     */
    Optional<MLModelRecord> findByNameAndVersion(String modelName, String version);
    
    /**
     * Find all models deployed in specific context
     */
    List<MLModelRecord> findByDeploymentContext(String context);
    
    /**
     * Get all models in deployment order (most recent first)
     */
    List<MLModelRecord> findAllDeployedModels();
}

