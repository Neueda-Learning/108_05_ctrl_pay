package com.neueda.fraud.rules;

import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry for mapping fraud rule names to their implementations.
 * 
 * This allows FraudRuleEngine to dynamically look up rule implementations
 * based on rule_name from the database without hardcoding rule instantiation.
 * 
 * All FraudRule beans must be registered here for dynamic loading to work.
 */
@Component
public class FraudRuleRegistry {
    
    private final Map<String, FraudRule> ruleRegistry;
    
    /**
     * Initialize registry with all available fraud rule implementations.
     * Uses Spring ApplicationContext to auto-discover all FraudRule beans.
     */
    public FraudRuleRegistry(ApplicationContext applicationContext) {
        this.ruleRegistry = new HashMap<>();
        
        // Auto-discover all beans implementing FraudRule interface
        Map<String, FraudRule> beansOfType = applicationContext.getBeansOfType(FraudRule.class);
        
        // Register each bean by its rule name
        for (FraudRule rule : beansOfType.values()) {
            this.ruleRegistry.put(rule.getRuleName(), rule);
        }
    }
    
    /**
     * Get a fraud rule implementation by its name.
     * 
     * @param ruleName the rule name (e.g., "LARGE_TRANSACTION_RULE")
     * @return the FraudRule implementation, or null if not found
     */
    public FraudRule getRule(String ruleName) {
        return ruleRegistry.get(ruleName);
    }
    
    /**
     * Check if a rule is registered.
     * 
     * @param ruleName the rule name
     * @return true if registered, false otherwise
     */
    public boolean hasRule(String ruleName) {
        return ruleRegistry.containsKey(ruleName);
    }
    
    /**
     * Get all registered rule names.
     * 
     * @return set of registered rule names
     */
    public java.util.Set<String> getRegisteredRuleNames() {
        return ruleRegistry.keySet();
    }
    
    /**
     * Get count of registered rules.
     * 
     * @return number of registered fraud rules
     */
    public int getRegistrySize() {
        return ruleRegistry.size();
    }
}

