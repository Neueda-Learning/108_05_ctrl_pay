package com.neueda.repository.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.neueda.domain.MLModelRecord;
import com.neueda.repository.MLModelRepository;

/**
 * JDBC implementation of MLModelRepository.
 * Handles graceful failure when ml_models table doesn't exist yet.
 */
@Repository
public class JdbcMLModelRepositoryImpl implements MLModelRepository {

    private static final Logger logger = LoggerFactory.getLogger(JdbcMLModelRepositoryImpl.class);
    private final JdbcTemplate jdbcTemplate;

    private static final RowMapper<MLModelRecord> ROW_MAPPER = (rs, rowNum) -> new MLModelRecord(
        rs.getLong("id"),
        rs.getString("model_name"),
        rs.getString("model_version"),
        rs.getString("description"),
        rs.getString("model_type"),
        rs.getString("model_path"),
        rs.getTimestamp("training_date") != null ? rs.getTimestamp("training_date").toLocalDateTime() : null,
        rs.getString("training_dataset_name"),
        rs.getObject("training_dataset_size", Integer.class),
        rs.getBigDecimal("accuracy_score"),
        rs.getBigDecimal("precision_score"),
        rs.getBigDecimal("recall_score"),
        rs.getBigDecimal("f1_score"),
        rs.getBigDecimal("auc_score"),
        rs.getBigDecimal("false_positive_rate"),
        rs.getBigDecimal("false_negative_rate"),
        rs.getBoolean("is_active"),
        rs.getTimestamp("deployment_date") != null ? rs.getTimestamp("deployment_date").toLocalDateTime() : null,
        rs.getTimestamp("retirement_date") != null ? rs.getTimestamp("retirement_date").toLocalDateTime() : null,
        rs.getString("deployment_context"),
        rs.getString("feature_columns_json"),
        rs.getString("hyperparameters_json"),
        rs.getString("created_by"),
        rs.getTimestamp("created_at").toLocalDateTime(),
        rs.getTimestamp("updated_at").toLocalDateTime()
    );

    public JdbcMLModelRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public MLModelRecord save(MLModelRecord model) {
        String sql = """
            INSERT INTO ml_models (
                model_name, model_version, description, model_type, model_path,
                training_date, training_dataset_name, training_dataset_size,
                accuracy_score, precision_score, recall_score, f1_score, auc_score,
                false_positive_rate, false_negative_rate, is_active, deployment_date,
                retirement_date, deployment_context, feature_columns_json,
                hyperparameters_json, created_by, created_at, updated_at
            ) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,NOW(),NOW())
            """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            var ps = con.prepareStatement(sql, new String[]{"id"});
            ps.setString(1, model.modelName());
            ps.setString(2, model.modelVersion());
            ps.setString(3, model.description());
            ps.setString(4, model.modelType());
            ps.setString(5, model.modelPath());
            ps.setObject(6, model.trainingDate());
            ps.setString(7, model.trainingDatasetName());
            ps.setObject(8, model.trainingDatasetSize());
            ps.setBigDecimal(9, model.accuracyScore());
            ps.setBigDecimal(10, model.precisionScore());
            ps.setBigDecimal(11, model.recallScore());
            ps.setBigDecimal(12, model.f1Score());
            ps.setBigDecimal(13, model.aucScore());
            ps.setBigDecimal(14, model.falsePositiveRate());
            ps.setBigDecimal(15, model.falseNegativeRate());
            ps.setBoolean(16, model.isActive());
            ps.setObject(17, model.deploymentDate());
            ps.setObject(18, model.retirementDate());
            ps.setString(19, model.deploymentContext());
            ps.setString(20, model.featureColumnsJson());
            ps.setString(21, model.hyperparametersJson());
            ps.setString(22, model.createdBy());
            return ps;
        }, keyHolder);
        return model;
    }

    @Override
    public MLModelRecord update(MLModelRecord model) {
        String sql = """
            UPDATE ml_models SET description=?, is_active=?, deployment_date=?,
                retirement_date=?, updated_at=NOW()
            WHERE id=?
            """;
        jdbcTemplate.update(sql, model.description(), model.isActive(),
            model.deploymentDate(), model.retirementDate(), model.id());
        return model;
    }

    @Override
    public Optional<MLModelRecord> findById(Long id) {
        try {
            List<MLModelRecord> r = jdbcTemplate.query("SELECT * FROM ml_models WHERE id=?", ROW_MAPPER, id);
            return r.isEmpty() ? Optional.empty() : Optional.of(r.get(0));
        } catch (Exception e) { return Optional.empty(); }
    }

    @Override
    public Optional<MLModelRecord> findActiveModel() {
        try {
            List<MLModelRecord> r = jdbcTemplate.query(
                "SELECT * FROM ml_models WHERE is_active=TRUE ORDER BY deployment_date DESC LIMIT 1", ROW_MAPPER);
            return r.isEmpty() ? Optional.empty() : Optional.of(r.get(0));
        } catch (Exception e) { return Optional.empty(); }
    }

    @Override
    public List<MLModelRecord> findAllActiveModels() {
        try {
            return jdbcTemplate.query("SELECT * FROM ml_models WHERE is_active=TRUE ORDER BY deployment_date DESC", ROW_MAPPER);
        } catch (Exception e) { return List.of(); }
    }

    @Override
    public List<MLModelRecord> findByModelName(String modelName) {
        try {
            return jdbcTemplate.query("SELECT * FROM ml_models WHERE model_name=? ORDER BY created_at DESC", ROW_MAPPER, modelName);
        } catch (Exception e) { return List.of(); }
    }

    @Override
    public Optional<MLModelRecord> findByNameAndVersion(String modelName, String version) {
        try {
            List<MLModelRecord> r = jdbcTemplate.query(
                "SELECT * FROM ml_models WHERE model_name=? AND model_version=?", ROW_MAPPER, modelName, version);
            return r.isEmpty() ? Optional.empty() : Optional.of(r.get(0));
        } catch (Exception e) { return Optional.empty(); }
    }

    @Override
    public List<MLModelRecord> findByDeploymentContext(String context) {
        try {
            return jdbcTemplate.query("SELECT * FROM ml_models WHERE deployment_context=?", ROW_MAPPER, context);
        } catch (Exception e) { return List.of(); }
    }

    @Override
    public List<MLModelRecord> findAllDeployedModels() {
        try {
            return jdbcTemplate.query(
                "SELECT * FROM ml_models WHERE is_active=TRUE OR deployment_date IS NOT NULL ORDER BY deployment_date DESC",
                ROW_MAPPER);
        } catch (Exception e) { return List.of(); }
    }
}

