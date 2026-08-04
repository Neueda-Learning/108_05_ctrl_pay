package com.neueda.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.neueda.domain.CustomerRecord;
import com.neueda.dto.CreateCustomerRequest;
import com.neueda.dto.CustomerResponse;
import com.neueda.exception.CustomerNotFoundException;
import com.neueda.service.CustomerService;

import jakarta.validation.Valid;

/**
 * REST controller for customer profile onboarding and retrieval.
 */
@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    /**
     * Create a new customer profile. PAN uniqueness ensures one profile per customer.
     */
    @PostMapping
    public ResponseEntity<CustomerResponse> createCustomer(@Valid @RequestBody CreateCustomerRequest request) {
        CustomerRecord toCreate = CustomerRecord.create(
            request.name(),
            request.dob(),
            request.phoneNumber(),
            request.panNumber(),
            request.country()
        );

        CustomerRecord saved = customerService.createCustomer(toCreate);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
    }

    /**
     * Retrieve customer profile by ID.
     */
    @GetMapping("/{customerId}")
    public ResponseEntity<CustomerResponse> getCustomer(@PathVariable Long customerId) {
        CustomerRecord customer = customerService.getCustomerById(customerId)
            .orElseThrow(() -> new CustomerNotFoundException("Customer not found: " + customerId));
        return ResponseEntity.ok(toResponse(customer));
    }

    private CustomerResponse toResponse(CustomerRecord customer) {
        return new CustomerResponse(
            customer.customerId(),
            customer.name(),
            customer.dob(),
            customer.phoneNumber(),
            customer.panNumber(),
            customer.profileCreated(),
            customer.lastUpdated(),
            customer.country(),
            customer.customerAccountStatus()
        );
    }
}

