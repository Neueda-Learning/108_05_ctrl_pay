package com.neueda.dto;

import java.time.LocalDateTime;

/**
 * DTO for ML Model Dashboard statistics.
 */
public record MLModelDashboardDTO(
    String activeModelName,
    String modelVersion,
    String trainingDataset,
    Double accuracy,
    Double precision,
    Double recall,
    Double f1Score,
    Double aucScore,
    Long totalPredictions,
    Double averagePredictionLatencyMs,
    LocalDateTime lastUpdated,
    LocalDateTime timestamp
) {}

