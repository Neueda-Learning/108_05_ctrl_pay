package com.neueda.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.neueda.domain.AccountRecord;
import com.neueda.domain.AccountStatus;
import com.neueda.domain.CustomerRecord;
import com.neueda.domain.CustomerStatus;
import com.neueda.domain.FraudAssessmentRecord;
import com.neueda.domain.FraudDecision;
import com.neueda.domain.FraudRiskLevel;
import com.neueda.domain.PaymentRecord;
import com.neueda.domain.PaymentStatus;
import com.neueda.dto.AccountDetailsDTO;
import com.neueda.dto.AccountSummaryDTO;
import com.neueda.dto.BulkPaymentSummaryDTO;
import com.neueda.dto.CustomerPaymentStatisticsDTO;
import com.neueda.dto.CustomerProfileDTO;
import com.neueda.dto.CustomerRiskDTO;
import com.neueda.dto.TransactionSummaryDTO;
import com.neueda.exception.AccountValidationException;
import com.neueda.exception.CustomerNotFoundException;
import com.neueda.repository.AccountRepository;
import com.neueda.repository.BulkPaymentBatchRepository;
import com.neueda.repository.CustomerRepository;
import com.neueda.repository.FraudAssessmentRepository;
import com.neueda.repository.PaymentRepository;

@ExtendWith(MockitoExtension.class)
class CustomerProfileServiceTest {

    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private FraudAssessmentRepository fraudAssessmentRepository;
    @Mock
    private BulkPaymentBatchRepository bulkPaymentBatchRepository;

    private CustomerProfileService profileService;

    private CustomerRecord sampleCustomer;
    private AccountRecord sampleAccount;
    private PaymentRecord samplePayment;

    @BeforeEach
    void setUp() {
        profileService = new CustomerProfileService(
            customerRepository,
            accountRepository,
            paymentRepository,
            fraudAssessmentRepository,
            bulkPaymentBatchRepository
        );

        sampleCustomer = new CustomerRecord(
            1L, "John Doe", LocalDate.of(1990, 1, 1), "+1234567890",
            "ABCDE1234F", LocalDateTime.now(), LocalDateTime.now(), "USA", CustomerStatus.ACTIVE
        );

        sampleAccount = new AccountRecord(
            100L, 1L, "ACC123", "John Main Account", BigDecimal.valueOf(5000),
            AccountStatus.ACTIVE, "USD", LocalDate.now(), LocalDateTime.now(),
            "IFSC001", "NYC", "Global Bank", "1234"
        );

        samplePayment = new PaymentRecord(
            1000L, "IDEM-01", "ACC123", "ACC456", BigDecimal.valueOf(200),
            "USD", BigDecimal.valueOf(200), BigDecimal.valueOf(200), BigDecimal.ONE,
            PaymentStatus.COMPLETED, null, null, 0, 3, null, null, null,
            LocalDateTime.now(), LocalDateTime.now()
        );
    }

    @Test
    @DisplayName("getCustomerProfile: Success returns customer profile DTO")
    void getCustomerProfile_Success() {
        // Arrange
        when(customerRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));

        // Act
        CustomerProfileDTO profile = profileService.getCustomerProfile(1L);

