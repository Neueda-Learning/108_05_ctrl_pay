package com.neueda.validation;

import com.neueda.domain.RuleType;
import com.neueda.validation.rules.ValidationRule;
import com.neueda.validation.rules.impl.AccountDifferenceRule;
import com.neueda.validation.rules.impl.AccountFormatRule;
import com.neueda.validation.rules.impl.AmountRangeRule;
import com.neueda.validation.rules.impl.CurrencyWhitelistRule;
import com.neueda.validation.rules.impl.MockSufficientFundsRule;

/**
 * Factory for instantiating the correct ValidationRule implementation based on RuleType.
 * Maps each RuleType enum to its corresponding rule implementation class.
 */
public class RuleFactory {
    
    /**
     * Create a ValidationRule instance for the given rule type.
     * 
     * @param ruleType the type of rule to create
     * @return new ValidationRule instance
     * @throws IllegalArgumentException if rule type is unknown
     */
    public static ValidationRule createRule(RuleType ruleType) {
        return switch (ruleType) {
            case AMOUNT_RANGE -> new AmountRangeRule();
            case CURRENCY_WHITELIST -> new CurrencyWhitelistRule();
            case ACCOUNT_FORMAT -> new AccountFormatRule();
            case ACCOUNT_DIFFERENCE -> new AccountDifferenceRule();
            case MOCK_SUFFICIENT_FUNDS -> new MockSufficientFundsRule();
        };
    }
}

