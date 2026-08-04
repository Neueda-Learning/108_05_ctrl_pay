package com.neueda.repository.impl;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import com.neueda.domain.CustomerRecord;
import com.neueda.domain.CustomerStatus;
import com.neueda.repository.CustomerRepository;

/**
 * JDBC implementation of CustomerRepository.
 */
@Repository
public class CustomerRepositoryImpl implements CustomerRepository {

    private static final CustomerRowMapper ROW_MAPPER = new CustomerRowMapper();
    private final JdbcTemplate jdbcTemplate;

    public CustomerRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public CustomerRecord save(CustomerRecord customer) {
        String sql = """
            INSERT INTO customers (name, dob, phone_number, pan_number, profile_created, last_updated, country, customer_account_status)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var ps = connection.prepareStatement(sql, new String[]{"customer_id"});
            ps.setString(1, customer.name());
            ps.setObject(2, customer.dob());
            ps.setString(3, customer.phoneNumber());
            ps.setString(4, customer.panNumber());
            ps.setObject(5, customer.profileCreated());
            ps.setObject(6, customer.lastUpdated());
            ps.setString(7, customer.country());
            ps.setString(8, customer.customerAccountStatus().name());
            return ps;
        }, keyHolder);

        Long generatedId = Objects.requireNonNull(keyHolder.getKey(), "Generated customer ID is missing").longValue();
        return new CustomerRecord(
            generatedId,
            customer.name(),
            customer.dob(),
            customer.phoneNumber(),
            customer.panNumber(),
            customer.profileCreated(),
            customer.lastUpdated(),
            customer.country(),
            customer.customerAccountStatus()
        );
    }

    @Override
    public Optional<CustomerRecord> findById(Long customerId) {
        String sql = """
            SELECT customer_id, name, dob, phone_number, pan_number, profile_created, last_updated, country, customer_account_status
            FROM customers
            WHERE customer_id = ?
            """;
        List<CustomerRecord> results = jdbcTemplate.query(sql, ROW_MAPPER, customerId);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public Optional<CustomerRecord> findByPanNumber(String panNumber) {
        String sql = """
            SELECT customer_id, name, dob, phone_number, pan_number, profile_created, last_updated, country, customer_account_status
            FROM customers
            WHERE pan_number = ?
            """;
        List<CustomerRecord> results = jdbcTemplate.query(sql, ROW_MAPPER, panNumber);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public boolean existsByPanNumber(String panNumber) {
        String sql = "SELECT COUNT(*) FROM customers WHERE pan_number = ?";
        Long count = jdbcTemplate.queryForObject(sql, Long.class, panNumber);
        return count != null && count > 0;
    }

    private static class CustomerRowMapper implements RowMapper<CustomerRecord> {
        @Override
        public CustomerRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
            LocalDate dob = rs.getDate("dob").toLocalDate();
            return new CustomerRecord(
                rs.getLong("customer_id"),
                rs.getString("name"),
                dob,
                rs.getString("phone_number"),
                rs.getString("pan_number"),
                rs.getTimestamp("profile_created").toLocalDateTime(),
                rs.getTimestamp("last_updated").toLocalDateTime(),
                rs.getString("country"),
                CustomerStatus.valueOf(rs.getString("customer_account_status"))
            );
        }
    }
}


