package com.neueda.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
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
import com.neueda.domain.CustomerRecord;
import com.neueda.domain.CustomerStatus;
import com.neueda.dto.CreateCustomerRequest;
import com.neueda.exception.GlobalExceptionHandler;
import com.neueda.service.AccountService;
import com.neueda.service.CustomerProfileService;
import com.neueda.service.CustomerService;
import com.neueda.service.FraudRiskService;
import com.neueda.service.PaymentService;

@ExtendWith(MockitoExtension.class)
class CustomerControllerTest {

    @Mock private CustomerService customerService;
    @Mock private PaymentService paymentService;
    @Mock private AccountService accountService;
    @Mock private FraudRiskService fraudRiskService;
    @Mock private CustomerProfileService customerProfileService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private CustomerRecord sampleCustomer;

    @BeforeEach
    void setUp() {
        CustomerController controller = new CustomerController(
            customerService, paymentService, accountService, fraudRiskService, customerProfileService
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        sampleCustomer = new CustomerRecord(
            1L, "Alice Smith", LocalDate.of(1990, 1, 1), "+1234567890", "ABCDE1234F",
            LocalDateTime.now(), LocalDateTime.now(), "US", CustomerStatus.ACTIVE
        );
    }

    @Test
    @DisplayName("POST /api/customers: Creates customer profile")
    void createCustomer_Success() throws Exception {
        CreateCustomerRequest req = new CreateCustomerRequest("Alice Smith", LocalDate.of(1990, 1, 1), "+1234567890", "ABCDE1234F", "US");

        when(customerService.createCustomer(any())).thenReturn(sampleCustomer);

        mockMvc.perform(post("/api/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Alice Smith"))
            .andExpect(jsonPath("$.panNumber").value("ABCDE1234F"));
    }

    @Test
    @DisplayName("GET /api/customers/1: Returns customer by ID")
    void getCustomerById_Success() throws Exception {
        when(customerService.getCustomerById(1L)).thenReturn(Optional.of(sampleCustomer));

        mockMvc.perform(get("/api/customers/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Alice Smith"));
    }
}
