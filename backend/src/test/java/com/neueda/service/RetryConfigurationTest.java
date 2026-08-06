package com.neueda.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RetryConfigurationTest {

    @Test
    @DisplayName("RetryConfiguration: Default getters and setters work correctly")
    void testGettersAndSetters() {
        // Arrange
        RetryConfiguration config = new RetryConfiguration();

        // Act & Assert Defaults
        assertEquals(3, config.getMaxAttempts());
        assertEquals(1000L, config.getInitialDelayMs());

        // Act Setters
        config.setMaxAttempts(5);
        config.setInitialDelayMs(2000L);

        // Assert Updated
        assertEquals(5, config.getMaxAttempts());
        assertEquals(2000L, config.getInitialDelayMs());
    }
}
