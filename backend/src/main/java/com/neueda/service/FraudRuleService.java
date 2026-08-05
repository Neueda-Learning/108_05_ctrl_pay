package com.neueda.service;

import com.neueda.domain.FraudRuleRecord;
import com.neueda.repository.FraudRuleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing fraud detection rules
 */
@Service
@Transactional
public class FraudRuleService {
    
    private final FraudRuleRepository fraudRuleRepository;
    
    public FraudRuleService(FraudRuleRepository fraudRuleRepository) {
        this.fraudRuleRepository = fraudRuleRepository;
    }
    
    /**
     * Create a new fraud rule
     */
    public FraudRuleRecord createRule(FraudRuleRecord rule) {
        // Check for duplicate rule name
        if (fraudRuleRepository.findByName(rule.ruleName()).isPresent()) {
            throw new IllegalArgumentException("Rule with name '" + rule.ruleName() + "' already exists");
        }
        return fraudRuleRepository.save(rule);
    }
    
    /**
     * Update an existing fraud rule
     */
    public FraudRuleRecord updateRule(Long id, FraudRuleRecord rule) {
        FraudRuleRecord existing = fraudRuleRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Rule not found: " + id));
        
        // Don't allow changing rule name after creation
        FraudRuleRecord updated = existing.withUpdatedData(
            rule.description(),
            rule.ruleType(),
            rule.severity(),
            rule.orderOfExecution(),
            rule.weight(),
            rule.ruleDefinitionJson(),
            rule.triggeringConditionsJson()
        );
        
        return fraudRuleRepository.update(updated);
    }
    
    /**
     * Get rule by ID
     */
    public Optional<FraudRuleRecord> getRule(Long id) {
        return fraudRuleRepository.findById(id);
    }
    
    /**
     * Get rule by name
     */
    public Optional<FraudRuleRecord> getRuleByName(String ruleName) {
        return fraudRuleRepository.findByName(ruleName);
    }
    
    /**
     * Get all rules
     */
    public List<FraudRuleRecord> getAllRules() {
        return fraudRuleRepository.findAll();
    }
    
    /**
     * Get all active rules
     */
    public List<FraudRuleRecord> getActiveRules() {
        return fraudRuleRepository.findAllActive();
    }
    
    /**
     * Get rules by type
     */
    public List<FraudRuleRecord> getRulesByType(String ruleType) {
        return fraudRuleRepository.findByType(ruleType);
    }
    
    /**
     * Toggle rule active/inactive status
     */
    public void toggleRuleStatus(Long id) {
        if (fraudRuleRepository.findById(id).isEmpty()) {
            throw new IllegalArgumentException("Rule not found: " + id);
        }
        fraudRuleRepository.toggleActive(id);
    }
    
    /**
     * Delete a rule
     */
    public void deleteRule(Long id) {
        if (fraudRuleRepository.findById(id).isEmpty()) {
            throw new IllegalArgumentException("Rule not found: " + id);
        }
        fraudRuleRepository.deleteById(id);
    }
    
    /**
     * Get total rule count
     */
    public long getRuleCount() {
        return fraudRuleRepository.count();
    }
    
    /**
     * Get active rule count
     */
    public long getActiveRuleCount() {
        return fraudRuleRepository.countActive();
    }
}

