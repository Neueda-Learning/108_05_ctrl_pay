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

import com.neueda.domain.AccountRecord;
import com.neueda.domain.AccountStatus;
import com.neueda.repository.AccountRepository;

/**
 * JDBC implementation of AccountRepository.
 */
@Repository
public class AccountRepositoryImpl implements AccountRepository {

    private static final AccountRowMapper ROW_MAPPER = new AccountRowMapper();
    private final JdbcTemplate jdbcTemplate;

    public AccountRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public AccountRecord save(AccountRecord account) {
        String sql = """
            INSERT INTO accounts (customer_id, account_name, account_balance, account_status, currency, account_opening_date, last_updated, ifsc_code, account_location, bank_name, account_pin_hash)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            var ps = connection.prepareStatement(sql, new String[]{"account_id"});
            ps.setLong(1, account.customerId());
            ps.setString(2, account.accountName());
            ps.setBigDecimal(3, account.accountBalance());
            ps.setString(4, account.accountStatus().name());
            ps.setString(5, account.currency());
            ps.setObject(6, account.accountOpeningDate());
            ps.setObject(7, account.lastUpdated());
            ps.setString(8, account.ifscCode());
            ps.setString(9, account.accountLocation());
            ps.setString(10, account.bankName());
            ps.setString(11, account.accountPin());
            return ps;
        }, keyHolder);

        Long generatedId = Objects.requireNonNull(keyHolder.getKey(), "Generated account ID is missing").longValue();
        return new AccountRecord(
            generatedId,
            account.customerId(),
            account.accountName(),
            account.accountBalance(),
            account.accountStatus(),
            account.currency(),
            account.accountOpeningDate(),
            account.lastUpdated(),
            account.ifscCode(),
            account.accountLocation(),
            account.bankName(),
            account.accountPin()
        );
    }

    @Override
    public Optional<AccountRecord> findById(Long accountId) {
        String sql = """
            SELECT account_id, customer_id, account_name, account_balance, account_status, currency, account_opening_date,
                   last_updated, ifsc_code, account_location, bank_name, account_pin_hash
            FROM accounts
            WHERE account_id = ?
            """;
        List<AccountRecord> results = jdbcTemplate.query(sql, ROW_MAPPER, accountId);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public List<AccountRecord> findByCustomerId(Long customerId) {
        String sql = """
            SELECT account_id, customer_id, account_name, account_balance, account_status, currency, account_opening_date,
                   last_updated, ifsc_code, account_location, bank_name, account_pin_hash
            FROM accounts
            WHERE customer_id = ?
            ORDER BY account_id DESC
            """;
        return jdbcTemplate.query(sql, ROW_MAPPER, customerId);
    }

    private static class AccountRowMapper implements RowMapper<AccountRecord> {
        @Override
        public AccountRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
            LocalDate openingDate = rs.getDate("account_opening_date").toLocalDate();
            return new AccountRecord(
                rs.getLong("account_id"),
                rs.getLong("customer_id"),
                rs.getString("account_name"),
                rs.getBigDecimal("account_balance"),
                AccountStatus.valueOf(rs.getString("account_status")),
                rs.getString("currency"),
                openingDate,
                rs.getTimestamp("last_updated").toLocalDateTime(),
                rs.getString("ifsc_code"),
                rs.getString("account_location"),
                rs.getString("bank_name"),
                rs.getString("account_pin_hash")
            );
        }
    }
}


