package com.neueda.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.neueda.domain.AccountRecord;
import com.neueda.domain.CustomerStatus;
import com.neueda.dto.CreateAccountRequest;
import com.neueda.exception.AccountValidationException;
import com.neueda.exception.CustomerNotFoundException;
import com.neueda.repository.AccountRepository;
import com.neueda.repository.CustomerRepository;

/**
 * Service layer for customer account operations.
 */
@Service
@Transactional
public class AccountService {

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;

    public AccountService(
        AccountRepository accountRepository,
        CustomerRepository customerRepository
    ) {
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
    }

    /**
     * Create an account for an existing, active customer profile.
     */
    public AccountRecord createAccount(Long customerId, CreateAccountRequest request) {
        var customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new CustomerNotFoundException("Customer not found: " + customerId));

        if (customer.customerAccountStatus() != CustomerStatus.ACTIVE) {
            throw new AccountValidationException(
                "Cannot create account for non-active customer profile",
                "CUSTOMER_PROFILE_NOT_ACTIVE"
            );
        }

        String accountPin = request.accountPin();
        AccountRecord accountToSave = AccountRecord.create(
            customerId,
            request.accountName(),
            request.accountBalance(),
            request.currency(),
            request.accountOpeningDate(),
            request.ifscCode(),
            request.accountLocation(),
            request.bankName(),
            accountPin
        );

        return accountRepository.save(accountToSave);
    }

    public Optional<AccountRecord> getAccountById(Long accountId) {
        return accountRepository.findById(accountId);
    }

    public List<AccountRecord> getAccountsByCustomerId(Long customerId) {
        if (customerRepository.findById(customerId).isEmpty()) {
            throw new CustomerNotFoundException("Customer not found: " + customerId);
        }
        return accountRepository.findByCustomerId(customerId);
    }

    /**
     * Validate an input PIN using direct string equality against stored PIN.
     */
    public boolean verifyAccountPin(Long accountId, String rawPin) {
        AccountRecord account = accountRepository.findById(accountId)
            .orElseThrow(() -> new AccountValidationException("Account not found: " + accountId, "ACCOUNT_NOT_FOUND"));
        return account.accountPin().equals(rawPin);
    }
}


