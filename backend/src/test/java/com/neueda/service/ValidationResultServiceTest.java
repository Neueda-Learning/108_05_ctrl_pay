package com.neueda.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ValidationResultServiceTest {

    @Test
    @DisplayName("processValidationResult: Returns true for valid and invalid inputs")
    void processValidationResult() {
        ValidationResultService service = new ValidationResultService();
        assertThat(service.processValidationResult(true, "Validation passed")).isTrue();
        assertThat(service.processValidationResult(false, "Validation failed")).isTrue();
    }
}
