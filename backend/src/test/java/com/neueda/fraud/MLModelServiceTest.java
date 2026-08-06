package com.neueda.fraud;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.neueda.domain.MLModelRecord;
import com.neueda.repository.MLModelPredictionRepository;
import com.neueda.repository.MLModelRepository;

@ExtendWith(MockitoExtension.class)
class MLModelServiceTest {

    @Mock
    private MLModelRepository modelRepository;
    @Mock
    private MLModelPredictionRepository predictionRepository;
    @Mock
    private MLModelRegistry modelRegistry;

    private MLModelService service;

    private MLModelRecord sampleModel;

    @BeforeEach
    void setUp() {
        service = new MLModelService(modelRepository, predictionRepository, modelRegistry);
        sampleModel = MLModelRecord.create(
            "FraudXGBoost", "1.0.0", "Main fraud model", "XGBOOST", "/models/xgb.bin",
            LocalDateTime.now(), "train.csv", 10000, BigDecimal.valueOf(0.95),
            BigDecimal.valueOf(0.94), BigDecimal.valueOf(0.93), BigDecimal.valueOf(0.935),
            BigDecimal.valueOf(0.98), "STAGING", "admin"
        );
        // Give sampleModel ID 1L
        sampleModel = new MLModelRecord(
            1L, sampleModel.modelName(), sampleModel.modelVersion(), sampleModel.description(),
            sampleModel.modelType(), sampleModel.modelPath(), sampleModel.trainingDate(),
            sampleModel.trainingDatasetName(), sampleModel.trainingDatasetSize(),
            sampleModel.accuracyScore(), sampleModel.precisionScore(), sampleModel.recallScore(),
            sampleModel.f1Score(), sampleModel.aucScore(), sampleModel.falsePositiveRate(),
            sampleModel.falseNegativeRate(), false, null, null, sampleModel.deploymentContext(),
            sampleModel.featureColumnsJson(), sampleModel.hyperparametersJson(), sampleModel.createdBy(),
            sampleModel.createdAt(), sampleModel.updatedAt()
        );
    }

    @Test
    @DisplayName("registerModel: Saves staging model to repository")
    void registerModel_Success() {
        // Arrange
        when(modelRepository.save(any())).thenReturn(sampleModel);

        // Act
        MLModelRecord result = service.registerModel(
            "FraudXGBoost", "1.0.0", "Main fraud model", "/models/xgb.bin",
            LocalDateTime.now(), "train.csv", 10000, BigDecimal.valueOf(0.95),
            BigDecimal.valueOf(0.94), BigDecimal.valueOf(0.93), BigDecimal.valueOf(0.935),
            BigDecimal.valueOf(0.98), "admin"
        );

        // Assert
        assertNotNull(result);
        verify(modelRepository).save(any());
    }

    @Test
    @DisplayName("deployModel: Retires active models and activates target model")
    void deployModel_Success() {
        // Arrange
        when(modelRepository.findById(1L)).thenReturn(Optional.of(sampleModel));
        when(modelRepository.findAllActiveModels()).thenReturn(List.of());
        
        MLModelRecord deployedModel = sampleModel.withDeployment();
        when(modelRepository.update(any())).thenReturn(deployedModel);

        // Act
        MLModelRecord result = service.deployModel(1L);

        // Assert
        assertNotNull(result);
        verify(modelRegistry).activateModel(1L);
    }

    @Test
    @DisplayName("deployModel: Throws exception when model not found")
    void deployModel_NotFound() {
        // Arrange
        when(modelRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> service.deployModel(99L));
    }

    @Test
    @DisplayName("recordPrediction: Saves prediction record asynchronously/safely")
    void recordPrediction_Success() {
        // Act
        service.recordPrediction(1L, 100L, 50L, BigDecimal.valueOf(0.8), BigDecimal.valueOf(0.9), 15);

        // Assert
        verify(predictionRepository).save(any());
    }

    @Test
    @DisplayName("getAccuracyReport: Calculates accuracy metrics")
    void getAccuracyReport_Success() {
        // Arrange
        when(predictionRepository.countByModelId(1L)).thenReturn(100L);
        when(predictionRepository.countCorrectPredictions(1L)).thenReturn(90L);
        
        MLModelPredictionRepository.PredictionAccuracyMetrics metrics =
            new MLModelPredictionRepository.PredictionAccuracyMetrics(
                100, 90, 5, 5, 45, 45, 90.0, 90.0, 90.0, 90.0
            );
        when(predictionRepository.getAccuracyMetrics(1L)).thenReturn(metrics);

        // Act
        MLModelService.ModelAccuracyReport report = service.getAccuracyReport(1L);

        // Assert
        assertNotNull(report);
        assertEquals(90.0, report.accuracy());
        assertEquals(100L, report.totalPredictions());
    }
}
