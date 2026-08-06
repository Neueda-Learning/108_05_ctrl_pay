package com.neueda.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.neueda.domain.PaymentStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StatusTransitionValidatorTest {

    @ParameterizedTest
    @CsvSource({
        "CREATED, VALIDATED, true",
        "CREATED, SUSPICIOUS, true",
        "CREATED, FAILED, true",
        "CREATED, SENT, false",
        "VALIDATED, SENT, true",
        "VALIDATED, SUSPICIOUS, true",
        "VALIDATED, FAILED, true",
        "VALIDATED, COMPLETED, false",
        "SUSPICIOUS, VALIDATED, true",
        "SUSPICIOUS, FAILED, true",
        "SUSPICIOUS, SENT, false",
        "SENT, COMPLETED, true",
        "SENT, FAILED, true",
        "COMPLETED, FAILED, false",
        "FAILED, CREATED, false"
    })
    @DisplayName("isTransitionAllowed: Validate allowed and forbidden transitions")
    void testIsTransitionAllowed(PaymentStatus from, PaymentStatus to, boolean expected) {
        assertThat(StatusTransitionValidator.isTransitionAllowed(from, to)).isEqualTo(expected);
    }

    @Test
    @DisplayName("validateTransition: Should not throw on valid transition")
    void validateTransition_Valid() {
        assertThatCode(() -> StatusTransitionValidator.validateTransition(PaymentStatus.CREATED, PaymentStatus.VALIDATED))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validateTransition: Should throw IllegalStateException on invalid transition")
    void validateTransition_Invalid() {
        assertThatThrownBy(() -> StatusTransitionValidator.validateTransition(PaymentStatus.COMPLETED, PaymentStatus.FAILED))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Cannot transition from COMPLETED to FAILED");
    }
}
