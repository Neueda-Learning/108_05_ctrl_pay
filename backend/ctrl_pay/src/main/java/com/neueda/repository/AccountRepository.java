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
     */
    AccountRecord save(AccountRecord account);

    /**
     * Retrieve account by ID.
     */
    Optional<AccountRecord> findById(Long accountId);

    /**
     * Retrieve all accounts for a specific customer.
     */
    List<AccountRecord> findByCustomerId(Long customerId);
}

