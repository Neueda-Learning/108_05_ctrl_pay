package com.neueda.repository.impl;

import com.neueda.domain.BulkPaymentBatchRecord;
import com.neueda.domain.BulkPaymentBatchStatus;
import com.neueda.repository.BulkPaymentBatchRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * JDBC-based implementation of BulkPaymentBatchRepository.
 * All SQL queries use prepared statements for security.
 */
@Repository
public class BulkPaymentBatchRepositoryImpl implements BulkPaymentBatchRepository {
    
    private final JdbcTemplate jdbcTemplate;
    
    public BulkPaymentBatchRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    /**
     * RowMapper for converting ResultSet rows to BulkPaymentBatchRecord objects.
     */
    private static class BulkPaymentBatchRowMapper implements RowMapper<BulkPaymentBatchRecord> {
        @Override
        public BulkPaymentBatchRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
            java.sql.Timestamp validationStartedTs = rs.getTimestamp("validation_started_at");
            java.sql.Timestamp validationCompletedTs = rs.getTimestamp("validation_completed_at");
            java.sql.Timestamp processingStartedTs = rs.getTimestamp("processing_started_at");
            java.sql.Timestamp processingCompletedTs = rs.getTimestamp("processing_completed_at");
            java.sql.Timestamp completedTs = rs.getTimestamp("completed_at");
            
            return new BulkPaymentBatchRecord(
                rs.getLong("id"),
                rs.getString("batch_reference"),
                rs.getString("idempotency_key"),
                rs.getString("source_account"),
                rs.getInt("total_transactions"),
                rs.getInt("successful_transactions"),
                rs.getInt("failed_transactions"),
                rs.getBigDecimal("total_amount"),
                BulkPaymentBatchStatus.valueOf(rs.getString("status")),
                rs.getString("created_by"),
                rs.getTimestamp("created_at").toLocalDateTime(),
                validationStartedTs != null ? validationStartedTs.toLocalDateTime() : null,
                validationCompletedTs != null ? validationCompletedTs.toLocalDateTime() : null,
                processingStartedTs != null ? processingStartedTs.toLocalDateTime() : null,
                processingCompletedTs != null ? processingCompletedTs.toLocalDateTime() : null,
                completedTs != null ? completedTs.toLocalDateTime() : null,
                rs.getString("last_error_message")
            );
        }
    }
    
    @Override
    public BulkPaymentBatchRecord create(BulkPaymentBatchRecord batch) {
        String sql = "INSERT INTO bulk_payment_batches (" +
            "batch_reference, idempotency_key, source_account, total_transactions, " +
            "successful_transactions, failed_transactions, total_amount, status, created_by, created_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        jdbcTemplate.update(sql,
            batch.batchReference(),
            batch.idempotencyKey(),
            batch.sourceAccount(),
            batch.totalTransactions(),
            batch.successfulTransactions(),
            batch.failedTransactions(),
            batch.totalAmount(),
            batch.status().toString(),
            batch.createdBy(),
            batch.createdAt()
        );
        
        // Retrieve generated ID
        String idSql = "SELECT id FROM bulk_payment_batches WHERE batch_reference = ?";
        Long id = jdbcTemplate.queryForObject(idSql, Long.class, batch.batchReference());
        
        return new BulkPaymentBatchRecord(
            id,
            batch.batchReference(),
            batch.idempotencyKey(),
            batch.sourceAccount(),
            batch.totalTransactions(),
            batch.successfulTransactions(),
            batch.failedTransactions(),
            batch.totalAmount(),
            batch.status(),
            batch.createdBy(),
            batch.createdAt(),
            batch.validationStartedAt(),
            batch.validationCompletedAt(),
            batch.processingStartedAt(),
            batch.processingCompletedAt(),
            batch.completedAt(),
            batch.lastErrorMessage()
        );
    }
    
    @Override
    public Optional<BulkPaymentBatchRecord> findById(Long batchId) {
        String sql = "SELECT * FROM bulk_payment_batches WHERE id = ?";
        List<BulkPaymentBatchRecord> results = jdbcTemplate.query(
            sql,
            new BulkPaymentBatchRowMapper(),
            batchId
        );
        return results.stream().findFirst();
    }
    
    @Override
    public Optional<BulkPaymentBatchRecord> findByReference(String batchReference) {
        String sql = "SELECT * FROM bulk_payment_batches WHERE batch_reference = ?";
        List<BulkPaymentBatchRecord> results = jdbcTemplate.query(
            sql,
            new BulkPaymentBatchRowMapper(),
            batchReference
        );
        return results.stream().findFirst();
    }
    
    @Override
    public Optional<BulkPaymentBatchRecord> findByIdempotencyKey(String idempotencyKey) {
        String sql = "SELECT * FROM bulk_payment_batches WHERE idempotency_key = ?";
        List<BulkPaymentBatchRecord> results = jdbcTemplate.query(
            sql,
            new BulkPaymentBatchRowMapper(),
            idempotencyKey
        );
        return results.stream().findFirst();
    }
    
    @Override
    public void update(BulkPaymentBatchRecord batch) {
        String sql = "UPDATE bulk_payment_batches SET " +
            "status = ?, total_transactions = ?, successful_transactions = ?, failed_transactions = ?, " +
            "validation_started_at = ?, validation_completed_at = ?, " +
            "processing_started_at = ?, processing_completed_at = ?, completed_at = ?, last_error_message = ? " +
            "WHERE id = ?";
        
        jdbcTemplate.update(sql,
            batch.status().toString(),
            batch.totalTransactions(),
            batch.successfulTransactions(),
            batch.failedTransactions(),
            batch.validationStartedAt(),
            batch.validationCompletedAt(),
            batch.processingStartedAt(),
            batch.processingCompletedAt(),
            batch.completedAt(),
            batch.lastErrorMessage(),
            batch.id()
        );
    }
    
    @Override
    public List<BulkPaymentBatchRecord> findByCreatedBy(String userId, int limit, int offset) {
        String sql = "SELECT * FROM bulk_payment_batches WHERE created_by = ? " +
            "ORDER BY created_at DESC LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql, new BulkPaymentBatchRowMapper(), userId, limit, offset);
    }
    
    @Override
    public List<BulkPaymentBatchRecord> findByStatus(BulkPaymentBatchStatus status, int limit, int offset) {
        String sql = "SELECT * FROM bulk_payment_batches WHERE status = ? " +
            "ORDER BY created_at DESC LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql, new BulkPaymentBatchRowMapper(), status.toString(), limit, offset);
    }
    
    @Override
    public List<BulkPaymentBatchRecord> findBySourceAccount(String sourceAccount, int limit, int offset) {
        String sql = "SELECT * FROM bulk_payment_batches WHERE source_account = ? " +
            "ORDER BY created_at DESC LIMIT ? OFFSET ?";
        return jdbcTemplate.query(sql, new BulkPaymentBatchRowMapper(), sourceAccount, limit, offset);
    }
    
    @Override
    public List<BulkPaymentBatchRecord> findByStatusForProcessing(BulkPaymentBatchStatus status) {
        String sql = "SELECT * FROM bulk_payment_batches WHERE status = ? ORDER BY created_at ASC";
        return jdbcTemplate.query(sql, new BulkPaymentBatchRowMapper(), status.toString());
    }
    
    @Override
    public int countByStatus(BulkPaymentBatchStatus status) {
        String sql = "SELECT COUNT(*) FROM bulk_payment_batches WHERE status = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, status.toString());
        return count != null ? count : 0;
    }
}

