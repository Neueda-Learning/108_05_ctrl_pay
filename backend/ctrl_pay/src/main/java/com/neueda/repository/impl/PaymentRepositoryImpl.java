package com.neueda.repository.impl;

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

import com.neueda.domain.PaymentRecord;
import com.neueda.domain.PaymentStatus;
import com.neueda.repository.PaymentRepository;

/**
 * JDBC-based implementation of PaymentRepository using Spring's JdbcTemplate.
 * All SQL queries implemented with prepared statements for security.
 */
@Repository
public class PaymentRepositoryImpl implements PaymentRepository {
    
    private final JdbcTemplate jdbcTemplate;
    private static final PaymentRowMapper ROW_MAPPER = new PaymentRowMapper();
    
    public PaymentRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public PaymentRecord save(PaymentRecord payment) {
        String sql = """
            INSERT INTO payments (idempotency_key, source_account, destination_account, amount, currency, status, error_code, error_message, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var ps = connection.prepareStatement(sql, new String[]{"id"});
            ps.setString(1, payment.idempotencyKey());
            ps.setString(2, payment.sourceAccount());
            ps.setString(3, payment.destinationAccount());
            ps.setBigDecimal(4, payment.amount());
            ps.setString(5, payment.currency());
            ps.setString(6, payment.status().name());
            ps.setString(7, payment.errorCode());
            ps.setString(8, payment.errorMessage());
            ps.setObject(9, payment.createdAt());
            ps.setObject(10, payment.updatedAt());
            return ps;
        }, keyHolder);
        
        Long generatedId = keyHolder.getKey().longValue();
        return new PaymentRecord(
            generatedId,
            payment.idempotencyKey(),
            payment.sourceAccount(),
            payment.destinationAccount(),
            payment.amount(),
            payment.currency(),
            payment.status(),
            payment.errorCode(),
            payment.errorMessage(),
            payment.createdAt(),
            payment.updatedAt()
        );
    }

    @Override
    public PaymentRecord update(PaymentRecord payment) {
        String sql = """
            UPDATE payments SET idempotency_key=?, source_account=?, destination_account=?, amount=?, currency=?, status=?, error_code=?, error_message=?, updated_at=? WHERE id=?
            """;
        
        jdbcTemplate.update(sql,
            payment.idempotencyKey(),
            payment.sourceAccount(),
            payment.destinationAccount(),
            payment.amount(),
            payment.currency(),
            payment.status().name(),
            payment.errorCode(),
            payment.errorMessage(),
            payment.updatedAt(),
            payment.id()
        );
        
        return payment;
    }

    @Override
    public Optional<PaymentRecord> findById(Long id) {
        String sql = """
            SELECT id, idempotency_key, source_account, destination_account, amount, currency, status, error_code, error_message, created_at, updated_at
            FROM payments
            WHERE id = ?
            """;
        
        List<PaymentRecord> results = jdbcTemplate.query(sql, ROW_MAPPER, id);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public Optional<PaymentRecord> findByIdempotencyKey(String idempotencyKey) {
        String sql = """
            SELECT id, idempotency_key, source_account, destination_account, amount, currency, status, error_code, error_message, created_at, updated_at
            FROM payments
            WHERE idempotency_key = ?
            """;
        
        List<PaymentRecord> results = jdbcTemplate.query(sql, ROW_MAPPER, idempotencyKey);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public List<PaymentRecord> findAll(PaymentStatus status, int limit, int offset) {
        String sql = """
            SELECT id, idempotency_key, source_account, destination_account, amount, currency, status, error_code, error_message, created_at, updated_at
            FROM payments
            WHERE status = ? OR ? IS NULL
            ORDER BY created_at DESC
            LIMIT ? OFFSET ?
            """;
        
        return jdbcTemplate.query(sql, ROW_MAPPER,
            status != null ? status.name() : null,
            status != null ? null : 1,
            limit,
            offset
        );
    }

    @Override
    public List<PaymentRecord> findAll() {
        String sql = """
            SELECT id, idempotency_key, source_account, destination_account, amount, currency, status, error_code, error_message, created_at, updated_at
            FROM payments
            ORDER BY created_at DESC
            """;
        
        return jdbcTemplate.query(sql, ROW_MAPPER);
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM payments";
        Long result = jdbcTemplate.queryForObject(sql, Long.class);
        return result != null ? result : 0L;
    }

    @Override
    public long countByStatus(PaymentStatus status) {
        String sql = "SELECT COUNT(*) FROM payments WHERE status = ?";
        Long result = jdbcTemplate.queryForObject(sql, Long.class, status.name());
        return result != null ? result : 0L;
    }

    @Override
    public boolean deleteById(Long id) {
        String sql = "DELETE FROM payments WHERE id = ?";
        int rowsAffected = jdbcTemplate.update(sql, id);
        return rowsAffected > 0;
    }
    
    /**
     * RowMapper for converting ResultSet rows to PaymentRecord objects.
     */
    private static class PaymentRowMapper implements RowMapper<PaymentRecord> {
        @Override
        public PaymentRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new PaymentRecord(
                rs.getLong("id"),
                rs.getString("idempotency_key"),
                rs.getString("source_account"),
                rs.getString("destination_account"),
                rs.getBigDecimal("amount"),
                rs.getString("currency"),
                PaymentStatus.valueOf(rs.getString("status")),
                rs.getString("error_code"),
                rs.getString("error_message"),
                rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getTimestamp("updated_at").toLocalDateTime()
            );
        }
    }
}

