package com.neueda.fraud.rules;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

@ExtendWith(MockitoExtension.class)
class FraudRuleRegistryTest {

    @Mock
    private ApplicationContext applicationContext;
    @Mock
    private FraudRule sampleRule;

    private FraudRuleRegistry registry;

    @BeforeEach
    void setUp() {
        when(sampleRule.getRuleName()).thenReturn("SAMPLE_RULE");
        when(applicationContext.getBeansOfType(FraudRule.class)).thenReturn(Map.of("sampleRule", sampleRule));

        registry = new FraudRuleRegistry(applicationContext);
    }

    @Test
    @DisplayName("FraudRuleRegistry: Auto-discovers and retrieves registered rules")
    void registry_Operations() {
        // Assert
        assertEquals(1, registry.getRegistrySize());
        assertTrue(registry.hasRule("SAMPLE_RULE"));
        assertNotNull(registry.getRule("SAMPLE_RULE"));
        assertFalse(registry.hasRule("NON_EXISTENT"));
        assertTrue(registry.getRegisteredRuleNames().contains("SAMPLE_RULE"));
    }
}
