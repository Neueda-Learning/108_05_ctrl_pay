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
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
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
import com.neueda.dto.CreateAccountRequest;
import com.neueda.exception.GlobalExceptionHandler;
import com.neueda.service.AccountService;

@ExtendWith(MockitoExtension.class)
class AccountControllerTest {

    @Mock
    private AccountService accountService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private AccountRecord sampleAccount;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        AccountController controller = new AccountController(accountService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
            .build();

        sampleAccount = new AccountRecord(
            1L, 101L, "123456789012", "John Doe Account", BigDecimal.valueOf(1000), AccountStatus.ACTIVE,
            "USD", LocalDate.now(), LocalDateTime.now(), "ABCD0123456", "NYC", "Global Bank", "1234"
        );
    }

    @Test
    @DisplayName("POST /api/customers/101/accounts: Success create account")
    void createAccount_Success() throws Exception {
        when(accountService.createAccount(eq(101L), any())).thenReturn(sampleAccount);

        CreateAccountRequest req = new CreateAccountRequest(
            "123456789012", "John Doe Account", BigDecimal.valueOf(1000), "USD",
            LocalDate.now(), "ABCD0123456", "NYC", "Global Bank", "1234"
        );

        mockMvc.perform(post("/api/customers/101/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.accountNumber").value("123456789012"));
    }

    @Test
    @DisplayName("GET /api/customers/101/accounts: Success list accounts for customer")
    void getAccountsByCustomer_Success() throws Exception {
        when(accountService.getAccountsByCustomerId(101L)).thenReturn(List.of(sampleAccount));

        mockMvc.perform(get("/api/customers/101/accounts"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].accountNumber").value("123456789012"));
    }

    @Test
    @DisplayName("GET /api/accounts/1: Success get account by ID")
    void getAccountById_Success() throws Exception {
        when(accountService.getAccountById(1L)).thenReturn(Optional.of(sampleAccount));

        mockMvc.perform(get("/api/accounts/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accountNumber").value("123456789012"));
    }

    @Test
    @DisplayName("GET /api/accounts/by-number/123456789012: Success get account by number")
    void getAccountByAccountNumber_Success() throws Exception {
        when(accountService.getAccountByAccountNumber("123456789012")).thenReturn(Optional.of(sampleAccount));

        mockMvc.perform(get("/api/accounts/by-number/123456789012"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accountNumber").value("123456789012"));
    }
}
