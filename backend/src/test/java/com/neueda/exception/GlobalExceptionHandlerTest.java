package com.neueda.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;

import com.neueda.dto.ErrorResponse;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @Mock
    private WebRequest webRequest;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        when(webRequest.getDescription(false)).thenReturn("uri=/api/test");
    }

    @Test
    @DisplayName("handlePaymentValidationException: Returns 400 Bad Request")
    void handlePaymentValidationException() {
        PaymentValidationException ex = new PaymentValidationException("Invalid payment", "INVALID_PAYMENT");

        ResponseEntity<ErrorResponse> response = handler.handlePaymentValidationException(ex, webRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("INVALID_PAYMENT", response.getBody().errorCode());
    }

    @Test
    @DisplayName("handlePaymentNotFoundException: Returns 404 Not Found")
    void handlePaymentNotFoundException() {
        PaymentNotFoundException ex = new PaymentNotFoundException("Payment not found");

        ResponseEntity<ErrorResponse> response = handler.handlePaymentNotFoundException(ex, webRequest);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("PAYMENT_NOT_FOUND", response.getBody().errorCode());
    }

    @Test
    @DisplayName("handleCustomerValidationException: Returns 400 Bad Request")
    void handleCustomerValidationException() {
        CustomerValidationException ex = new CustomerValidationException("Invalid customer", "INVALID_CUSTOMER");

        ResponseEntity<ErrorResponse> response = handler.handleCustomerValidationException(ex, webRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    @DisplayName("handleCustomerNotFoundException: Returns 404 Not Found")
    void handleCustomerNotFoundException() {
        CustomerNotFoundException ex = new CustomerNotFoundException("Customer not found");

        ResponseEntity<ErrorResponse> response = handler.handleCustomerNotFoundException(ex, webRequest);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    @DisplayName("handleAccountValidationException: Returns 400 Bad Request")
    void handleAccountValidationException() {
        AccountValidationException ex = new AccountValidationException("Invalid account", "INVALID_ACCOUNT");

        ResponseEntity<ErrorResponse> response = handler.handleAccountValidationException(ex, webRequest);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    @DisplayName("handleAccountNotFoundException: Returns 404 Not Found")
    void handleAccountNotFoundException() {
        AccountNotFoundException ex = new AccountNotFoundException("Account not found");

        ResponseEntity<ErrorResponse> response = handler.handleAccountNotFoundException(ex, webRequest);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    @DisplayName("handleGlobalException: Returns 500 Internal Server Error for unhandled exceptions")
    void handleGlobalException() {
        Exception ex = new RuntimeException("Unexpected error");

        ResponseEntity<ErrorResponse> response = handler.handleGlobalException(ex, webRequest);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("INTERNAL_ERROR", response.getBody().errorCode());
    }
}
