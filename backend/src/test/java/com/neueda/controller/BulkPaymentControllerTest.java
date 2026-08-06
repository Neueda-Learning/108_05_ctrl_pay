package com.neueda.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.neueda.dto.BulkPaymentItemDTO;
import com.neueda.dto.BulkPaymentResponseDTO;
import com.neueda.dto.CSVValidationResultDTO;
import com.neueda.dto.CreateBulkPaymentRequest;
import com.neueda.exception.GlobalExceptionHandler;
import com.neueda.service.bulk.BulkPaymentService;

@ExtendWith(MockitoExtension.class)
class BulkPaymentControllerTest {

    @Mock private BulkPaymentService bulkPaymentService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private BulkPaymentResponseDTO sampleResponse;

    @BeforeEach
    void setUp() {
        BulkPaymentController controller = new BulkPaymentController(bulkPaymentService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        sampleResponse = new BulkPaymentResponseDTO(
            1L, "BP1001", "111122223333", 1, 0, 0, "CREATED", BigDecimal.valueOf(100),
            LocalDateTime.now(), null, List.of()
        );
    }

    @Test
    @DisplayName("POST /api/bulk-payments/validate-csv: Validates uploaded CSV file")
    void validateCSV_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "payments.csv", "text/csv", "destinationAccount,amount\n444455556666,100".getBytes());

        CSVValidationResultDTO resultDTO = new CSVValidationResultDTO(1, 1, 0, true, List.of());
        when(bulkPaymentService.validateCSVUpload(any())).thenReturn(resultDTO);

        mockMvc.perform(multipart("/api/bulk-payments/validate-csv").file(file))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isValid").value(true))
            .andExpect(jsonPath("$.validRecords").value(1));
    }

    @Test
    @DisplayName("POST /api/bulk-payments: Creates bulk payment batch")
    void createBulkPayment_Success() throws Exception {
        CreateBulkPaymentRequest req = new CreateBulkPaymentRequest(
            "111122223333", "1234",
            List.of(new BulkPaymentItemDTO("444455556666", BigDecimal.valueOf(100), "USD", "Payment 1")),
            "IDEM_BULK"
        );

        when(bulkPaymentService.createBulkPayment(any(), eq("DEMO_USER"))).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/bulk-payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.batchReference").value("BP1001"));
    }

    @Test
    @DisplayName("GET /api/bulk-payments/1: Returns batch details")
    void getBatchDetails_Success() throws Exception {
        when(bulkPaymentService.getBatchDetails(1L)).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/bulk-payments/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.batchId").value(1));
    }
}
