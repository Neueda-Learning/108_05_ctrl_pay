package com.neueda.repository.impl;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
            INSERT INTO payments (idempotency_key, source_account, destination_account, amount, currency, 
                                  source_amount, destination_amount, exchange_rate,
                                  status, error_code, error_message, 
                                  settlement_attempt_count, max_settlement_attempts, 
                                  last_settlement_attempt_time, next_settlement_retry_time, settled_at,
                                  created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var ps = connection.prepareStatement(sql, new String[]{"id"});
            ps.setString(1, payment.idempotencyKey());
            ps.setString(2, payment.sourceAccount());
            ps.setString(3, payment.destinationAccount());
            ps.setBigDecimal(4, payment.amount());
            ps.setString(5, payment.currency());
            ps.setBigDecimal(6, payment.sourceAmount());
            ps.setBigDecimal(7, payment.destinationAmount());
            ps.setBigDecimal(8, payment.exchangeRate());
            ps.setString(9, payment.status().name());
            ps.setString(10, payment.errorCode());
            ps.setString(11, payment.errorMessage());
            ps.setInt(12, payment.settlementAttemptCount());
            ps.setInt(13, payment.maxSettlementAttempts());
            ps.setObject(14, payment.lastSettlementAttemptTime());
            ps.setObject(15, payment.nextSettlementRetryTime());
            ps.setObject(16, payment.settledAt());
            ps.setObject(17, payment.createdAt());
            ps.setObject(18, payment.updatedAt());
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
            null, // sourceAmount - set during settlement
            null, // destinationAmount - set during settlement
            null, // exchangeRate - set during settlement
            payment.status(),
            payment.errorCode(),
            payment.errorMessage(),
            0, // settlementAttemptCount
            3, // maxSettlementAttempts
            null, // lastSettlementAttemptTime
            null, // nextSettlementRetryTime
            null, // settledAt
            payment.createdAt(),
            payment.updatedAt()
        );
    }

    @Override
    public PaymentRecord update(PaymentRecord payment) {
        String sql = """
            UPDATE payments SET 
                idempotency_key=?, source_account=?, destination_account=?, amount=?, currency=?, 
                source_amount=?, destination_amount=?, exchange_rate=?,
                status=?, error_code=?, error_message=?, 
                settlement_attempt_count=?, max_settlement_attempts=?, 
                last_settlement_attempt_time=?, next_settlement_retry_time=?, settled_at=?,
                updated_at=? 
            WHERE id=?
            """;
        
        jdbcTemplate.update(sql,
            payment.idempotencyKey(),
            payment.sourceAccount(),
            payment.destinationAccount(),
            payment.amount(),
            payment.currency(),
            payment.sourceAmount(),
            payment.destinationAmount(),
            payment.exchangeRate(),
            payment.status().name(),
            payment.errorCode(),
            payment.errorMessage(),
            payment.settlementAttemptCount(),
            payment.maxSettlementAttempts(),
            payment.lastSettlementAttemptTime(),
            payment.nextSettlementRetryTime(),
            payment.settledAt(),
            payment.updatedAt(),
            payment.id()
        );
        
        return payment;
    }

    @Override
    public Optional<PaymentRecord> findById(Long id) {
        String sql = """
            SELECT id, idempotency_key, source_account, destination_account, amount, currency, 
                   source_amount, destination_amount, exchange_rate,
                   status, error_code, error_message, 
                   settlement_attempt_count, max_settlement_attempts, last_settlement_attempt_time, next_settlement_retry_time, settled_at,
                   created_at, updated_at
            FROM payments
            WHERE id = ?
            """;
        
        List<PaymentRecord> results = jdbcTemplate.query(sql, ROW_MAPPER, id);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public Optional<PaymentRecord> findByIdempotencyKey(String idempotencyKey) {
        String sql = """
            SELECT id, idempotency_key, source_account, destination_account, amount, currency, 
                   source_amount, destination_amount, exchange_rate,
                   status, error_code, error_message, 
                   settlement_attempt_count, max_settlement_attempts, last_settlement_attempt_time, next_settlement_retry_time, settled_at,
                   created_at, updated_at
            FROM payments
            WHERE idempotency_key = ?
            """;
        
        List<PaymentRecord> results = jdbcTemplate.query(sql, ROW_MAPPER, idempotencyKey);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public List<PaymentRecord> findAll(PaymentStatus status, int limit, int offset) {
        // Build dynamic SQL based on whether status filter is provided
        if (status != null) {
            // Status filter provided: only query by specific status
            String sql = """
                SELECT id, idempotency_key, source_account, destination_account, amount, currency, 
                       source_amount, destination_amount, exchange_rate,
                       status, error_code, error_message, 
                       settlement_attempt_count, max_settlement_attempts, last_settlement_attempt_time, next_settlement_retry_time, settled_at,
                       created_at, updated_at
                FROM payments
                WHERE status = ?
                ORDER BY created_at DESC
                LIMIT ? OFFSET ?
                """;
            
            return jdbcTemplate.query(sql, ROW_MAPPER,
                status.name(),
                limit,
                offset
            );
        } else {
            // No status filter: return all payments
            String sql = """
                SELECT id, idempotency_key, source_account, destination_account, amount, currency, 
                       source_amount, destination_amount, exchange_rate,
                       status, error_code, error_message, 
                       settlement_attempt_count, max_settlement_attempts, last_settlement_attempt_time, next_settlement_retry_time, settled_at,
                       created_at, updated_at
                FROM payments
                ORDER BY created_at DESC
                LIMIT ? OFFSET ?
                """;
            
            return jdbcTemplate.query(sql, ROW_MAPPER,
                limit,
                offset
            );
        }
    }

    @Override
    public List<PaymentRecord> findAllFiltered(
        PaymentStatus status,
        String account,
        String currency,
        LocalDateTime dateFrom,
        LocalDateTime dateTo,
        Long failedRuleId,
        int limit,
        int offset
    ) {
        // Build dynamic SQL query with filters
        StringBuilder sql = new StringBuilder(
            "SELECT DISTINCT p.id, p.idempotency_key, p.source_account, p.destination_account, p.amount, p.currency, " +
            "p.source_amount, p.destination_amount, p.exchange_rate, " +
            "p.status, p.error_code, p.error_message, " +
            "p.settlement_attempt_count, p.max_settlement_attempts, p.last_settlement_attempt_time, p.next_settlement_retry_time, p.settled_at, " +
            "p.created_at, p.updated_at "
        );
        sql.append("FROM payments p ");
        
        // Join validation_results if filtering by failed rule
        if (failedRuleId != null) {
            sql.append("JOIN validation_results vr ON p.id = vr.payment_id ");
        }
        
        // Build WHERE clause
        List<Object> params = new ArrayList<>();
        sql.append("WHERE 1=1 ");
        
        // Status filter
        if (status != null) {
            sql.append("AND p.status = ? ");
            params.add(status.name());
        }
        
        // Account filter (source or destination)
        if (account != null) {
            sql.append("AND (p.source_account = ? OR p.destination_account = ?) ");
            params.add(account);
            params.add(account);
        }
        
        // Currency filter
        if (currency != null) {
            sql.append("AND p.currency = ? ");
            params.add(currency);
        }
        
        // Date range filters
        if (dateFrom != null) {
            sql.append("AND p.created_at >= ? ");
            params.add(Timestamp.valueOf(dateFrom));
        }
        if (dateTo != null) {
            sql.append("AND p.created_at <= ? ");
            params.add(Timestamp.valueOf(dateTo));
        }
        
        // Failed rule filter
        if (failedRuleId != null) {
            sql.append("AND vr.validation_rule_id = ? AND vr.passed = false ");
            params.add(failedRuleId);
        }
        
        // Order and pagination
        sql.append("ORDER BY p.created_at DESC ");
        sql.append("LIMIT ? OFFSET ? ");
        params.add(limit);
        params.add(offset);
        
        // Execute query
        return jdbcTemplate.query(sql.toString(), ROW_MAPPER, params.toArray());
    }

    @Override
    public List<PaymentRecord> findCompletedByAccountSince(String accountNumber, LocalDateTime since) {
        String sql = """
            SELECT id, idempotency_key, source_account, destination_account, amount, currency,
                   source_amount, destination_amount, exchange_rate,
                   status, error_code, error_message,
                   settlement_attempt_count, max_settlement_attempts, last_settlement_attempt_time, next_settlement_retry_time, settled_at,
                   created_at, updated_at
            FROM payments
            WHERE status = 'COMPLETED'
              AND (source_account = ? OR destination_account = ?)
              AND created_at >= ?
            ORDER BY created_at DESC
            """;

        return jdbcTemplate.query(
            sql,
            ROW_MAPPER,
            accountNumber,
            accountNumber,
            Timestamp.valueOf(since)
        );
    }

    @Override
    public List<PaymentRecord> findAll() {
        String sql = """
            SELECT id, idempotency_key, source_account, destination_account, amount, currency, 
                   source_amount, destination_amount, exchange_rate,
                   status, error_code, error_message, 
                   settlement_attempt_count, max_settlement_attempts, last_settlement_attempt_time, next_settlement_retry_time, settled_at,
                   created_at, updated_at
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
            // Handle optional settlement columns that might be NULL
            Timestamp lastAttemptTs = rs.getTimestamp("last_settlement_attempt_time");
            Timestamp nextRetryTs = rs.getTimestamp("next_settlement_retry_time");
            Timestamp settledAtTs = rs.getTimestamp("settled_at");
            
            return new PaymentRecord(
                rs.getLong("id"),
                rs.getString("idempotency_key"),
                rs.getString("source_account"),
                rs.getString("destination_account"),
                rs.getBigDecimal("amount"),
                rs.getString("currency"),
                rs.getBigDecimal("source_amount"),
                rs.getBigDecimal("destination_amount"),
                rs.getBigDecimal("exchange_rate"),
                PaymentStatus.valueOf(rs.getString("status")),
                rs.getString("error_code"),
                rs.getString("error_message"),
                rs.getInt("settlement_attempt_count"),
                rs.getInt("max_settlement_attempts"),
                lastAttemptTs != null ? lastAttemptTs.toLocalDateTime() : null,
                nextRetryTs != null ? nextRetryTs.toLocalDateTime() : null,
                settledAtTs != null ? settledAtTs.toLocalDateTime() : null,
                rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getTimestamp("updated_at").toLocalDateTime()
            );
        }
    }
}

