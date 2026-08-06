package com.neueda.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.neueda.domain.AccountRecord;
import com.neueda.domain.CustomerRecord;
import com.neueda.domain.CustomerStatus;
import com.neueda.dto.CreateAccountRequest;
import com.neueda.exception.AccountValidationException;
import com.neueda.exception.CustomerNotFoundException;
import com.neueda.repository.AccountRepository;
import com.neueda.repository.CustomerRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private FraudRiskService fraudRiskService;

    private AccountService accountService;

    private CustomerRecord activeCustomer;
    private CustomerRecord inactiveCustomer;
    private AccountRecord sampleAccount;

    @BeforeEach
    void setUp() {
        accountService = new AccountService(accountRepository, customerRepository, fraudRiskService);

        activeCustomer = new CustomerRecord(
            101L, "John Doe", LocalDate.of(1990, 1, 1), "+1234567890", "ABCDE1234F",
            LocalDateTime.now(), LocalDateTime.now(), "US", CustomerStatus.ACTIVE
        );

        inactiveCustomer = new CustomerRecord(
            102L, "Jane Doe", LocalDate.of(1992, 2, 2), "+0987654321", "FGHIJ5678K",
            LocalDateTime.now(), LocalDateTime.now(), "US", CustomerStatus.PASSIVE
        );

        sampleAccount = AccountRecord.create(
            101L, "123456789012", "John Doe-Savings", BigDecimal.valueOf(1000),
            "USD", LocalDate.now(), "IFSC0001", "New York", "Global Bank", "1234"
        );
    }

    @Test
    @DisplayName("createAccount: Should create account successfully when customer is ACTIVE")
    void createAccount_Success() {
        CreateAccountRequest request = new CreateAccountRequest(
            "123456789012", "Savings", BigDecimal.valueOf(1000), "USD",
            LocalDate.now(), "IFSC0001", "New York", "Global Bank", "1234"
        );

        when(customerRepository.findById(101L)).thenReturn(Optional.of(activeCustomer));
        when(accountRepository.save(any(AccountRecord.class))).thenReturn(sampleAccount);

        AccountRecord created = accountService.createAccount(101L, request);

        assertThat(created).isNotNull();
        assertThat(created.accountName()).isEqualTo("John Doe-Savings");
        verify(accountRepository).save(any(AccountRecord.class));
    }

    @Test
    @DisplayName("createAccount: Should throw CustomerNotFoundException when customer does not exist")
    void createAccount_CustomerNotFound() {
        CreateAccountRequest request = new CreateAccountRequest(
            "123456789012", "Savings", BigDecimal.valueOf(1000), "USD",
            LocalDate.now(), "IFSC0001", "New York", "Global Bank", "1234"
        );

        when(customerRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.createAccount(999L, request))
            .isInstanceOf(CustomerNotFoundException.class)
            .hasMessageContaining("Customer not found: 999");
    }

    @Test
    @DisplayName("createAccount: Should throw AccountValidationException when customer status is not ACTIVE")
    void createAccount_CustomerInactive() {
        CreateAccountRequest request = new CreateAccountRequest(
            "123456789012", "Savings", BigDecimal.valueOf(1000), "USD",
            LocalDate.now(), "IFSC0001", "New York", "Global Bank", "1234"
        );

        when(customerRepository.findById(102L)).thenReturn(Optional.of(inactiveCustomer));

        assertThatThrownBy(() -> accountService.createAccount(102L, request))
            .isInstanceOf(AccountValidationException.class)
            .hasMessageContaining("Cannot create account for non-active customer profile");
    }

    @Test
    @DisplayName("getAccountById: Should return account and silently attempt risk refresh")
    void getAccountById_Found() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(sampleAccount));

        Optional<AccountRecord> result = accountService.getAccountById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().accountNumber()).isEqualTo("123456789012");
        verify(fraudRiskService).refreshAccountRiskStatus("123456789012");
    }

    @Test
    @DisplayName("getAccountById: Should safely handle risk refresh exception")
    void getAccountById_RiskRefreshThrowsException() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(sampleAccount));
        doThrow(new RuntimeException("Fraud service down")).when(fraudRiskService).refreshAccountRiskStatus("123456789012");

        Optional<AccountRecord> result = accountService.getAccountById(1L);

        assertThat(result).isPresent();
    }

    @Test
    @DisplayName("getAccountByAccountNumber: Should return account by account number")
    void getAccountByAccountNumber_Found() {
        when(accountRepository.findByAccountNumber("123456789012")).thenReturn(Optional.of(sampleAccount));

        Optional<AccountRecord> result = accountService.getAccountByAccountNumber("123456789012");

        assertThat(result).isPresent();
        assertThat(result.get().customerId()).isEqualTo(101L);
    }

    @Test
    @DisplayName("getAccountsByCustomerId: Should return accounts when customer exists")
    void getAccountsByCustomerId_Success() {
        when(customerRepository.findById(101L)).thenReturn(Optional.of(activeCustomer));
        when(accountRepository.findByCustomerId(101L)).thenReturn(List.of(sampleAccount));

        List<AccountRecord> accounts = accountService.getAccountsByCustomerId(101L);

        assertThat(accounts).hasSize(1);
        assertThat(accounts.get(0).accountNumber()).isEqualTo("123456789012");
    }

    @Test
    @DisplayName("getAccountsByCustomerId: Should throw CustomerNotFoundException when customer missing")
    void getAccountsByCustomerId_NotFound() {
        when(customerRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getAccountsByCustomerId(999L))
            .isInstanceOf(CustomerNotFoundException.class);
    }

    @Test
    @DisplayName("customerOwnsAnyAccount: Should return true when customer owns any of target account numbers")
    void customerOwnsAnyAccount_True() {
        when(customerRepository.findById(101L)).thenReturn(Optional.of(activeCustomer));
        when(accountRepository.findByCustomerId(101L)).thenReturn(List.of(sampleAccount));

        boolean owns = accountService.customerOwnsAnyAccount(101L, "999999999999", "123456789012");

        assertThat(owns).isTrue();
    }

    @Test
    @DisplayName("customerOwnsAnyAccount: Should return false when customer owns none of account numbers")
    void customerOwnsAnyAccount_False() {
        when(customerRepository.findById(101L)).thenReturn(Optional.of(activeCustomer));
        when(accountRepository.findByCustomerId(101L)).thenReturn(List.of(sampleAccount));

        boolean owns = accountService.customerOwnsAnyAccount(101L, "999999999999");

        assertThat(owns).isFalse();
    }

    @Test
    @DisplayName("verifyAccountPin: Should return true for correct PIN and false for wrong PIN")
    void verifyAccountPin_Validation() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(sampleAccount));

        boolean match = accountService.verifyAccountPin(1L, "1234");
        boolean wrong = accountService.verifyAccountPin(1L, "12345");

        assertThat(match).isTrue();
        assertThat(wrong).isFalse();
    }

    @Test
    @DisplayName("verifyAccountPin: Should throw AccountValidationException if account not found")
    void verifyAccountPin_AccountNotFound() {
        when(accountRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.verifyAccountPin(99L, "1234"))
            .isInstanceOf(AccountValidationException.class);
    }

    @Test
    @DisplayName("verifyAccountPinByAccountNumber: Should verify PIN or throw on mismatch")
    void verifyAccountPinByAccountNumber_Validation() {
        when(accountRepository.findByAccountNumber("123456789012")).thenReturn(Optional.of(sampleAccount));

        boolean verified = accountService.verifyAccountPinByAccountNumber("123456789012", "1234");
        assertThat(verified).isTrue();

        assertThatThrownBy(() -> accountService.verifyAccountPinByAccountNumber("123456789012", "9999"))
            .isInstanceOf(AccountValidationException.class)
            .hasMessageContaining("Invalid PIN for account");
    }
}
