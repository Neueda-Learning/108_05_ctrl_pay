package com.neueda.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Request DTO for creating an account under an existing customer.
 */
public record CreateAccountRequest(
    @NotBlank(message = "Account number is required")
    @Pattern(regexp = "^[0-9]{12}$", message = "Account number must be exactly 12 digits")
    String accountNumber,

    @NotBlank(message = "Account name is required")
    String accountName,

    @NotNull(message = "Account balance is required")
    @PositiveOrZero(message = "Account balance cannot be negative")
    BigDecimal accountBalance,

    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-character ISO 4217 code")
    String currency,

    @NotNull(message = "Account opening date is required")
    @PastOrPresent(message = "Account opening date cannot be in the future")
    LocalDate accountOpeningDate,

    @NotBlank(message = "IFSC code is required")
    @Pattern(regexp = "^[A-Z]{4}0[A-Z0-9]{6}$", message = "IFSC code must be a valid 11-character code")
    String ifscCode,

    @NotBlank(message = "Account location is required")
    String accountLocation,

    @NotBlank(message = "Bank name is required")
    String bankName,

    @NotBlank(message = "Account PIN is required")
    @Pattern(regexp = "^[0-9]{4}$", message = "Account PIN must be exactly 4 digits")
    String accountPin
) {
}

