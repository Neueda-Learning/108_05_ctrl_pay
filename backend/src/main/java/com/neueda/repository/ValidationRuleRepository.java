package com.neueda.repository;

import java.util.List;
import java.util.Optional;

import com.neueda.domain.RuleType;
import com.neueda.domain.ValidationRuleRecord;

/**
 * Repository interface for ValidationRule data access operations.
 * Rules are stored in the database and can be enabled/disabled without code changes.
 * Uses Spring JdbcTemplate for SQL query execution.
 */
public interface ValidationRuleRepository {
    
    /**
     * Insert a new validation rule into the database.
     * 
     * @param rule validation rule record to insert
     * @return rule record with generated ID
     */
    ValidationRuleRecord save(ValidationRuleRecord rule);
    
    /**
     * Update an existing validation rule.
     * 
     * @param rule validation rule record with updated values
     * @return updated rule record
     */
    ValidationRuleRecord update(ValidationRuleRecord rule);
    
    /**
     * Retrieve a validation rule by its ID.
     * 
     * @param id rule ID
     * @return rule record, or empty if not found
     */
    Optional<ValidationRuleRecord> findById(Long id);
    
    /**
     * Retrieve a validation rule by its unique name.
     * 
     * @param name rule name
     * @return rule record, or empty if not found
     */
    Optional<ValidationRuleRecord> findByName(String name);
    
    /**
     * Retrieve all validation rules (both active and inactive).
     * 
     * @return list of all rule records, ordered by order_of_execution
     */
    List<ValidationRuleRecord> findAll();
    
    /**
     * Retrieve only active validation rules.
     * These are the rules that should be executed during payment validation.
     * 
     * @return list of active rule records, ordered by order_of_execution
     */
    List<ValidationRuleRecord> findActiveRules();
    
    /**
     * Retrieve validation rules filtered by type.
     * 
     * @param ruleType rule type to filter by
     * @return list of rule records matching the type
     */
    List<ValidationRuleRecord> findByRuleType(RuleType ruleType);
    
    /**
     * Retrieve active validation rules filtered by type.
     * 
     * @param ruleType rule type to filter by
     * @return list of active rule records matching the type
     */
    List<ValidationRuleRecord> findActiveByRuleType(RuleType ruleType);
    
    /**
     * Delete a validation rule by its ID.
     * 
     * @param id rule ID
     * @return true if rule was deleted, false if not found
     */
    boolean deleteById(Long id);
    
    /**
     * Count total number of validation rules.
     * 
     * @return count of rules
     */
    long count();
    
    /**
     * Count active validation rules.
     * 
     * @return count of active rules
     */
    long countActive();
}

