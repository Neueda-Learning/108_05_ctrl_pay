package com.neueda.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.neueda.domain.AccountRecord;
import com.neueda.dto.AccountResponse;
import com.neueda.dto.CreateAccountRequest;
import com.neueda.exception.AccountNotFoundException;
import com.neueda.service.AccountService;

import jakarta.validation.Valid;

/**
 * REST controller for account onboarding under a customer profile.
 */
@RestController
@RequestMapping("/api")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    /**
     * Create an account for an existing customer.
     */
    @PostMapping("/customers/{customerId}/accounts")
    public ResponseEntity<AccountResponse> createAccount(
        @PathVariable Long customerId,
        @Valid @RequestBody CreateAccountRequest request
    ) {
        AccountRecord saved = accountService.createAccount(customerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
    }

    /**
     * List all accounts for a customer.
     */
    @GetMapping("/customers/{customerId}/accounts")
    public ResponseEntity<List<AccountResponse>> getAccountsByCustomer(@PathVariable Long customerId) {
        List<AccountResponse> accounts = accountService.getAccountsByCustomerId(customerId).stream()
            .map(this::toResponse)
            .toList();
        return ResponseEntity.ok(accounts);
    }

    /**
     * Retrieve account by ID.
     */
    @GetMapping("/accounts/{accountId}")
    public ResponseEntity<AccountResponse> getAccountById(@PathVariable Long accountId) {
        AccountRecord account = accountService.getAccountById(accountId)
            .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountId));
        return ResponseEntity.ok(toResponse(account));
    }

    /**
     * Retrieve account by account number (12-digit unique identifier).
     * 
     * Request: GET /api/accounts/by-number/{accountNumber}
     * 
     * Response:
     * - 200 OK: Account found
     * - 404 Not Found: Account does not exist
     */
    @GetMapping("/accounts/by-number/{accountNumber}")
    public ResponseEntity<AccountResponse> getAccountByAccountNumber(@PathVariable String accountNumber) {
        AccountRecord account = accountService.getAccountByAccountNumber(accountNumber)
            .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));
        return ResponseEntity.ok(toResponse(account));
    }

    private AccountResponse toResponse(AccountRecord account) {
        return new AccountResponse(
            account.accountId(),
            account.customerId(),
            account.accountNumber(),
            account.accountName(),
            account.accountBalance(),
            account.accountStatus(),
            account.currency(),
            account.accountOpeningDate(),
            account.lastUpdated(),
            account.ifscCode(),
            account.accountLocation(),
            account.bankName()
        );
    }
}



