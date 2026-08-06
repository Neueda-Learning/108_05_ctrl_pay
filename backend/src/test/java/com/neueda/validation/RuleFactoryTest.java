package com.neueda.validation;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import com.neueda.domain.RuleType;
import com.neueda.service.AccountService;
import com.neueda.validation.rules.ValidationRule;
import com.neueda.validation.rules.impl.AccountDifferenceRule;
import com.neueda.validation.rules.impl.AccountFormatRule;
import com.neueda.validation.rules.impl.AmountRangeRule;
import com.neueda.validation.rules.impl.SufficientFundsRule;

@ExtendWith(MockitoExtension.class)
class RuleFactoryTest {

    @Mock
    private ApplicationContext applicationContext;
    @Mock
    private AccountService accountService;

    private RuleFactory ruleFactory;

    @BeforeEach
    void setUp() {
        ruleFactory = new RuleFactory(applicationContext);
    }

    @Test
    @DisplayName("createRule: Instantiates stateless and stateful validation rules correctly")
    void createRule_Types() {
        // Stateless rules
        ValidationRule amountRange = ruleFactory.createRule(RuleType.AMOUNT_RANGE);
        assertTrue(amountRange instanceof AmountRangeRule);

        ValidationRule accountFormat = ruleFactory.createRule(RuleType.ACCOUNT_FORMAT);
        assertTrue(accountFormat instanceof AccountFormatRule);

        ValidationRule accountDiff = ruleFactory.createRule(RuleType.ACCOUNT_DIFFERENCE);
        assertTrue(accountDiff instanceof AccountDifferenceRule);

        // Stateful rules (requiring AccountService)
        when(applicationContext.getBean(AccountService.class)).thenReturn(accountService);
        ValidationRule suffFunds = ruleFactory.createRule(RuleType.SUFFICIENT_FUNDS);
        assertTrue(suffFunds instanceof SufficientFundsRule);
        assertNotNull(suffFunds);
    }
}
