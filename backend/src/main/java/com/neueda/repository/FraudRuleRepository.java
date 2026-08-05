package com.neueda.repository;

import com.neueda.domain.FraudRuleRecord;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Fraud Rule management
 */
public interface FraudRuleRepository {
    
    /**
     * Save a new fraud rule
     */
    FraudRuleRecord save(FraudRuleRecord rule);
    
    /**
     * Update an existing fraud rule
     */
    FraudRuleRecord update(FraudRuleRecord rule);
    
    /**
     * Find rule by ID
     */
    Optional<FraudRuleRecord> findById(Long id);
    
    /**
     * Find rule by name
     */
    Optional<FraudRuleRecord> findByName(String ruleName);
    
    /**
     * Get all rules
     */
    List<FraudRuleRecord> findAll();
    
    /**
     * Get all active rules
     */
    List<FraudRuleRecord> findAllActive();
    
    /**
     * Get rules by type
     */
    List<FraudRuleRecord> findByType(String ruleType);
    
    /**
     * Delete rule by ID
     */
    void deleteById(Long id);
    
    /**
     * Toggle rule active status
     */
    void toggleActive(Long id);
    
    /**
     * Count total rules
     */
    long count();
    
    /**
     * Count active rules
     */
    long countActive();
}

