package com.neueda.controller;

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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.neueda.domain.AccountRecord;
import com.neueda.domain.AccountStatus;
import com.neueda.domain.BulkPaymentBatchStatus;
import com.neueda.domain.CustomerRecord;
import com.neueda.domain.CustomerStatus;
import com.neueda.domain.PaymentRecord;
import com.neueda.domain.PaymentStatus;
import com.neueda.dto.AccountDetailsDTO;
import com.neueda.dto.AccountSummaryDTO;
import com.neueda.dto.BulkPaymentSummaryDTO;
import com.neueda.dto.CreateCustomerRequest;
import com.neueda.dto.CustomerPaymentStatisticsDTO;
import com.neueda.dto.CustomerProfileDTO;
import com.neueda.dto.CustomerRiskDTO;
import com.neueda.dto.TransactionSummaryDTO;
import com.neueda.exception.GlobalExceptionHandler;
import com.neueda.service.AccountService;
import com.neueda.service.CustomerProfileService;
import com.neueda.service.CustomerService;
import com.neueda.service.FraudRiskService;
import com.neueda.service.PaymentService;

@ExtendWith(MockitoExtension.class)
class CustomerControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CustomerService customerService;
    @Mock
    private PaymentService paymentService;
    @Mock
    private AccountService accountService;
    @Mock
    private FraudRiskService fraudRiskService;
    @Mock
    private CustomerProfileService customerProfileService;

    private ObjectMapper objectMapper;
    private CustomerRecord sampleCustomer;
    private AccountRecord sampleAccount;
    private PaymentRecord samplePayment;

    @BeforeEach
    void setUp() {
        CustomerController controller = new CustomerController(
                customerService, paymentService, accountService, fraudRiskService, customerProfileService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        sampleCustomer = new CustomerRecord(
                1L, "John Doe", LocalDate.of(1990, 1, 1), "+1234567890", "ABCDE1234F",
                LocalDateTime.now(), LocalDateTime.now(), "USA", CustomerStatus.ACTIVE);

        sampleAccount = new AccountRecord(
                10L, 1L, "111122223333", "Checking", new BigDecimal("1000.00"), AccountStatus.ACTIVE,
                "USD", LocalDate.now(), LocalDateTime.now(), "IFSC0001234", "NY", "Bank", "1234");

        samplePayment = new PaymentRecord(
                100L, null, "111122223333", "444455556666", new BigDecimal("100.00"), "USD",
                null, null, null, PaymentStatus.COMPLETED, null, null,
                0, 3, null, null, null, LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    @DisplayName("POST /api/customers: Creates new customer")
    void createCustomer_Success() throws Exception {
        CreateCustomerRequest request = new CreateCustomerRequest(
                "John Doe", LocalDate.of(1990, 1, 1), "+1234567890", "ABCDE1234F", "USA");

        when(customerService.createCustomer(any())).thenReturn(sampleCustomer);

        mockMvc.perform(post("/api/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").value(1))
                .andExpect(jsonPath("$.name").value("John Doe"));
    }

    @Test
    @DisplayName("GET /api/customers/{id}: Returns customer profile")
    void getCustomer_Success() throws Exception {
        when(customerService.getCustomerById(1L)).thenReturn(Optional.of(sampleCustomer));

        mockMvc.perform(get("/api/customers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(1))
                .andExpect(jsonPath("$.name").value("John Doe"));
    }

    @Test
    @DisplayName("GET /api/customers/{id}: Returns 404 when customer not found")
    void getCustomer_NotFound() throws Exception {
        when(customerService.getCustomerById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/customers/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /api/customers/{id}/payments: Returns customer payments")
    void getCustomerPayments_Success() throws Exception {
        when(customerService.getCustomerById(1L)).thenReturn(Optional.of(sampleCustomer));
        when(accountService.getAccountsByCustomerId(1L)).thenReturn(List.of(sampleAccount));
        when(paymentService.listPaymentsFiltered(any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of(samplePayment));
        when(fraudRiskService.assessPaymentRisk(any()))
                .thenReturn(new FraudRiskService.PaymentRisk(5.0, false));
        when(paymentService.getValidationResults(100L)).thenReturn(List.of());

        mockMvc.perform(get("/api/customers/1/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(100));
    }

    @Test
    @DisplayName("GET /api/customers/{id}/statistics: Returns customer payment statistics")
    void getCustomerStatistics_Success() throws Exception {
        when(customerService.getCustomerById(1L)).thenReturn(Optional.of(sampleCustomer));
        when(accountService.getAccountsByCustomerId(1L)).thenReturn(List.of(sampleAccount));
        when(paymentService.listPaymentsFiltered(any(), any(), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of(samplePayment));

        mockMvc.perform(get("/api/customers/1/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPayments").value(1))
                .andExpect(jsonPath("$.completedPayments").value(1));
    }

    @Test
    @DisplayName("GET /api/customers/{id}/profile: Returns profile DTO")
    void getProfile_Success() throws Exception {
        CustomerProfileDTO profileDTO = new CustomerProfileDTO(
                1L, "John Doe", LocalDate.of(1990, 1, 1), "+1234567890", "ABCDE1234F",
                "USA", CustomerStatus.ACTIVE, LocalDateTime.now(), LocalDateTime.now());
        when(customerProfileService.getCustomerProfile(1L)).thenReturn(profileDTO);

        mockMvc.perform(get("/api/customers/1/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(1))
                .andExpect(jsonPath("$.name").value("John Doe"));
    }

    @Test
    @DisplayName("GET /api/customers/{id}/profile/accounts: Returns account summary DTOs")
    void getProfileAccounts_Success() throws Exception {
        AccountSummaryDTO dto = new AccountSummaryDTO(10L, "111122223333", "Checking", "SAVINGS", "USD",
                new BigDecimal("1000.00"), new BigDecimal("1000.00"), AccountStatus.ACTIVE, LocalDate.now());
        when(customerProfileService.getCustomerAccounts(1L)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/customers/1/profile/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].accountNumber").value("111122223333"));
    }

    @Test
    @DisplayName("GET /api/customers/{id}/profile/accounts/{acc}: Returns account details")
    void getAccountDetails_Success() throws Exception {
        AccountDetailsDTO dto = new AccountDetailsDTO(10L, "111122223333", "Checking", "SAVINGS", "USD",
                LocalDate.now(), AccountStatus.ACTIVE, "IFSC0001234", "Bank", "NY",
                new BigDecimal("1000.00"), new BigDecimal("1000.00"), BigDecimal.ZERO,
                List.of(), LocalDateTime.now(), 0L);
        when(customerProfileService.getAccountDetails(1L, "111122223333")).thenReturn(dto);

        mockMvc.perform(get("/api/customers/1/profile/accounts/111122223333"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("111122223333"));
    }

    @Test
    @DisplayName("GET /api/customers/{id}/profile/transactions: Returns transactions")
    void getProfileTransactions_Success() throws Exception {
        TransactionSummaryDTO dto = new TransactionSummaryDTO(100L, "111122223333", "444455556666",
                new BigDecimal("100.00"), "USD", PaymentStatus.COMPLETED, LocalDateTime.now(), null);
        when(customerProfileService.getCustomerTransactions(eq(1L), any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of(dto));

        mockMvc.perform(get("/api/customers/1/profile/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].transactionId").value(100));
    }

    @Test
    @DisplayName("GET /api/customers/{id}/profile/payment-statistics: Returns payment statistics")
    void getProfilePaymentStatistics_Success() throws Exception {
        CustomerPaymentStatisticsDTO dto = new CustomerPaymentStatisticsDTO(10L, 8L, 1L, 1L, new BigDecimal("5000.00"),
                new BigDecimal("500.00"));
        when(customerProfileService.getPaymentStatistics(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/customers/1/profile/payment-statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPayments").value(10));
    }

    @Test
    @DisplayName("GET /api/customers/{id}/profile/risk: Returns risk info")
    void getProfileRiskInformation_Success() throws Exception {
        CustomerRiskDTO dto = new CustomerRiskDTO("LOW", 0L, 0L, LocalDateTime.now());
        when(customerProfileService.getCustomerRiskInformation(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/customers/1/profile/risk"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riskLevel").value("LOW"));
    }

    @Test
    @DisplayName("GET /api/customers/{id}/profile/bulk-payments: Returns bulk payment summaries")
    void getProfileBulkPayments_Success() throws Exception {
        BulkPaymentSummaryDTO dto = new BulkPaymentSummaryDTO(1L, "BP123", "111122223333", 10, 10, 0,
                new BigDecimal("1000.00"), BulkPaymentBatchStatus.COMPLETED, LocalDateTime.now(), null);
        when(customerProfileService.getCustomerBulkPayments(1L, 0, 10)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/customers/1/profile/bulk-payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].batchReference").value("BP123"));
    }
}
