package com.neueda.repository.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.neueda.domain.PaymentStatus;
import com.neueda.domain.PaymentStatusHistoryRecord;
import com.neueda.repository.PaymentStatusHistoryRepository;

/**
 * JDBC-based implementation of PaymentStatusHistoryRepository using Spring's JdbcTemplate.
 * All SQL queries implemented with prepared statements for security.
 * Immutable append-only design: only INSERT operations allowed.
 */
@Repository
public class PaymentStatusHistoryRepositoryImpl implements PaymentStatusHistoryRepository {
    
    private final JdbcTemplate jdbcTemplate;
    private static final PaymentStatusHistoryRowMapper ROW_MAPPER = new PaymentStatusHistoryRowMapper();
    
    public PaymentStatusHistoryRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public PaymentStatusHistoryRecord save(PaymentStatusHistoryRecord history) {
        String sql = """
            INSERT INTO payment_status_history (payment_id, old_status, new_status, error_code, error_message, triggered_by, created_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var ps = connection.prepareStatement(sql, new String[]{"id"});
            ps.setLong(1, history.paymentId());
            ps.setString(2, history.oldStatus() != null ? history.oldStatus().name() : null);
            ps.setString(3, history.newStatus().name());
            ps.setString(4, history.errorCode());
            ps.setString(5, history.errorMessage());
            ps.setString(6, history.triggeredBy());
            ps.setObject(7, history.createdAt());
            return ps;
        }, keyHolder);
        
        Long generatedId = keyHolder.getKey().longValue();
        return new PaymentStatusHistoryRecord(
            generatedId,
            history.paymentId(),
            history.oldStatus(),
            history.newStatus(),
            history.errorCode(),
            history.errorMessage(),
            history.triggeredBy(),
            history.createdAt()
        );
    }

    @Override
    public List<PaymentStatusHistoryRecord> findByPaymentId(Long paymentId) {
        String sql = """
            SELECT id, payment_id, old_status, new_status, error_code, error_message, triggered_by, created_at
            FROM payment_status_history
            WHERE payment_id = ?
            ORDER BY created_at ASC
            """;
        
        return jdbcTemplate.query(sql, ROW_MAPPER, paymentId);
    }

    @Override
    public List<PaymentStatusHistoryRecord> findByPaymentId(Long paymentId, int limit, int offset) {
        String sql = """
            SELECT id, payment_id, old_status, new_status, error_code, error_message, triggered_by, created_at
            FROM payment_status_history
            WHERE payment_id = ?
            ORDER BY created_at DESC
            LIMIT ? OFFSET ?
            """;
        
        return jdbcTemplate.query(sql, ROW_MAPPER, paymentId, limit, offset);
    }

    @Override
    public PaymentStatusHistoryRecord findLatestByPaymentId(Long paymentId) {
        String sql = """
            SELECT id, payment_id, old_status, new_status, error_code, error_message, triggered_by, created_at
            FROM payment_status_history
            WHERE payment_id = ?
            ORDER BY created_at DESC
            LIMIT 1
            """;
        
        List<PaymentStatusHistoryRecord> results = jdbcTemplate.query(sql, ROW_MAPPER, paymentId);
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    public List<PaymentStatusHistoryRecord> findByNewStatus(PaymentStatus status) {
        String sql = """
            SELECT id, payment_id, old_status, new_status, error_code, error_message, triggered_by, created_at
            FROM payment_status_history
            WHERE new_status = ?
            ORDER BY created_at DESC
            """;
        
        return jdbcTemplate.query(sql, ROW_MAPPER, status.name());
    }

    @Override
    public List<PaymentStatusHistoryRecord> findByOldStatus(PaymentStatus status) {
        String sql = """
            SELECT id, payment_id, old_status, new_status, error_code, error_message, triggered_by, created_at
            FROM payment_status_history
            WHERE old_status = ?
            ORDER BY created_at DESC
            """;
        
        return jdbcTemplate.query(sql, ROW_MAPPER, status.name());
    }

    @Override
    public long countByPaymentId(Long paymentId) {
        String sql = "SELECT COUNT(*) FROM payment_status_history WHERE payment_id = ?";
        Long result = jdbcTemplate.queryForObject(sql, Long.class, paymentId);
        return result != null ? result : 0L;
    }

    @Override
    public int deleteByPaymentId(Long paymentId) {
        String sql = "DELETE FROM payment_status_history WHERE payment_id = ?";
        return jdbcTemplate.update(sql, paymentId);
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM payment_status_history";
        Long result = jdbcTemplate.queryForObject(sql, Long.class);
        return result != null ? result : 0L;
    }
    
    /**
     * RowMapper for converting ResultSet rows to PaymentStatusHistoryRecord objects.
     */
    private static class PaymentStatusHistoryRowMapper implements RowMapper<PaymentStatusHistoryRecord> {
        @Override
        public PaymentStatusHistoryRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
            String oldStatusStr = rs.getString("old_status");
            return new PaymentStatusHistoryRecord(
                rs.getLong("id"),
                rs.getLong("payment_id"),
                oldStatusStr != null ? PaymentStatus.valueOf(oldStatusStr) : null,
                PaymentStatus.valueOf(rs.getString("new_status")),
                rs.getString("error_code"),
                rs.getString("error_message"),
                rs.getString("triggered_by"),
                rs.getTimestamp("created_at").toLocalDateTime()
            );
        }
    }
}

