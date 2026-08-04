package com.neueda.repository.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neueda.domain.ValidationResultRecord;
import com.neueda.repository.ValidationResultRepository;

/**
 * JDBC-based implementation of ValidationResultRepository using Spring's JdbcTemplate.
 * All SQL queries implemented with prepared statements for security.
 */
@Repository
public class ValidationResultRepositoryImpl implements ValidationResultRepository {
    
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private static final ValidationResultRowMapper ROW_MAPPER = new ValidationResultRowMapper();
    
    public ValidationResultRepositoryImpl(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public ValidationResultRecord save(ValidationResultRecord result) {
        String sql = """
            INSERT INTO validation_results (payment_id, validation_rule_id, rule_name, rule_definition, passed, error_code, error_message, execution_time_ms, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var ps = connection.prepareStatement(sql, new String[]{"id"});
            ps.setLong(1, result.paymentId());
            ps.setLong(2, result.validationRuleId());
            ps.setString(3, result.ruleName());
            ps.setString(4, result.ruleDefinition().toString());
            ps.setBoolean(5, result.passed());
            ps.setString(6, result.errorCode());
            ps.setString(7, result.errorMessage());
            ps.setInt(8, result.executionTimeMs());
            ps.setObject(9, result.createdAt());
            return ps;
        }, keyHolder);
        
        Long generatedId = keyHolder.getKey().longValue();
        return new ValidationResultRecord(
            generatedId,
            result.paymentId(),
            result.validationRuleId(),
            result.ruleName(),
            result.ruleDefinition(),
            result.passed(),
            result.errorCode(),
            result.errorMessage(),
            result.executionTimeMs(),
            result.createdAt()
        );
    }

    @Override
    public List<ValidationResultRecord> findByPaymentId(Long paymentId) {
        String sql = """
            SELECT id, payment_id, validation_rule_id, rule_name, rule_definition, passed, error_code, error_message, execution_time_ms, created_at
            FROM validation_results
            WHERE payment_id = ?
            ORDER BY created_at ASC
            """;
        
        return jdbcTemplate.query(sql, ROW_MAPPER, paymentId);
    }

    @Override
    public List<ValidationResultRecord> findByPaymentId(Long paymentId, int limit, int offset) {
        String sql = """
            SELECT id, payment_id, validation_rule_id, rule_name, rule_definition, passed, error_code, error_message, execution_time_ms, created_at
            FROM validation_results
            WHERE payment_id = ?
            ORDER BY created_at DESC
            LIMIT ? OFFSET ?
            """;
        
        return jdbcTemplate.query(sql, ROW_MAPPER, paymentId, limit, offset);
    }

    @Override
    public List<ValidationResultRecord> findByValidationRuleId(Long validationRuleId) {
        String sql = """
            SELECT id, payment_id, validation_rule_id, rule_name, rule_definition, passed, error_code, error_message, execution_time_ms, created_at
            FROM validation_results
            WHERE validation_rule_id = ?
            ORDER BY created_at DESC
            """;
        
        return jdbcTemplate.query(sql, ROW_MAPPER, validationRuleId);
    }

    @Override
    public List<ValidationResultRecord> findFailedByValidationRuleId(Long validationRuleId) {
        String sql = """
            SELECT id, payment_id, validation_rule_id, rule_name, rule_definition, passed, error_code, error_message, execution_time_ms, created_at
            FROM validation_results
            WHERE validation_rule_id = ? AND passed = false
            ORDER BY created_at DESC
            """;
        
        return jdbcTemplate.query(sql, ROW_MAPPER, validationRuleId);
    }

    @Override
    public long countByPaymentId(Long paymentId) {
        String sql = "SELECT COUNT(*) FROM validation_results WHERE payment_id = ?";
        Long result = jdbcTemplate.queryForObject(sql, Long.class, paymentId);
        return result != null ? result : 0L;
    }

    @Override
    public long countByValidationRuleId(Long validationRuleId) {
        String sql = "SELECT COUNT(*) FROM validation_results WHERE validation_rule_id = ?";
        Long result = jdbcTemplate.queryForObject(sql, Long.class, validationRuleId);
        return result != null ? result : 0L;
    }

    @Override
    public long countFailedByValidationRuleId(Long validationRuleId) {
        String sql = "SELECT COUNT(*) FROM validation_results WHERE validation_rule_id = ? AND passed = false";
        Long result = jdbcTemplate.queryForObject(sql, Long.class, validationRuleId);
        return result != null ? result : 0L;
    }

    @Override
    public int deleteByPaymentId(Long paymentId) {
        String sql = "DELETE FROM validation_results WHERE payment_id = ?";
        return jdbcTemplate.update(sql, paymentId);
    }
    
    /**
     * RowMapper for converting ResultSet rows to ValidationResultRecord objects.
     */
    private static class ValidationResultRowMapper implements RowMapper<ValidationResultRecord> {
        private static final ObjectMapper OM = new ObjectMapper();
        
        @Override
        public ValidationResultRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
            try {
                JsonNode ruleDefinition = OM.readTree(rs.getString("rule_definition"));
                return new ValidationResultRecord(
                    rs.getLong("id"),
                    rs.getLong("payment_id"),
                    rs.getLong("validation_rule_id"),
                    rs.getString("rule_name"),
                    ruleDefinition,
                    rs.getBoolean("passed"),
                    rs.getString("error_code"),
                    rs.getString("error_message"),
                    rs.getInt("execution_time_ms"),
                    rs.getTimestamp("created_at").toLocalDateTime()
                );
            } catch (Exception e) {
                throw new SQLException("Error mapping ValidationResultRecord", e);
            }
        }
    }
}

