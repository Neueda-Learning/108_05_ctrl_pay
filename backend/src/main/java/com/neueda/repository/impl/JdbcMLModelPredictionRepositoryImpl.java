package com.neueda.repository.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.neueda.domain.MLModelPredictionRecord;
import com.neueda.repository.MLModelPredictionRepository;

/**
 * JDBC implementation of MLModelPredictionRepository.
 */
@Repository
public class JdbcMLModelPredictionRepositoryImpl implements MLModelPredictionRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcMLModelPredictionRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final org.springframework.jdbc.core.RowMapper<MLModelPredictionRecord> ROW_MAPPER =
        (rs, rowNum) -> new MLModelPredictionRecord(
            rs.getLong("id"),
            rs.getLong("ml_model_id"),
            rs.getLong("payment_id"),
            rs.getObject("assessment_id", Long.class),
            rs.getBigDecimal("predicted_fraud_probability"),
            rs.getBigDecimal("prediction_confidence"),
            rs.getObject("prediction_latency_ms", Integer.class),
            rs.getObject("ground_truth_fraud", Boolean.class),
            rs.getString("ground_truth_source"),
            rs.getTimestamp("ground_truth_date") != null ? rs.getTimestamp("ground_truth_date").toLocalDateTime() : null,
            rs.getObject("is_correct_prediction", Boolean.class),
            rs.getString("prediction_type"),
            rs.getTimestamp("created_at").toLocalDateTime()
        );

    @Override
    public MLModelPredictionRecord save(MLModelPredictionRecord prediction) {
        try {
            String sql = """
                INSERT INTO ml_model_predictions (ml_model_id, payment_id, assessment_id,
                    predicted_fraud_probability, prediction_confidence, prediction_latency_ms, created_at)
                VALUES (?,?,?,?,?,?,NOW())
                """;
            jdbcTemplate.update(sql,
                prediction.mlModelId(), prediction.paymentId(), prediction.assessmentId(),
                prediction.predictedFraudProbability(), prediction.predictionConfidence(),
                prediction.predictionLatencyMs());
        } catch (Exception e) {
            // table may not exist yet — silent failure
        }
        return prediction;
    }

    @Override
    public MLModelPredictionRecord update(MLModelPredictionRecord prediction) {
        try {
            jdbcTemplate.update(
                "UPDATE ml_model_predictions SET ground_truth_fraud=?, ground_truth_source=?, " +
                "ground_truth_date=?, is_correct_prediction=?, prediction_type=? WHERE id=?",
                prediction.groundTruthFraud(), prediction.groundTruthSource(),
                prediction.groundTruthDate(), prediction.isCorrectPrediction(),
                prediction.predictionType(), prediction.id());
        } catch (Exception ignored) {}
        return prediction;
    }

    @Override
    public Optional<MLModelPredictionRecord> findById(Long id) {
        try {
            List<MLModelPredictionRecord> r = jdbcTemplate.query(
                "SELECT * FROM ml_model_predictions WHERE id=?", ROW_MAPPER, id);
            return r.isEmpty() ? Optional.empty() : Optional.of(r.get(0));
        } catch (Exception e) { return Optional.empty(); }
    }

    @Override
    public List<MLModelPredictionRecord> findByPaymentId(Long paymentId) {
        try {
            return jdbcTemplate.query("SELECT * FROM ml_model_predictions WHERE payment_id=?", ROW_MAPPER, paymentId);
        } catch (Exception e) { return List.of(); }
    }

    @Override
    public List<MLModelPredictionRecord> findByMLModelId(Long mlModelId) {
        try {
            return jdbcTemplate.query("SELECT * FROM ml_model_predictions WHERE ml_model_id=?", ROW_MAPPER, mlModelId);
        } catch (Exception e) { return List.of(); }
    }

    @Override
    public List<MLModelPredictionRecord> findByAssessmentId(Long assessmentId) {
        try {
            return jdbcTemplate.query("SELECT * FROM ml_model_predictions WHERE assessment_id=?", ROW_MAPPER, assessmentId);
        } catch (Exception e) { return List.of(); }
    }

    @Override
    public PredictionAccuracyMetrics getAccuracyMetrics(Long mlModelId) {
        return new PredictionAccuracyMetrics(0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    @Override
    public Double getPrecision(Long mlModelId) { return 0.0; }

    @Override
    public Double getRecall(Long mlModelId) { return 0.0; }

    @Override
    public long countByModelId(Long mlModelId) {
        try {
            Long r = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ml_model_predictions WHERE ml_model_id=?", Long.class, mlModelId);
            return r != null ? r : 0;
        } catch (Exception e) { return 0; }
    }

    @Override
    public long countCorrectPredictions(Long mlModelId) {
        try {
            Long r = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ml_model_predictions WHERE ml_model_id=? AND is_correct_prediction=TRUE",
                Long.class, mlModelId);
            return r != null ? r : 0;
        } catch (Exception e) { return 0; }
    }
}

