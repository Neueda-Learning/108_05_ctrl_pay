package com.neueda.repository;

import java.util.List;
import java.util.Optional;

import com.neueda.domain.AccountRecord;

/**
 * Repository interface for account data access operations.
 */
public interface AccountRepository {

    /**
     * Insert a new account record.
     * Used only when creating new accounts.
     * @param account new account to insert
     * @return account record with generated ID
     */
    AccountRecord save(AccountRecord account);

    /**
     * Update an existing account record.
     * Used for modifying existing accounts (e.g., balance updates during settlement).
     * @param account account to update with new values
     * @return updated account record
     */
    AccountRecord update(AccountRecord account);

    /**
     * Retrieve account by ID.
     */
    Optional<AccountRecord> findById(Long accountId);

    /**
     * Retrieve all accounts for a specific customer.
     */
    List<AccountRecord> findByCustomerId(Long customerId);

    /**
     * Retrieve account by account number (12-digit unique identifier).
     */
    Optional<AccountRecord> findByAccountNumber(String accountNumber);
}

