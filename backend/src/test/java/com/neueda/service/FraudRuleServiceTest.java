package com.neueda.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.neueda.domain.FraudRuleRecord;
import com.neueda.repository.FraudRuleRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class FraudRuleServiceTest {

    @Mock
    private FraudRuleRepository fraudRuleRepository;

    private FraudRuleService fraudRuleService;

    private FraudRuleRecord sampleRule;

    @BeforeEach
    void setUp() {
        fraudRuleService = new FraudRuleService(fraudRuleRepository);
        sampleRule = new FraudRuleRecord(
            1L, "LARGE_TX", "VELOCITY", "Large transaction rule", true, "HIGH",
            1, BigDecimal.valueOf(1.5), "{}", "{}", 0, LocalDateTime.now(), LocalDateTime.now(), "system", "system"
        );
    }

    @Test
    @DisplayName("createRule: Saves rule when rule name is unique")
    void createRule_Success() {
        when(fraudRuleRepository.findByName("LARGE_TX")).thenReturn(Optional.empty());
        when(fraudRuleRepository.save(any())).thenReturn(sampleRule);

        FraudRuleRecord created = fraudRuleService.createRule(sampleRule);

        assertThat(created.ruleName()).isEqualTo("LARGE_TX");
        verify(fraudRuleRepository).save(any());
    }

    @Test
    @DisplayName("createRule: Throws IllegalArgumentException on duplicate rule name")
    void createRule_Duplicate() {
        when(fraudRuleRepository.findByName("LARGE_TX")).thenReturn(Optional.of(sampleRule));

        assertThatThrownBy(() -> fraudRuleService.createRule(sampleRule))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("updateRule: Updates existing rule properties")
    void updateRule_Success() {
        when(fraudRuleRepository.findById(1L)).thenReturn(Optional.of(sampleRule));
        when(fraudRuleRepository.update(any())).thenAnswer(i -> i.getArgument(0));

        FraudRuleRecord updated = fraudRuleService.updateRule(1L, sampleRule);

        assertThat(updated).isNotNull();
        verify(fraudRuleRepository).update(any());
    }

    @Test
    @DisplayName("toggleRuleStatus: Toggles active status when rule exists")
    void toggleRuleStatus_Success() {
        when(fraudRuleRepository.findById(1L)).thenReturn(Optional.of(sampleRule));

        fraudRuleService.toggleRuleStatus(1L);

        verify(fraudRuleRepository).toggleActive(1L);
    }

    @Test
    @DisplayName("deleteRule: Deletes rule when rule exists")
    void deleteRule_Success() {
        when(fraudRuleRepository.findById(1L)).thenReturn(Optional.of(sampleRule));

        fraudRuleService.deleteRule(1L);

        verify(fraudRuleRepository).deleteById(1L);
    }
}
