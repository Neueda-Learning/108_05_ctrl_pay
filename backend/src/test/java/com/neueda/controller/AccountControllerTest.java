package com.neueda.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

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
import org.springframework.test.web.servlet.MockMvc;
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
        AccountController controller = new AccountController(accountService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        sampleAccount = new AccountRecord(
            1L, 101L, "123456789012", "John Doe-Savings", BigDecimal.valueOf(1000), AccountStatus.ACTIVE,
            "USD", LocalDate.now(), LocalDateTime.now(), "IFSC0000001", "New York", "Global Bank", "1234"
        );
    }

    @Test
    @DisplayName("POST /api/customers/101/accounts: Success creates account")
    void createAccount_Success() throws Exception {
        CreateAccountRequest req = new CreateAccountRequest(
            "123456789012", "Savings", BigDecimal.valueOf(1000), "USD",
            LocalDate.now(), "IFSC0000001", "New York", "Global Bank", "1234"
        );

        when(accountService.createAccount(eq(101L), any())).thenReturn(sampleAccount);

        mockMvc.perform(post("/api/customers/101/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.accountNumber").value("123456789012"))
            .andExpect(jsonPath("$.accountName").value("John Doe-Savings"));
    }
}
