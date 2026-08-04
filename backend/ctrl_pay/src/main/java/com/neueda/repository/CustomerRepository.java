package com.neueda.repository;

import java.util.Optional;

import com.neueda.domain.CustomerRecord;

/**
 * Repository interface for customer profile data access operations.
 */
public interface CustomerRepository {

    /**
     * Insert a new customer profile.
     */
    CustomerRecord save(CustomerRecord customer);

    /**
     * Retrieve customer profile by ID.
     */
    Optional<CustomerRecord> findById(Long customerId);

    /**
     * Retrieve customer profile by PAN number.
     */
    Optional<CustomerRecord> findByPanNumber(String panNumber);

    /**
     * Check whether a PAN already has a customer profile.
     */
    boolean existsByPanNumber(String panNumber);
}

