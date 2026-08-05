package com.neueda.repository.impl;

import com.neueda.domain.BulkPaymentItemRecord;
import com.neueda.domain.BulkPaymentItemStatus;
import com.neueda.repository.BulkPaymentItemRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * JDBC-based implementation of BulkPaymentItemRepository.
 * All SQL queries use prepared statements for security.
 */
@Repository
public class BulkPaymentItemRepositoryImpl implements BulkPaymentItemRepository {
    
    private final JdbcTemplate jdbcTemplate;
    
    public BulkPaymentItemRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    /**
     * RowMapper for converting ResultSet rows to BulkPaymentItemRecord objects.
     */
    private static class BulkPaymentItemRowMapper implements RowMapper<BulkPaymentItemRecord> {
        @Override
        public BulkPaymentItemRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
            java.sql.Timestamp validatedTs = rs.getTimestamp("validated_at");
            java.sql.Timestamp processingStartedTs = rs.getTimestamp("processing_started_at");
            java.sql.Timestamp completedTs = rs.getTimestamp("completed_at");
            
            return new BulkPaymentItemRecord(
                rs.getLong("id"),
                rs.getLong("batch_id"),
                rs.getLong("payment_id") != 0 ? rs.getLong("payment_id") : null,
                rs.getInt("line_number"),
                rs.getString("destination_account"),
                rs.getBigDecimal("amount"),
                rs.getString("currency"),
                rs.getString("description"),
                BulkPaymentItemStatus.valueOf(rs.getString("status")),
                rs.getString("error_code"),
                rs.getString("failure_reason"),
                rs.getBigDecimal("fraud_score"),
                rs.getString("fraud_decision"),
                rs.getString("validation_errors"),
                rs.getString("rollback_status"),
                rs.getTimestamp("created_at").toLocalDateTime(),
                validatedTs != null ? validatedTs.toLocalDateTime() : null,
                processingStartedTs != null ? processingStartedTs.toLocalDateTime() : null,
                completedTs != null ? completedTs.toLocalDateTime() : null
            );
        }
    }
    
    @Override
    public BulkPaymentItemRecord create(BulkPaymentItemRecord item) {
        String sql = "INSERT INTO bulk_payment_items (" +
            "batch_id, payment_id, line_number, destination_account, amount, currency, description, status, created_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        jdbcTemplate.update(sql,
            item.batchId(),
            item.paymentId(),
            item.lineNumber(),
            item.destinationAccount(),
            item.amount(),
            item.currency(),
            item.description(),
            item.status().toString(),
            item.createdAt()
        );
        
        // Retrieve generated ID
        String idSql = "SELECT id FROM bulk_payment_items WHERE batch_id = ? AND line_number = ?";
        Long id = jdbcTemplate.queryForObject(idSql, Long.class, item.batchId(), item.lineNumber());
        
        return new BulkPaymentItemRecord(
            id,
            item.batchId(),
            item.paymentId(),
            item.lineNumber(),
            item.destinationAccount(),
            item.amount(),
            item.currency(),
            item.description(),
            item.status(),
            item.errorCode(),
            item.failureReason(),
            item.fraudScore(),
            item.fraudDecision(),
            item.validationErrors(),
            item.rollbackStatus(),
            item.createdAt(),
            item.validatedAt(),
            item.processingStartedAt(),
            item.completedAt()
        );
    }
    
    @Override
    public void createBatch(List<BulkPaymentItemRecord> items) {
        String sql = "INSERT INTO bulk_payment_items (" +
            "batch_id, payment_id, line_number, destination_account, amount, currency, description, status, created_at) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        for (BulkPaymentItemRecord item : items) {
            jdbcTemplate.update(sql,
                item.batchId(),
                item.paymentId(),
                item.lineNumber(),
                item.destinationAccount(),
                item.amount(),
                item.currency(),
                item.description(),
                item.status().toString(),
                item.createdAt()
            );
        }
    }
    
    @Override
    public Optional<BulkPaymentItemRecord> findById(Long itemId) {
        String sql = "SELECT * FROM bulk_payment_items WHERE id = ?";
        List<BulkPaymentItemRecord> results = jdbcTemplate.query(
            sql,
            new BulkPaymentItemRowMapper(),
            itemId
        );
        return results.stream().findFirst();
    }
    
    @Override
    public List<BulkPaymentItemRecord> findByBatchId(Long batchId) {
        String sql = "SELECT * FROM bulk_payment_items WHERE batch_id = ? ORDER BY line_number ASC";
        return jdbcTemplate.query(sql, new BulkPaymentItemRowMapper(), batchId);
    }
    
    @Override
    public List<BulkPaymentItemRecord> findByBatchIdAndStatus(Long batchId, BulkPaymentItemStatus status) {
        String sql = "SELECT * FROM bulk_payment_items WHERE batch_id = ? AND status = ? ORDER BY line_number ASC";
        return jdbcTemplate.query(sql, new BulkPaymentItemRowMapper(), batchId, status.toString());
    }
    
    @Override
    public void update(BulkPaymentItemRecord item) {
        String sql = "UPDATE bulk_payment_items SET " +
            "payment_id = ?, status = ?, error_code = ?, failure_reason = ?, " +
            "fraud_score = ?, fraud_decision = ?, validation_errors = ?, rollback_status = ?, " +
            "validated_at = ?, processing_started_at = ?, completed_at = ? " +
            "WHERE id = ?";
        
        jdbcTemplate.update(sql,
            item.paymentId(),
            item.status().toString(),
            item.errorCode(),
            item.failureReason(),
            item.fraudScore(),
            item.fraudDecision(),
            item.validationErrors(),
            item.rollbackStatus(),
            item.validatedAt(),
            item.processingStartedAt(),
            item.completedAt(),
            item.id()
        );
    }
    
    @Override
    public int countByBatchIdAndStatus(Long batchId, BulkPaymentItemStatus status) {
        String sql = "SELECT COUNT(*) FROM bulk_payment_items WHERE batch_id = ? AND status = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, batchId, status.toString());
        return count != null ? count : 0;
    }
    
    @Override
    public void delete(Long itemId) {
        String sql = "DELETE FROM bulk_payment_items WHERE id = ?";
        jdbcTemplate.update(sql, itemId);
    }
}

