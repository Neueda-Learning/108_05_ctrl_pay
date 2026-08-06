package com.neueda.controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.neueda.domain.AccountRecord;
import com.neueda.domain.CustomerRecord;
import com.neueda.domain.PaymentRecord;
import com.neueda.domain.PaymentStatus;
import com.neueda.dto.AccountDetailsDTO;
import com.neueda.dto.BulkPaymentSummaryDTO;
import com.neueda.dto.CreateCustomerRequest;
import com.neueda.dto.CustomerPaymentStatisticsDTO;
import com.neueda.dto.CustomerProfileDTO;
import com.neueda.dto.CustomerResponse;
import com.neueda.dto.CustomerRiskDTO;
import com.neueda.dto.AccountSummaryDTO;
import com.neueda.dto.PaymentResponse;
import com.neueda.dto.TransactionSummaryDTO;
import com.neueda.exception.CustomerNotFoundException;
import com.neueda.service.AccountService;
import com.neueda.service.CustomerProfileService;
import com.neueda.service.CustomerService;
import com.neueda.service.FraudRiskService;
import com.neueda.service.PaymentService;

import jakarta.validation.Valid;

/**
 * REST controller for customer profile onboarding and retrieval.
 */
@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;
    private final PaymentService paymentService;
    private final AccountService accountService;
    private final FraudRiskService fraudRiskService;
    private final CustomerProfileService customerProfileService;

    public CustomerController(
        CustomerService customerService,
        PaymentService paymentService,
        AccountService accountService,
        FraudRiskService fraudRiskService,
        CustomerProfileService customerProfileService
    ) {
        this.customerService = customerService;
        this.paymentService = paymentService;
        this.accountService = accountService;
        this.fraudRiskService = fraudRiskService;
        this.customerProfileService = customerProfileService;
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

    /**
     * Get all payments for a customer's accounts.
     * 
     * Request: GET /api/customers/{customerId}/payments?status=COMPLETED&account=123456789012&date-from=2026-07-01&date-to=2026-07-31&limit=10&offset=0
     * 
     * Query Parameters:
     * - status (optional): Filter by payment status (CREATED, VALIDATED, SENT, COMPLETED, FAILED)
     * - account (optional): Filter by specific account (12-digit account number)
     * - date-from (optional): Filter by created_at >= this timestamp (ISO 8601 format)
     * - date-to (optional): Filter by created_at <= this timestamp (ISO 8601 format)
     * - limit (optional, default 10): Max results to return
     * - offset (optional, default 0): Pagination offset
     * 
     * Response:
     * - 200 OK: List of payments for customer's accounts
     * - 404 Not Found: Customer does not exist
     * - 500 Internal Server Error: Server error
     * 
     * @param customerId customer ID
     * @param status optional status filter
     * @param account optional account filter
     * @param dateFrom optional date from filter
     * @param dateTo optional date to filter
     * @param limit max results
     * @param offset pagination offset
     * @return 200 OK with list of payments
     */
    @GetMapping("/{customerId}/payments")
    public ResponseEntity<List<PaymentResponse>> getCustomerPayments(
        @PathVariable Long customerId,
        @RequestParam(required = false) PaymentStatus status,
        @RequestParam(required = false) String account,
        @RequestParam(name = "date-from", required = false) String dateFrom,
        @RequestParam(name = "date-to", required = false) String dateTo,
        @RequestParam(defaultValue = "10") int limit,
        @RequestParam(defaultValue = "0") int offset
    ) {
        try {
            // Verify customer exists
            customerService.getCustomerById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found: " + customerId));
            
            // Validate pagination parameters
            if (limit <= 0 || limit > 1000) {
                throw new IllegalArgumentException("Limit must be between 1 and 1000");
            }
            if (offset < 0) {
                throw new IllegalArgumentException("Offset must be >= 0");
            }
            
            // Parse date parameters if provided
            LocalDateTime dateFromParsed = null;
            LocalDateTime dateToParsed = null;
            
            if (dateFrom != null && !dateFrom.isEmpty()) {
                try {
                    dateFromParsed = LocalDateTime.parse(dateFrom, DateTimeFormatter.ISO_DATE_TIME);
                } catch (Exception e) {
                    throw new IllegalArgumentException("Invalid date-from format. Use ISO 8601 (e.g., 2026-07-01T00:00:00)");
                }
            }
            
            if (dateTo != null && !dateTo.isEmpty()) {
                try {
                    dateToParsed = LocalDateTime.parse(dateTo, DateTimeFormatter.ISO_DATE_TIME);
                } catch (Exception e) {
                    throw new IllegalArgumentException("Invalid date-to format. Use ISO 8601 (e.g., 2026-07-31T23:59:59)");
                }
            }
            
            // Get customer's accounts
            List<AccountRecord> customerAccounts = accountService.getAccountsByCustomerId(customerId);
            
            // Build list of customer's account numbers for filtering
            List<String> customerAccountNumbers = customerAccounts.stream()
                .map(AccountRecord::accountNumber)
                .toList();
            
            // If specific account filter provided, verify it belongs to customer
            if (account != null && !account.isEmpty()) {
                if (!customerAccountNumbers.contains(account)) {
                    // Return empty list - customer doesn't have access to this account
                    return ResponseEntity.ok(List.of());
                }
            }
            
            // Query all payments with filters
            List<PaymentRecord> allPayments = paymentService.listPaymentsFiltered(
                status,
                account,
                null, // no currency filter
                dateFromParsed,
                dateToParsed,
                null, // no failed rule filter
                limit,
                offset
            );
            
            // Filter to only include payments where source or destination is customer's account
            List<PaymentResponse> responses = allPayments.stream()
                .filter(payment -> 
                    customerAccountNumbers.contains(payment.sourceAccount()) ||
                    customerAccountNumbers.contains(payment.destinationAccount())
                )
                .map(this::toPaymentResponse)
                .toList();
            
            return ResponseEntity.ok(responses);
            
        } catch (CustomerNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new com.neueda.exception.PaymentProcessingException("Error retrieving customer payments: " + e.getMessage());
        }
    }

    /**
     * Get payment statistics for a customer.
     * 
     * Request: GET /api/customers/{customerId}/statistics
     * 
     * Response:
     * - 200 OK: Statistics summary
     * - 404 Not Found: Customer does not exist
     * - 500 Internal Server Error: Server error
     * 
     * @param customerId customer ID
     * @return 200 OK with statistics
     */
    @GetMapping("/{customerId}/statistics")
    public ResponseEntity<com.neueda.dto.CustomerStatisticsResponse> getCustomerStatistics(
        @PathVariable Long customerId
    ) {
        try {
            // Verify customer exists
            customerService.getCustomerById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found: " + customerId));
            
            // Get customer's accounts
            List<AccountRecord> customerAccounts = accountService.getAccountsByCustomerId(customerId);
            List<String> customerAccountNumbers = customerAccounts.stream()
                .map(AccountRecord::accountNumber)
                .toList();
            
            // Get all payments for customer
            List<PaymentRecord> allPayments = paymentService.listPaymentsFiltered(
                null, // no status filter
                null, // no account filter
                null, // no currency filter
                null, // no date filter
                null,
                null, // no failed rule filter
                10000, // large limit to get all
                0
            );
            
            // Filter to customer's accounts
            List<PaymentRecord> customerPayments = allPayments.stream()
                .filter(payment -> 
                    customerAccountNumbers.contains(payment.sourceAccount()) ||
                    customerAccountNumbers.contains(payment.destinationAccount())
                )
                .toList();
            
            // Calculate statistics
            long total = customerPayments.size();
            long completed = customerPayments.stream()
                .filter(p -> p.status() == PaymentStatus.COMPLETED)
                .count();
            long failed = customerPayments.stream()
                .filter(p -> p.status() == PaymentStatus.FAILED)
                .count();
            long pending = total - completed - failed;
            
            com.neueda.dto.CustomerStatisticsResponse stats = new com.neueda.dto.CustomerStatisticsResponse(
                customerId,
                total,
                completed,
                failed,
                pending,
                (long) customerAccounts.size()
            );
            
            return ResponseEntity.ok(stats);
            
         } catch (CustomerNotFoundException e) {
             throw e;
         } catch (Exception e) {
             throw new com.neueda.exception.PaymentProcessingException("Error retrieving customer statistics: " + e.getMessage());
         }
     }


    // =========================================================================
    // CUSTOMER PROFILE SECTION ENDPOINTS
    // =========================================================================

    /**
     * GET /api/customers/{customerId}/profile
     * Retrieve complete customer profile information.
     */
    @GetMapping("/{customerId}/profile")
    public ResponseEntity<CustomerProfileDTO> getProfile(@PathVariable Long customerId) {
        try {
            CustomerProfileDTO profile = customerProfileService.getCustomerProfile(customerId);
            return ResponseEntity.ok(profile);
        } catch (CustomerNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new com.neueda.exception.ServiceException("Error retrieving customer profile: " + e.getMessage(), "PROFILE_RETRIEVAL_ERROR");
        }
    }

    /**
     * GET /api/customers/{customerId}/profile/accounts
     * Retrieve all accounts for a customer.
     */
    @GetMapping("/{customerId}/profile/accounts")
    public ResponseEntity<List<AccountSummaryDTO>> getProfileAccounts(@PathVariable Long customerId) {
        try {
            List<AccountSummaryDTO> accounts = customerProfileService.getCustomerAccounts(customerId);
            return ResponseEntity.ok(accounts);
        } catch (CustomerNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new com.neueda.exception.ServiceException("Error retrieving customer accounts: " + e.getMessage(), "ACCOUNTS_RETRIEVAL_ERROR");
        }
    }

    /**
     * GET /api/customers/{customerId}/profile/accounts/{accountNumber}
     * Retrieve detailed information for a specific account.
     *
     * Response: AccountDetailsDTO with account info, balance, recent transactions, and statistics.
     */
    @GetMapping("/{customerId}/profile/accounts/{accountNumber}")
    public ResponseEntity<AccountDetailsDTO> getAccountDetails(
        @PathVariable Long customerId,
        @PathVariable String accountNumber
    ) {
        try {
            AccountDetailsDTO accountDetails = customerProfileService.getAccountDetails(customerId, accountNumber);
            return ResponseEntity.ok(accountDetails);
        } catch (CustomerNotFoundException e) {
            throw e;
        } catch (com.neueda.exception.AccountValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new com.neueda.exception.ServiceException("Error retrieving account details: " + e.getMessage(), "ACCOUNT_DETAILS_ERROR");
        }
    }

    /**
     * GET /api/customers/{customerId}/profile/transactions
     * Retrieve paginated transaction history for customer.
     *
     * Query Parameters:
     * - status (optional): Filter by payment status
     * - account (optional): Filter by specific account
     * - dateFrom (optional): Filter by start date (ISO 8601)
     * - dateTo (optional): Filter by end date (ISO 8601)
     * - page (default 0): Page number for pagination
     * - pageSize (default 10): Number of results per page
     */
    @GetMapping("/{customerId}/profile/transactions")
    public ResponseEntity<List<TransactionSummaryDTO>> getProfileTransactions(
        @PathVariable Long customerId,
        @RequestParam(required = false) PaymentStatus status,
        @RequestParam(required = false) String account,
        @RequestParam(name = "dateFrom", required = false) String dateFrom,
        @RequestParam(name = "dateTo", required = false) String dateTo,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int pageSize
    ) {
        try {
            // Validate pagination parameters
            if (pageSize <= 0 || pageSize > 100) {
                throw new IllegalArgumentException("Page size must be between 1 and 100");
            }
            if (page < 0) {
                throw new IllegalArgumentException("Page must be >= 0");
            }

            // Parse date parameters if provided
            LocalDateTime dateFromParsed = null;
            LocalDateTime dateToParsed = null;

            if (dateFrom != null && !dateFrom.isEmpty()) {
                try {
                    dateFromParsed = LocalDateTime.parse(dateFrom, DateTimeFormatter.ISO_DATE_TIME);
                } catch (Exception e) {
                    throw new IllegalArgumentException("Invalid dateFrom format. Use ISO 8601 (e.g., 2026-07-01T00:00:00)");
                }
            }

            if (dateTo != null && !dateTo.isEmpty()) {
                try {
                    dateToParsed = LocalDateTime.parse(dateTo, DateTimeFormatter.ISO_DATE_TIME);
                } catch (Exception e) {
                    throw new IllegalArgumentException("Invalid dateTo format. Use ISO 8601 (e.g., 2026-07-31T23:59:59)");
                }
            }

            List<TransactionSummaryDTO> transactions = customerProfileService.getCustomerTransactions(
                customerId,
                status,
                account,
                dateFromParsed,
                dateToParsed,
                page,
                pageSize
            );
            return ResponseEntity.ok(transactions);

        } catch (CustomerNotFoundException e) {
            throw e;
        } catch (com.neueda.exception.AccountValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new com.neueda.exception.ServiceException("Error retrieving transactions: " + e.getMessage(), "TRANSACTIONS_RETRIEVAL_ERROR");
        }
    }

    /**
     * GET /api/customers/{customerId}/profile/payment-statistics
     * Retrieve payment statistics for customer.
     */
    @GetMapping("/{customerId}/profile/payment-statistics")
    public ResponseEntity<CustomerPaymentStatisticsDTO> getProfilePaymentStatistics(@PathVariable Long customerId) {
        try {
            CustomerPaymentStatisticsDTO stats = customerProfileService.getPaymentStatistics(customerId);
            return ResponseEntity.ok(stats);
        } catch (CustomerNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new com.neueda.exception.ServiceException("Error retrieving payment statistics: " + e.getMessage(), "STATISTICS_RETRIEVAL_ERROR");
        }
    }

    /**
     * GET /api/customers/{customerId}/profile/risk
     * Retrieve risk and security information for customer.
     */
    @GetMapping("/{customerId}/profile/risk")
    public ResponseEntity<CustomerRiskDTO> getProfileRiskInformation(@PathVariable Long customerId) {
        try {
            CustomerRiskDTO risk = customerProfileService.getCustomerRiskInformation(customerId);
            return ResponseEntity.ok(risk);
        } catch (CustomerNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new com.neueda.exception.ServiceException("Error retrieving risk information: " + e.getMessage(), "RISK_RETRIEVAL_ERROR");
        }
    }

    /**
     * GET /api/customers/{customerId}/profile/bulk-payments
     * Retrieve bulk payment batches for customer.
     *
     * Query Parameters:
     * - page (default 0): Page number for pagination
     * - pageSize (default 10): Number of results per page
     * 
     * Note: Returns empty list if bulk payments data is unavailable (graceful degradation).
     */
    @GetMapping("/{customerId}/profile/bulk-payments")
    public ResponseEntity<List<BulkPaymentSummaryDTO>> getProfileBulkPayments(
        @PathVariable Long customerId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int pageSize
    ) {
        try {
            // Validate pagination parameters
            if (pageSize <= 0 || pageSize > 100) {
                throw new IllegalArgumentException("Page size must be between 1 and 100");
            }
            if (page < 0) {
                throw new IllegalArgumentException("Page must be >= 0");
            }

            List<BulkPaymentSummaryDTO> bulkPayments = customerProfileService.getCustomerBulkPayments(
                customerId,
                page,
                pageSize
            );
            return ResponseEntity.ok(bulkPayments);

        } catch (CustomerNotFoundException e) {
            throw e;
        } catch (Exception e) {
            // Return empty list gracefully - allows profile to load even if bulk payments feature has issues
            return ResponseEntity.ok(List.of());
        }
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

    private PaymentResponse toPaymentResponse(PaymentRecord payment) {
        FraudRiskService.PaymentRisk paymentRisk = fraudRiskService.assessPaymentRisk(payment);

        // Fetch validation results from service
        var validationResults = paymentService.getValidationResults(payment.id()).stream()
            .map(vr -> new com.neueda.dto.ValidationResultResponse(
                vr.validationRuleId(),
                vr.ruleName(),
                vr.passed(),
                vr.errorCode(),
                vr.errorMessage(),
                vr.executionTimeMs()
            ))
            .toList();
        
        return new PaymentResponse(
            payment.id(),
            payment.idempotencyKey(),
            payment.sourceAccount(),
            payment.destinationAccount(),
            payment.amount(),
            payment.currency(),
            payment.status(),
            payment.errorCode(),
            payment.errorMessage(),
            payment.sourceAmount(),
            payment.destinationAmount(),
            payment.exchangeRate(),
            payment.createdAt(),
            payment.updatedAt(),
            paymentRisk.fraudProbability(),
            paymentRisk.highRisk(),
            validationResults
        );
    }
}


