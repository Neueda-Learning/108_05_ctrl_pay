package com.neueda.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;

/**
 * Request DTO for creating a customer profile.
 */
public record CreateCustomerRequest(
    @NotBlank(message = "Customer name is required")
    String name,

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    LocalDate dob,

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[0-9+() -]{7,20}$", message = "Phone number format is invalid")
    String phoneNumber,

    @NotBlank(message = "PAN number is required")
    @Pattern(regexp = "^[A-Za-z]{5}[0-9]{4}[A-Za-z]{1}$", message = "PAN number must be a valid 10-character PAN")
    String panNumber,

    @NotBlank(message = "Country is required")
    String country
) {
}

