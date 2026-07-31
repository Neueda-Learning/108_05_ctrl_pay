package com.neueda.repository.impl;

import java.util.List;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.neueda.domain.PaymentRecord;
import com.neueda.domain.PaymentStatus;
import com.neueda.repository.PaymentRepository;

/**
 * JDBC-based implementation of PaymentRepository using Spring's JdbcTemplate.
 * SQL queries will be implemented in Phase 2.
 */
@Repository
public class PaymentRepositoryImpl implements PaymentRepository {
    
    private final JdbcTemplate jdbcTemplate;
    
    public PaymentRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public PaymentRecord save(PaymentRecord payment) {
        // TODO: Implement INSERT query in Phase 2
        // SQL: INSERT INTO payments (idempotency_key, source_account, ...) VALUES (?, ?, ...)
        // Should return generated ID
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public PaymentRecord update(PaymentRecord payment) {
        // TODO: Implement UPDATE query in Phase 2
        // SQL: UPDATE payments SET status=?, updated_at=? WHERE id=?
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public Optional<PaymentRecord> findById(Long id) {
        // TODO: Implement SELECT by ID query in Phase 2
        // SQL: SELECT * FROM payments WHERE id=?
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public Optional<PaymentRecord> findByIdempotencyKey(String idempotencyKey) {
        // TODO: Implement SELECT by idempotency key query in Phase 2
        // SQL: SELECT * FROM payments WHERE idempotency_key=?
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public List<PaymentRecord> findAll(PaymentStatus status, int limit, int offset) {
        // TODO: Implement SELECT with optional status filter in Phase 2
        // SQL: SELECT * FROM payments WHERE status=? OR ? IS NULL ORDER BY created_at DESC LIMIT ? OFFSET ?
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public List<PaymentRecord> findAll() {
        // TODO: Implement SELECT all query in Phase 2
        // SQL: SELECT * FROM payments ORDER BY created_at DESC
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public long count() {
        // TODO: Implement COUNT query in Phase 2
        // SQL: SELECT COUNT(*) FROM payments
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public long countByStatus(PaymentStatus status) {
        // TODO: Implement COUNT with status filter in Phase 2
        // SQL: SELECT COUNT(*) FROM payments WHERE status=?
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public boolean deleteById(Long id) {
        // TODO: Implement DELETE query in Phase 2
        // SQL: DELETE FROM payments WHERE id=?
        throw new UnsupportedOperationException("Not yet implemented");
    }
}

