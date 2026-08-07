package com.neueda.fraud;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

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

import com.neueda.domain.MLModelRecord;
import com.neueda.repository.MLModelRepository;

@ExtendWith(MockitoExtension.class)
class MLModelRegistryTest {

    @Mock
    private MLModelRepository repo;

    private MLModelRegistry registry;

    private MLModelRecord sampleModel;

    @BeforeEach
    void setUp() {
        sampleModel = MLModelRecord.create(
            "FraudXGBoost", "1.0.0", "Main fraud model", "XGBOOST", "/models/xgb.bin",
            LocalDateTime.now(), "train.csv", 10000, BigDecimal.valueOf(0.95),
            BigDecimal.valueOf(0.94), BigDecimal.valueOf(0.93), BigDecimal.valueOf(0.935),
            BigDecimal.valueOf(0.98), "PROD", "admin"
        ).withDeployment();
    }

    @Test
    @DisplayName("Registry Initialization: Loads active model from database")
    void initializeActiveModel_Success() {
        // Arrange
        when(repo.findActiveModel()).thenReturn(Optional.of(sampleModel));

        // Act
        registry = new MLModelRegistry(repo);

        // Assert
        assertTrue(registry.hasActiveModel());
        assertEquals("1.0.0", registry.getActiveModelVersion());
        assertNotNull(registry.getPerformanceMetrics());
        assertEquals("FraudXGBoost", registry.getPerformanceMetrics().modelName());
    }

    @Test
    @DisplayName("Registry Initialization: Gracefully handles empty or exception")
    void initializeActiveModel_EmptyOrException() {
        // Arrange
        when(repo.findActiveModel()).thenThrow(new RuntimeException("Table missing"));

        // Act
        registry = new MLModelRegistry(repo);

        // Assert
        assertFalse(registry.hasActiveModel());
        assertEquals("NONE", registry.getActiveModelVersion());
        assertEquals(MLModelRegistry.ModelPerformanceMetrics.NONE, registry.getPerformanceMetrics());
    }

    @Test
    @DisplayName("activateModel: Switches active model")
    void activateModel_Success() {
        // Arrange
        when(repo.findActiveModel()).thenReturn(Optional.of(sampleModel));
        registry = new MLModelRegistry(repo);

        MLModelRecord newModel = MLModelRecord.create(
            "FraudXGBoost", "2.0.0", "New version", "XGBOOST", "/models/xgb2.bin",
            LocalDateTime.now(), "train2.csv", 20000, BigDecimal.valueOf(0.97),
            BigDecimal.valueOf(0.96), BigDecimal.valueOf(0.95), BigDecimal.valueOf(0.955),
            BigDecimal.valueOf(0.99), "PROD", "admin"
        ).withDeployment();
        when(repo.findById(2L)).thenReturn(Optional.of(newModel));

        // Act
        registry.activateModel(2L);

        // Assert
        assertEquals("2.0.0", registry.getActiveModelVersion());
    }
}
