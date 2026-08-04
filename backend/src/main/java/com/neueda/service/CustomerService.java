package com.neueda.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.neueda.domain.CustomerRecord;
import com.neueda.exception.CustomerValidationException;
import com.neueda.repository.CustomerRepository;

/**
 * Service layer for customer profile operations.
 */
@Service
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    /**
     * Create a customer profile. PAN is unique and allows only one profile per customer.
     */
    public CustomerRecord createCustomer(CustomerRecord customer) {
        String normalizedPan = normalizePan(customer.panNumber());
        if (customerRepository.existsByPanNumber(normalizedPan)) {
            throw new CustomerValidationException(
                "Customer profile already exists for PAN: " + normalizedPan,
                "DUPLICATE_CUSTOMER_PROFILE"
            );
        }

        CustomerRecord normalized = new CustomerRecord(
            null,
            customer.name(),
            customer.dob(),
            customer.phoneNumber(),
            normalizedPan,
            customer.profileCreated(),
            customer.lastUpdated(),
            customer.country(),
            customer.customerAccountStatus()
        );

        return customerRepository.save(normalized);
    }

    public Optional<CustomerRecord> getCustomerById(Long customerId) {
        return customerRepository.findById(customerId);
    }

    public Optional<CustomerRecord> getCustomerByPan(String panNumber) {
        return customerRepository.findByPanNumber(normalizePan(panNumber));
    }

    private String normalizePan(String panNumber) {
        return panNumber == null ? null : panNumber.trim().toUpperCase();
    }
}