        // Assert
        assertNotNull(profile);
        assertEquals(1L, profile.customerId());
        assertEquals("John Doe", profile.name());
        assertEquals("ABCDE1234F", profile.panNumber());
    }

    @Test
    @DisplayName("getCustomerProfile: Throws CustomerNotFoundException when non-existent")
    void getCustomerProfile_NotFound() {
        // Arrange
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(CustomerNotFoundException.class, () -> profileService.getCustomerProfile(99L));
    }

    @Test
    @DisplayName("getCustomerAccounts: Success returns list of AccountSummaryDTO")
    void getCustomerAccounts_Success() {
        // Arrange
        when(customerRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));
        when(accountRepository.findByCustomerId(1L)).thenReturn(List.of(sampleAccount));

        // Act
        List<AccountSummaryDTO> accounts = profileService.getCustomerAccounts(1L);

        // Assert
        assertEquals(1, accounts.size());
        assertEquals("ACC123", accounts.get(0).accountNumber());
    }

    @Test
    @DisplayName("getCustomerAccounts: Throws CustomerNotFoundException")
    void getCustomerAccounts_NotFound() {
        // Arrange
        when(customerRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(CustomerNotFoundException.class, () -> profileService.getCustomerAccounts(1L));
    }

    @Test
    @DisplayName("getAccountDetails: Success returns detailed account info and recent transactions")
    void getAccountDetails_Success() {
        // Arrange
        when(customerRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));
        when(accountRepository.findByAccountNumber("ACC123")).thenReturn(Optional.of(sampleAccount));
        when(paymentRepository.findAll()).thenReturn(List.of(samplePayment));

        // Act
        AccountDetailsDTO details = profileService.getAccountDetails(1L, "ACC123");

        // Assert
        assertNotNull(details);
        assertEquals("ACC123", details.accountNumber());
        assertEquals(1, details.recentTransactions().size());
        assertEquals(1L, details.transactionCount());
    }

    @Test
    @DisplayName("getAccountDetails: Throws when customer does not exist")
    void getAccountDetails_CustomerNotFound() {
        // Arrange
        when(customerRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(CustomerNotFoundException.class, () -> profileService.getAccountDetails(1L, "ACC123"));
    }

    @Test
    @DisplayName("getAccountDetails: Throws AccountValidationException when account not found or not owned")
    void getAccountDetails_UnauthorizedAccount() {
        // Arrange
        AccountRecord otherAccount = new AccountRecord(
            101L, 999L, "ACC999", "Other Account", BigDecimal.TEN,
            AccountStatus.ACTIVE, "USD", LocalDate.now(), LocalDateTime.now(),
            "IFSC001", "NYC", "Bank", "1234"
        );
        when(customerRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));
        when(accountRepository.findByAccountNumber("ACC999")).thenReturn(Optional.of(otherAccount));

        // Act & Assert
        assertThrows(AccountValidationException.class, () -> profileService.getAccountDetails(1L, "ACC999"));
    }

    @Test
    @DisplayName("getCustomerTransactions: Success returns paginated transactions")
    void getCustomerTransactions_Success() {
        // Arrange
        when(customerRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));
        when(accountRepository.findByCustomerId(1L)).thenReturn(List.of(sampleAccount));
        when(paymentRepository.findAllFiltered(any(), eq("ACC123"), any(), any(), any(), any(), anyInt(), anyInt()))
            .thenReturn(List.of(samplePayment));

        // Act
        List<TransactionSummaryDTO> transactions = profileService.getCustomerTransactions(
            1L, PaymentStatus.COMPLETED, "ACC123", null, null, 0, 10
        );

        // Assert
        assertEquals(1, transactions.size());
        assertEquals("ACC123", transactions.get(0).sourceAccount());
    }

    @Test
    @DisplayName("getCustomerTransactions: Empty account list returns empty result")
    void getCustomerTransactions_NoAccounts() {
        // Arrange
        when(customerRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));
        when(accountRepository.findByCustomerId(1L)).thenReturn(Collections.emptyList());

        // Act
        List<TransactionSummaryDTO> transactions = profileService.getCustomerTransactions(
            1L, null, null, null, null, 0, 10
        );

        // Assert
        assertTrue(transactions.isEmpty());
    }

    @Test
    @DisplayName("getPaymentStatistics: Success calculates totals and averages")
    void getPaymentStatistics_Success() {
        // Arrange
        when(customerRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));
        when(accountRepository.findByCustomerId(1L)).thenReturn(List.of(sampleAccount));
        when(paymentRepository.findAll()).thenReturn(List.of(samplePayment));

        FraudAssessmentRecord assessment = FraudAssessmentRecord.create(
            1000L, BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ZERO, "[]", "{}",
            FraudDecision.APPROVED, FraudRiskLevel.LOW, "OK"
        );
        when(fraudAssessmentRepository.findByPaymentId(1000L)).thenReturn(Optional.of(assessment));

        // Act
        CustomerPaymentStatisticsDTO stats = profileService.getPaymentStatistics(1L);

        // Assert
        assertEquals(1L, stats.totalPayments());
        assertEquals(1L, stats.successfulPayments());
        assertEquals(BigDecimal.valueOf(200), stats.totalAmount());
    }

    @Test
    @DisplayName("getCustomerRiskInformation: Calculates risk level correctly")
    void getCustomerRiskInformation_Success() {
        // Arrange
        when(customerRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));
        when(accountRepository.findByCustomerId(1L)).thenReturn(List.of(sampleAccount));
        when(paymentRepository.findAll()).thenReturn(List.of(samplePayment));
        when(fraudAssessmentRepository.findByPaymentId(1000L)).thenReturn(Optional.empty());

        // Act
        CustomerRiskDTO risk = profileService.getCustomerRiskInformation(1L);

        // Assert
        assertEquals("LOW", risk.riskLevel());
        assertEquals(0L, risk.fraudFlags());
    }

    @Test
    @DisplayName("getCustomerBulkPayments: Gracefully handles empty bulk payments")
    void getCustomerBulkPayments_Empty() {
        // Arrange
        when(customerRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));
        when(accountRepository.findByCustomerId(1L)).thenReturn(List.of(sampleAccount));
        when(bulkPaymentBatchRepository.findBySourceAccount(eq("ACC123"), anyInt(), anyInt()))
            .thenReturn(Collections.emptyList());

        // Act
        List<BulkPaymentSummaryDTO> bulkPayments = profileService.getCustomerBulkPayments(1L, 0, 10);

        // Assert
        assertTrue(bulkPayments.isEmpty());
    }
}
