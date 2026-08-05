package com.neueda.repository.impl;

import com.neueda.domain.FraudRuleRecord;
import com.neueda.repository.FraudRuleRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * JDBC-based implementation of FraudRuleRepository
 */
@Repository
public class FraudRuleRepositoryImpl implements FraudRuleRepository {
    
    private final JdbcTemplate jdbcTemplate;
    private static final FraudRuleRowMapper ROW_MAPPER = new FraudRuleRowMapper();
    
    public FraudRuleRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    @Override
    public FraudRuleRecord save(FraudRuleRecord rule) {
        String sql = """
            INSERT INTO fraud_rules (rule_name, rule_type, description, is_active, severity, 
                                     order_of_execution, weight, rule_definition, triggering_conditions, mock_score,
                                     created_at, updated_at, created_by, updated_by)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, rule.ruleName());
            ps.setString(2, rule.ruleType());
            ps.setString(3, rule.description());
            ps.setBoolean(4, rule.isActive() != null ? rule.isActive() : true);
            ps.setString(5, rule.severity());
            ps.setInt(6, rule.orderOfExecution() != null ? rule.orderOfExecution() : 100);
            ps.setBigDecimal(7, rule.weight());
            ps.setString(8, rule.ruleDefinitionJson());
            ps.setString(9, rule.triggeringConditionsJson());
            ps.setInt(10, rule.mockScore() != null ? rule.mockScore() : 0);
            ps.setObject(11, rule.createdAt());
            ps.setObject(12, rule.updatedAt());
            ps.setString(13, rule.createdBy());
            ps.setString(14, rule.updatedBy());
            return ps;
        }, keyHolder);
        
        Long generatedId = keyHolder.getKey().longValue();
        return new FraudRuleRecord(
            generatedId,
            rule.ruleName(),
            rule.ruleType(),
            rule.description(),
            rule.isActive(),
            rule.severity(),
            rule.orderOfExecution(),
            rule.weight(),
            rule.ruleDefinitionJson(),
            rule.triggeringConditionsJson(),
            rule.mockScore(),
            rule.createdAt(),
            rule.updatedAt(),
            rule.createdBy(),
            rule.updatedBy()
        );
    }
    
    @Override
    public FraudRuleRecord update(FraudRuleRecord rule) {
        String sql = """
            UPDATE fraud_rules SET rule_name=?, rule_type=?, description=?, is_active=?, severity=?,
                                   order_of_execution=?, weight=?, rule_definition=?, triggering_conditions=?,
                                   mock_score=?, updated_at=?, updated_by=?
            WHERE id=?
            """;
        
        jdbcTemplate.update(sql,
            rule.ruleName(),
            rule.ruleType(),
            rule.description(),
            rule.isActive(),
            rule.severity(),
            rule.orderOfExecution(),
            rule.weight(),
            rule.ruleDefinitionJson(),
            rule.triggeringConditionsJson(),
            rule.mockScore(),
            Timestamp.valueOf(LocalDateTime.now()),
            "SYSTEM",
            rule.id()
        );
        
        return rule;
    }
    
    @Override
    public Optional<FraudRuleRecord> findById(Long id) {
        String sql = """
            SELECT id, rule_name, rule_type, description, is_active, severity,
                   order_of_execution, weight, rule_definition, triggering_conditions,
                   mock_score, created_at, updated_at, created_by, updated_by
            FROM fraud_rules WHERE id = ?
            """;
        
        List<FraudRuleRecord> results = jdbcTemplate.query(sql, ROW_MAPPER, id);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }
    
    @Override
    public Optional<FraudRuleRecord> findByName(String ruleName) {
        String sql = """
            SELECT id, rule_name, rule_type, description, is_active, severity,
                   order_of_execution, weight, rule_definition, triggering_conditions,
                   mock_score, created_at, updated_at, created_by, updated_by
            FROM fraud_rules WHERE rule_name = ?
            """;
        
        List<FraudRuleRecord> results = jdbcTemplate.query(sql, ROW_MAPPER, ruleName);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }
    
    @Override
    public List<FraudRuleRecord> findAll() {
        String sql = """
            SELECT id, rule_name, rule_type, description, is_active, severity,
                   order_of_execution, weight, rule_definition, triggering_conditions,
                   mock_score, created_at, updated_at, created_by, updated_by
            FROM fraud_rules
            ORDER BY order_of_execution ASC, created_at DESC
            """;
        
        return jdbcTemplate.query(sql, ROW_MAPPER);
    }
    
    @Override
    public List<FraudRuleRecord> findAllActive() {
        String sql = """
            SELECT id, rule_name, rule_type, description, is_active, severity,
                   order_of_execution, weight, rule_definition, triggering_conditions,
                   mock_score, created_at, updated_at, created_by, updated_by
            FROM fraud_rules
            WHERE is_active = true
            ORDER BY order_of_execution ASC, created_at DESC
            """;
        
        return jdbcTemplate.query(sql, ROW_MAPPER);
    }
    
    @Override
    public List<FraudRuleRecord> findByType(String ruleType) {
        String sql = """
            SELECT id, rule_name, rule_type, description, is_active, severity,
                   order_of_execution, weight, rule_definition, triggering_conditions,
                   mock_score, created_at, updated_at, created_by, updated_by
            FROM fraud_rules
            WHERE rule_type = ?
            ORDER BY order_of_execution ASC
            """;
        
        return jdbcTemplate.query(sql, ROW_MAPPER, ruleType);
    }
    
    @Override
    public void deleteById(Long id) {
        String sql = "DELETE FROM fraud_rules WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }
    
    @Override
    public void toggleActive(Long id) {
        String sql = "UPDATE fraud_rules SET is_active = NOT is_active, updated_at = ?, updated_by = ? WHERE id = ?";
        jdbcTemplate.update(sql, Timestamp.valueOf(LocalDateTime.now()), "SYSTEM", id);
    }
    
    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM fraud_rules";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        return count != null ? count : 0;
    }
    
    @Override
    public long countActive() {
        String sql = "SELECT COUNT(*) FROM fraud_rules WHERE is_active = true";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        return count != null ? count : 0;
    }
    
    /**
     * Row mapper for FraudRuleRecord
     */
    private static class FraudRuleRowMapper implements RowMapper<FraudRuleRecord> {
        @Override
        public FraudRuleRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new FraudRuleRecord(
                rs.getLong("id"),
                rs.getString("rule_name"),
                rs.getString("rule_type"),
                rs.getString("description"),
                rs.getBoolean("is_active"),
                rs.getString("severity"),
                rs.getInt("order_of_execution"),
                rs.getBigDecimal("weight"),
                rs.getString("rule_definition"),
                rs.getString("triggering_conditions"),
                rs.getInt("mock_score"),
                rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null,
                rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null,
                rs.getString("created_by"),
                rs.getString("updated_by")
            );
        }
    }
}

