package com.neueda.repository.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.neueda.domain.FraudAuditEventRecord;
import com.neueda.repository.FraudAuditEventRepository;

/**
 * JDBC implementation of FraudAuditEventRepository.
 */
@Repository
public class JdbcFraudAuditEventRepositoryImpl implements FraudAuditEventRepository {

    private final JdbcTemplate jdbcTemplate;

    public JdbcFraudAuditEventRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final org.springframework.jdbc.core.RowMapper<FraudAuditEventRecord> ROW_MAPPER =
        (rs, rowNum) -> new FraudAuditEventRecord(
            rs.getLong("id"),
            rs.getLong("assessment_id"),
            rs.getString("event_type"),
            rs.getTimestamp("event_timestamp").toLocalDateTime(),
            rs.getString("triggered_by"),
            rs.getString("triggered_by_user_id"),
            rs.getString("event_data"),
            rs.getTimestamp("created_at").toLocalDateTime()
        );

    @Override
    public FraudAuditEventRecord save(FraudAuditEventRecord event) {
        try {
            jdbcTemplate.update(
                "INSERT INTO fraud_audit_events (assessment_id, event_type, event_timestamp, triggered_by, triggered_by_user_id, event_data, created_at) VALUES (?,?,?,?,?,?,NOW())",
                event.assessmentId(), event.eventType(), event.eventTimestamp(),
                event.triggeredBy(), event.triggeredByUserId(), event.eventDataJson());
        } catch (Exception ignored) {}
        return event;
    }

    @Override
    public Optional<FraudAuditEventRecord> findById(Long id) {
        try {
            List<FraudAuditEventRecord> r = jdbcTemplate.query("SELECT * FROM fraud_audit_events WHERE id=?", ROW_MAPPER, id);
            return r.isEmpty() ? Optional.empty() : Optional.of(r.get(0));
        } catch (Exception e) { return Optional.empty(); }
    }

    @Override
    public List<FraudAuditEventRecord> findByAssessmentId(Long assessmentId) {
        try {
            return jdbcTemplate.query(
                "SELECT * FROM fraud_audit_events WHERE assessment_id=? ORDER BY event_timestamp ASC", ROW_MAPPER, assessmentId);
        } catch (Exception e) { return List.of(); }
    }

    @Override
    public List<FraudAuditEventRecord> findByEventType(String eventType) {
        try {
            return jdbcTemplate.query("SELECT * FROM fraud_audit_events WHERE event_type=?", ROW_MAPPER, eventType);
        } catch (Exception e) { return List.of(); }
    }

    @Override
    public List<FraudAuditEventRecord> findByTriggeredByUser(String userId) {
        try {
            return jdbcTemplate.query("SELECT * FROM fraud_audit_events WHERE triggered_by_user_id=?", ROW_MAPPER, userId);
        } catch (Exception e) { return List.of(); }
    }

    @Override
    public long countByEventType(String eventType) {
        try {
            Long r = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM fraud_audit_events WHERE event_type=?", Long.class, eventType);
            return r != null ? r : 0;
        } catch (Exception e) { return 0; }
    }
}

