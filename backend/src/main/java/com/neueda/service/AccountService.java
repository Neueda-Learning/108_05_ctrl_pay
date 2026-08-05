package com.neueda.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

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
    private final FraudRiskService fraudRiskService;

    public AccountService(
        AccountRepository accountRepository,
        CustomerRepository customerRepository,
        FraudRiskService fraudRiskService
    ) {
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
        this.fraudRiskService = fraudRiskService;
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
            request.accountNumber(),
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
        Optional<AccountRecord> account = accountRepository.findById(accountId);
        account.ifPresent(value -> refreshSuspiciousStatusSafely(value.accountNumber()));
        return accountRepository.findById(accountId);
    }

    public Optional<AccountRecord> getAccountByAccountNumber(String accountNumber) {
        Optional<AccountRecord> account = accountRepository.findByAccountNumber(accountNumber);
        account.ifPresent(value -> refreshSuspiciousStatusSafely(value.accountNumber()));
        return accountRepository.findByAccountNumber(accountNumber);
    }

    public List<AccountRecord> getAccountsByCustomerId(Long customerId) {
        if (customerRepository.findById(customerId).isEmpty()) {
            throw new CustomerNotFoundException("Customer not found: " + customerId);
        }
        List<AccountRecord> accounts = accountRepository.findByCustomerId(customerId);
        for (AccountRecord account : accounts) {
            refreshSuspiciousStatusSafely(account.accountNumber());
        }
        return accountRepository.findByCustomerId(customerId);
    }

    /**
     * Check whether the given customer owns at least one of the supplied account numbers.
     */
    public boolean customerOwnsAnyAccount(Long customerId, String... accountNumbers) {
        Set<String> customerAccountNumbers = getAccountsByCustomerId(customerId).stream()
            .map(AccountRecord::accountNumber)
            .collect(java.util.stream.Collectors.toSet());

        for (String accountNumber : accountNumbers) {
            if (accountNumber != null && customerAccountNumbers.contains(accountNumber)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Validate an input PIN using direct string equality against stored PIN.
     */
    public boolean verifyAccountPin(Long accountId, String rawPin) {
        AccountRecord account = accountRepository.findById(accountId)
            .orElseThrow(() -> new AccountValidationException("Account not found: " + accountId, "ACCOUNT_NOT_FOUND"));
        return account.accountPin().equals(rawPin);
    }

    /**
     * Verify account PIN by account number (12-digit identifier).
     * Useful for payment processing where we have account number not ID.
     * 
     * @param accountNumber 12-digit account number
     * @param pin 4-6 digit PIN to verify
     * @return true if PIN matches
     * @throws AccountValidationException if account not found or PIN is invalid
     */
    public boolean verifyAccountPinByAccountNumber(String accountNumber, String pin) {
        AccountRecord account = accountRepository.findByAccountNumber(accountNumber)
            .orElseThrow(() -> new AccountValidationException("Account not found: " + accountNumber, "ACCOUNT_NOT_FOUND"));
        
        if (!account.accountPin().equals(pin)) {
            throw new AccountValidationException("Invalid PIN for account: " + accountNumber, "INVALID_PIN");
        }
        
        return true;
    }

    private void refreshSuspiciousStatusSafely(String accountNumber) {
        try {
            fraudRiskService.refreshAccountRiskStatus(accountNumber);
        } catch (Exception ignored) {
            // Risk refresh is best-effort and must never affect core account reads.
        }
    }
}


