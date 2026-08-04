package com.neueda.repository.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neueda.domain.RuleType;
import com.neueda.domain.Severity;
import com.neueda.domain.ValidationRuleRecord;
import com.neueda.repository.ValidationRuleRepository;

/**
 * JDBC-based implementation of ValidationRuleRepository using Spring's JdbcTemplate.
 * All SQL queries implemented with prepared statements for security.
 */
@Repository
public class ValidationRuleRepositoryImpl implements ValidationRuleRepository {
    
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private static final ValidationRuleRowMapper ROW_MAPPER = new ValidationRuleRowMapper();
    
    public ValidationRuleRepositoryImpl(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public ValidationRuleRecord save(ValidationRuleRecord rule) {
        String sql = """
            INSERT INTO validation_rules (name, description, rule_type, rule_definition, is_active, severity, order_of_execution, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var ps = connection.prepareStatement(sql, new String[]{"id"});
            ps.setString(1, rule.name());
            ps.setString(2, rule.description());
            ps.setString(3, rule.ruleType().name());
            ps.setString(4, rule.ruleDefinition().toString());
            ps.setBoolean(5, rule.isActive());
            ps.setString(6, rule.severity().name());
            ps.setInt(7, rule.orderOfExecution());
            ps.setObject(8, rule.createdAt());
            ps.setObject(9, rule.updatedAt());
            return ps;
        }, keyHolder);
        
        Long generatedId = keyHolder.getKey().longValue();
        return new ValidationRuleRecord(
            generatedId,
            rule.name(),
            rule.description(),
            rule.ruleType(),
            rule.ruleDefinition(),
            rule.isActive(),
            rule.severity(),
            rule.orderOfExecution(),
            rule.createdAt(),
            rule.updatedAt()
        );
    }

    @Override
    public ValidationRuleRecord update(ValidationRuleRecord rule) {
        String sql = """
            UPDATE validation_rules SET name=?, description=?, rule_type=?, rule_definition=?, is_active=?, severity=?, order_of_execution=?, updated_at=? WHERE id=?
            """;
        
        jdbcTemplate.update(sql,
            rule.name(),
            rule.description(),
            rule.ruleType().name(),
            rule.ruleDefinition().toString(),
            rule.isActive(),
            rule.severity().name(),
            rule.orderOfExecution(),
            rule.updatedAt(),
            rule.id()
        );
        
        return rule;
    }

    @Override
    public Optional<ValidationRuleRecord> findById(Long id) {
        String sql = """
            SELECT id, name, description, rule_type, rule_definition, is_active, severity, order_of_execution, created_at, updated_at
            FROM validation_rules
            WHERE id = ?
            """;
        
        List<ValidationRuleRecord> results = jdbcTemplate.query(sql, ROW_MAPPER, id);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public Optional<ValidationRuleRecord> findByName(String name) {
        String sql = """
            SELECT id, name, description, rule_type, rule_definition, is_active, severity, order_of_execution, created_at, updated_at
            FROM validation_rules
            WHERE name = ?
            """;
        
        List<ValidationRuleRecord> results = jdbcTemplate.query(sql, ROW_MAPPER, name);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public List<ValidationRuleRecord> findAll() {
        String sql = """
            SELECT id, name, description, rule_type, rule_definition, is_active, severity, order_of_execution, created_at, updated_at
            FROM validation_rules
            ORDER BY order_of_execution ASC
            """;
        
        return jdbcTemplate.query(sql, ROW_MAPPER);
    }

    @Override
    public List<ValidationRuleRecord> findActiveRules() {
        String sql = """
            SELECT id, name, description, rule_type, rule_definition, is_active, severity, order_of_execution, created_at, updated_at
            FROM validation_rules
            WHERE is_active = true
            ORDER BY order_of_execution ASC
            """;
        
        return jdbcTemplate.query(sql, ROW_MAPPER);
    }

    @Override
    public List<ValidationRuleRecord> findByRuleType(RuleType ruleType) {
        String sql = """
            SELECT id, name, description, rule_type, rule_definition, is_active, severity, order_of_execution, created_at, updated_at
            FROM validation_rules
            WHERE rule_type = ?
            ORDER BY order_of_execution ASC
            """;
        
        return jdbcTemplate.query(sql, ROW_MAPPER, ruleType.name());
    }

    @Override
    public List<ValidationRuleRecord> findActiveByRuleType(RuleType ruleType) {
        String sql = """
            SELECT id, name, description, rule_type, rule_definition, is_active, severity, order_of_execution, created_at, updated_at
            FROM validation_rules
            WHERE is_active = true AND rule_type = ?
            ORDER BY order_of_execution ASC
            """;
        
        return jdbcTemplate.query(sql, ROW_MAPPER, ruleType.name());
    }

    @Override
    public boolean deleteById(Long id) {
        String sql = "DELETE FROM validation_rules WHERE id = ?";
        int rowsAffected = jdbcTemplate.update(sql, id);
        return rowsAffected > 0;
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM validation_rules";
        Long result = jdbcTemplate.queryForObject(sql, Long.class);
        return result != null ? result : 0L;
    }

    @Override
    public long countActive() {
        String sql = "SELECT COUNT(*) FROM validation_rules WHERE is_active = true";
        Long result = jdbcTemplate.queryForObject(sql, Long.class);
        return result != null ? result : 0L;
    }
    
    /**
     * RowMapper for converting ResultSet rows to ValidationRuleRecord objects.
     */
    private static class ValidationRuleRowMapper implements RowMapper<ValidationRuleRecord> {
        private static final ObjectMapper OM = new ObjectMapper();
        
        @Override
        public ValidationRuleRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
            try {
                JsonNode ruleDefinition = OM.readTree(rs.getString("rule_definition"));
                return new ValidationRuleRecord(
                    rs.getLong("id"),
                    rs.getString("name"),
                    rs.getString("description"),
                    RuleType.valueOf(rs.getString("rule_type")),
                    ruleDefinition,
                    rs.getBoolean("is_active"),
                    Severity.valueOf(rs.getString("severity")),
                    rs.getInt("order_of_execution"),
                    rs.getTimestamp("created_at").toLocalDateTime(),
                    rs.getTimestamp("updated_at").toLocalDateTime()
                );
            } catch (Exception e) {
                throw new SQLException("Error mapping ValidationRuleRecord", e);
            }
        }
    }
}

