package com.neueda.repository;

import java.math.BigDecimal;
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
import java.sql.Statement;
import java.sql.PreparedStatement;

import com.neueda.domain.FraudAssessmentRecord;
import com.neueda.domain.FraudDecision;
import com.neueda.domain.FraudRiskLevel;

@Repository
public class JdbcFraudAssessmentRepository implements FraudAssessmentRepository {

    private final JdbcTemplate jdbcTemplate;
    
    private static final RowMapper<FraudAssessmentRecord> ROW_MAPPER = (rs, rowNum) -> 
        new FraudAssessmentRecord(
            rs.getLong("id"),
            rs.getLong("payment_id"),
            rs.getBigDecimal("hybrid_fraud_score"),
            rs.getBigDecimal("rule_engine_score"),
            rs.getBigDecimal("ml_fraud_probability"),
            rs.getString("triggered_rules_json"),
            rs.getString("rule_scores_json"),
            FraudDecision.valueOf(rs.getString("decision")),
            FraudRiskLevel.valueOf(rs.getString("risk_level")),
            rs.getString("explanation"),
            rs.getString("reviewed_by"),
            rs.getTimestamp("reviewed_at") != null ? rs.getTimestamp("reviewed_at").toLocalDateTime() : null,
            rs.getString("reviewer_notes"),
            rs.getTimestamp("created_at").toLocalDateTime(),
            rs.getTimestamp("updated_at").toLocalDateTime()
        );

    public JdbcFraudAssessmentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public FraudAssessmentRecord save(FraudAssessmentRecord assessment) {
        String sql = """
            INSERT INTO fraud_assessments 
            (payment_id, hybrid_fraud_score, rule_engine_score, ml_fraud_probability, 
             triggered_rules_json, rule_scores_json, decision, risk_level, explanation, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
            """;
        
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setLong(1, assessment.paymentId());
            ps.setBigDecimal(2, assessment.hybridFraudScore());
            ps.setBigDecimal(3, assessment.ruleEngineScore());
            ps.setBigDecimal(4, assessment.mlFraudProbability());
            ps.setString(5, assessment.triggeredRulesJson());
            ps.setString(6, assessment.ruleScoresJson());
            ps.setString(7, assessment.decision().name());
            ps.setString(8, assessment.riskLevel().name());
            ps.setString(9, assessment.explanation());
            return ps;
        }, keyHolder);
        
        Long id = keyHolder.getKey().longValue();
        return assessment;
    }

    @Override
    public FraudAssessmentRecord update(FraudAssessmentRecord assessment) {
        String sql = """
            UPDATE fraud_assessments 
            SET hybrid_fraud_score = ?, rule_engine_score = ?, ml_fraud_probability = ?,
                triggered_rules_json = ?, rule_scores_json = ?, decision = ?, risk_level = ?,
                explanation = ?, reviewed_by = ?, reviewed_at = ?, reviewer_notes = ?, updated_at = NOW()
            WHERE id = ?
            """;
        
        jdbcTemplate.update(sql, 
            assessment.hybridFraudScore(),
            assessment.ruleEngineScore(),
            assessment.mlFraudProbability(),
            assessment.triggeredRulesJson(),
            assessment.ruleScoresJson(),
            assessment.decision().name(),
            assessment.riskLevel().name(),
            assessment.explanation(),
            assessment.reviewedBy(),
            assessment.reviewedAt(),
            assessment.reviewerNotes(),
            assessment.id()
        );
        
        return assessment;
    }

    @Override
    public Optional<FraudAssessmentRecord> findById(Long id) {
        String sql = "SELECT * FROM fraud_assessments WHERE id = ?";
        try {
            FraudAssessmentRecord result = jdbcTemplate.queryForObject(sql, ROW_MAPPER, id);
            return Optional.ofNullable(result);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<FraudAssessmentRecord> findByPaymentId(Long paymentId) {
        String sql = "SELECT * FROM fraud_assessments WHERE payment_id = ? ORDER BY created_at DESC LIMIT 1";
        try {
            FraudAssessmentRecord result = jdbcTemplate.queryForObject(sql, ROW_MAPPER, paymentId);
            return Optional.ofNullable(result);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public List<FraudAssessmentRecord> findAll(
        FraudDecision decision,
        String riskLevel,
        LocalDateTime dateFrom,
        LocalDateTime dateTo,
        int limit,
        int offset
    ) {
        StringBuilder sql = new StringBuilder("SELECT * FROM fraud_assessments WHERE 1=1");
        List<Object> params = new java.util.ArrayList<>();
        
        if (decision != null) {
            sql.append(" AND decision = ?");
            params.add(decision.name());
        }
        
        if (riskLevel != null) {
            sql.append(" AND risk_level = ?");
            params.add(riskLevel);
        }
        
        if (dateFrom != null) {
            sql.append(" AND created_at >= ?");
            params.add(dateFrom);
        }
        
        if (dateTo != null) {
            sql.append(" AND created_at <= ?");
            params.add(dateTo);
        }
        
        sql.append(" ORDER BY created_at DESC LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);
        
        return jdbcTemplate.query(sql.toString(), ROW_MAPPER, params.toArray());
    }

    @Override
    public long countByDecision(FraudDecision decision) {
        String sql = "SELECT COUNT(*) FROM fraud_assessments WHERE decision = ?";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, decision.name());
        return count != null ? count : 0;
    }

    @Override
    public long countPendingReview() {
        String sql = "SELECT COUNT(*) FROM fraud_assessments WHERE decision = ? AND reviewed_by IS NULL";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, FraudDecision.SUSPICIOUS.name());
        return count != null ? count : 0;
    }

    @Override
    public List<FraudAssessmentRecord> findPendingReview(int limit, int offset) {
        String sql = """
            SELECT * FROM fraud_assessments 
            WHERE decision = ? AND reviewed_by IS NULL
            ORDER BY created_at ASC
            LIMIT ? OFFSET ?
            """;
        return jdbcTemplate.query(sql, ROW_MAPPER, 
            FraudDecision.SUSPICIOUS.name(), limit, offset);
    }

    @Override
    public List<FraudAssessmentRecord> findRecent(int limit) {
        String sql = "SELECT * FROM fraud_assessments ORDER BY created_at DESC LIMIT ?";
        return jdbcTemplate.query(sql, ROW_MAPPER, limit);
    }
}

