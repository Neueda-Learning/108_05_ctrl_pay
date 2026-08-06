package com.neueda.service;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.neueda.domain.CustomerRecord;
import com.neueda.domain.CustomerStatus;
import com.neueda.exception.CustomerValidationException;
import com.neueda.repository.CustomerRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    private CustomerService customerService;

    private CustomerRecord sampleCustomer;

    @BeforeEach
    void setUp() {
        customerService = new CustomerService(customerRepository);
        sampleCustomer = new CustomerRecord(
            1L, "Alice Smith", LocalDate.of(1990, 1, 1), "+1234567890", "ABCDE1234F",
            java.time.LocalDateTime.now(), java.time.LocalDateTime.now(), "US", CustomerStatus.ACTIVE
        );
    }

    @Test
    @DisplayName("createCustomer: Should normalize PAN and save customer")
    void createCustomer_Success() {
        CustomerRecord request = new CustomerRecord(
            null, "Alice Smith", LocalDate.of(1990, 1, 1), "+1234567890", " abcde1234f ",
            null, null, "US", CustomerStatus.ACTIVE
        );

        when(customerRepository.existsByPanNumber("ABCDE1234F")).thenReturn(false);
        when(customerRepository.save(any(CustomerRecord.class))).thenReturn(sampleCustomer);

        CustomerRecord result = customerService.createCustomer(request);

        assertThat(result).isNotNull();
        assertThat(result.customerId()).isEqualTo(1L);
        verify(customerRepository).save(any(CustomerRecord.class));
    }

    @Test
    @DisplayName("createCustomer: Should throw CustomerValidationException if PAN exists")
    void createCustomer_DuplicatePan() {
        CustomerRecord request = new CustomerRecord(
            null, "Alice Smith", LocalDate.of(1990, 1, 1), "+1234567890", "ABCDE1234F",
            null, null, "US", CustomerStatus.ACTIVE
        );

        when(customerRepository.existsByPanNumber("ABCDE1234F")).thenReturn(true);

        assertThatThrownBy(() -> customerService.createCustomer(request))
            .isInstanceOf(CustomerValidationException.class)
            .hasMessageContaining("Customer profile already exists for PAN");
    }

    @Test
    @DisplayName("getCustomerById: Should return customer when found")
    void getCustomerById_Found() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));

        Optional<CustomerRecord> result = customerService.getCustomerById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().name()).isEqualTo("Alice Smith");
    }

    @Test
    @DisplayName("getCustomerByPan: Should normalize PAN and return customer")
    void getCustomerByPan_Found() {
        when(customerRepository.findByPanNumber("ABCDE1234F")).thenReturn(Optional.of(sampleCustomer));

        Optional<CustomerRecord> result = customerService.getCustomerByPan(" abcde1234f ");

        assertThat(result).isPresent();
        assertThat(result.get().panNumber()).isEqualTo("ABCDE1234F");
    }
}
