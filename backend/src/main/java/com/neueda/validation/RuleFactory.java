package com.neueda.validation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import com.neueda.domain.RuleType;
import com.neueda.service.AccountService;
import com.neueda.validation.rules.ValidationRule;
import com.neueda.validation.rules.impl.AccountDifferenceRule;
import com.neueda.validation.rules.impl.AccountFormatRule;
import com.neueda.validation.rules.impl.AmountRangeRule;
import com.neueda.validation.rules.impl.CurrencyWhitelistRule;
import com.neueda.validation.rules.impl.SufficientFundsRule;

/**
 * Factory for instantiating the correct ValidationRule implementation based on RuleType.
 * Maps each RuleType enum to its corresponding rule implementation class.
 * 
 * This factory is a Spring component to support dependency injection for rules that require services.
 * 
 * Rules that require dependencies:
 * - SUFFICIENT_FUNDS: requires AccountService for real balance validation
 * 
 * Rules that are stateless:
 * - AMOUNT_RANGE, CURRENCY_WHITELIST, ACCOUNT_FORMAT, ACCOUNT_DIFFERENCE
 */
@Component
public class RuleFactory {
    
    private final ApplicationContext applicationContext;
    
    @Autowired
    public RuleFactory(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }
    
    /**
     * Create a ValidationRule instance for the given rule type.
     * 
     * For rules that require Spring-managed dependencies (e.g., services),
     * this method injects them automatically.
     * 
     * @param ruleType the type of rule to create
     * @return new ValidationRule instance
     * @throws IllegalArgumentException if rule type is unknown
     */
    public ValidationRule createRule(RuleType ruleType) {
        return switch (ruleType) {
            // Stateless rules: no dependencies needed
            case AMOUNT_RANGE -> new AmountRangeRule();
            case CURRENCY_WHITELIST -> new CurrencyWhitelistRule();
            case ACCOUNT_FORMAT -> new AccountFormatRule();
            case ACCOUNT_DIFFERENCE -> new AccountDifferenceRule();
            
            // Rules with service dependencies
            case SUFFICIENT_FUNDS -> {
                // Real sufficient funds rule requires AccountService
                AccountService accountService = applicationContext.getBean(AccountService.class);
                yield new SufficientFundsRule(accountService);
            }
        };
    }
}

