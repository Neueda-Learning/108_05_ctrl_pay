package com.neueda.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.neueda.domain.AccountRecord;
import com.neueda.domain.BulkPaymentBatchRecord;
import com.neueda.domain.CustomerRecord;
import com.neueda.domain.FraudAssessmentRecord;
import com.neueda.domain.FraudDecision;
import com.neueda.domain.PaymentRecord;
import com.neueda.domain.PaymentStatus;
import com.neueda.dto.AccountDetailsDTO;
import com.neueda.dto.AccountSummaryDTO;
import com.neueda.dto.BulkPaymentSummaryDTO;
import com.neueda.dto.CustomerPaymentStatisticsDTO;
import com.neueda.dto.CustomerProfileDTO;
import com.neueda.dto.CustomerRiskDTO;
import com.neueda.dto.TransactionSummaryDTO;
import com.neueda.exception.CustomerNotFoundException;
import com.neueda.repository.AccountRepository;
import com.neueda.repository.BulkPaymentBatchRepository;
import com.neueda.repository.CustomerRepository;
import com.neueda.repository.FraudAssessmentRepository;
import com.neueda.repository.PaymentRepository;

/**
 * Service for customer profile management.
 * Orchestrates data from multiple repositories to provide complete customer profile information.
 */
@Service
@Transactional(readOnly = true)
public class CustomerProfileService {

    private final CustomerRepository customerRepository;
    private final AccountRepository accountRepository;
    private final PaymentRepository paymentRepository;
    private final FraudAssessmentRepository fraudAssessmentRepository;
    private final BulkPaymentBatchRepository bulkPaymentBatchRepository;

    public CustomerProfileService(
        CustomerRepository customerRepository,
        AccountRepository accountRepository,
        PaymentRepository paymentRepository,
        FraudAssessmentRepository fraudAssessmentRepository,
        BulkPaymentBatchRepository bulkPaymentBatchRepository
    ) {
        this.customerRepository = customerRepository;
        this.accountRepository = accountRepository;
        this.paymentRepository = paymentRepository;
        this.fraudAssessmentRepository = fraudAssessmentRepository;
        this.bulkPaymentBatchRepository = bulkPaymentBatchRepository;
    }

    /**
     * Get complete customer profile information.
     */
    public CustomerProfileDTO getCustomerProfile(Long customerId) {
        CustomerRecord customer = customerRepository.findById(customerId)
            .orElseThrow(() -> new CustomerNotFoundException("Customer not found: " + customerId));

        return new CustomerProfileDTO(
            customer.customerId(),
            customer.name(),
            customer.dob(),
            customer.phoneNumber(),
            customer.panNumber(),
            customer.country(),
            customer.customerAccountStatus(),
            customer.profileCreated(),
            customer.lastUpdated()
        );
    }

    /**
     * Get all accounts for a customer with summary information.
     */
    public List<AccountSummaryDTO> getCustomerAccounts(Long customerId) {
        // Verify customer exists
        if (customerRepository.findById(customerId).isEmpty()) {
            throw new CustomerNotFoundException("Customer not found: " + customerId);
        }

        List<AccountRecord> accounts = accountRepository.findByCustomerId(customerId);
        
        return accounts.stream()
            .map(account -> new AccountSummaryDTO(
                account.accountId(),
                account.accountNumber(),
                account.accountName(),
                "SAVINGS", // Default type - can be extended if needed
                account.currency(),
                account.accountBalance(),
                account.accountBalance(), // Available = current for now
                account.accountStatus(),
                account.accountOpeningDate()
            ))
            .collect(Collectors.toList());
    }

    /**
     * Get detailed information for a specific account including recent transactions.
     */
    public AccountDetailsDTO getAccountDetails(Long customerId, String accountNumber) {
        // Verify customer exists
        if (customerRepository.findById(customerId).isEmpty()) {
            throw new CustomerNotFoundException("Customer not found: " + customerId);
        }

        // Verify customer owns this account
        AccountRecord account = accountRepository.findByAccountNumber(accountNumber)
            .orElseThrow(() -> new com.neueda.exception.AccountValidationException(
                "Account not found: " + accountNumber,
                "ACCOUNT_NOT_FOUND"
            ));

        if (!account.customerId().equals(customerId)) {
            throw new com.neueda.exception.AccountValidationException(
                "Customer does not own this account",
                "UNAUTHORIZED_ACCOUNT_ACCESS"
            );
        }

        // Get recent transactions for this account
        List<PaymentRecord> allPayments = paymentRepository.findAll();
        List<TransactionSummaryDTO> recentTransactions = allPayments.stream()
            .filter(p -> p.sourceAccount().equals(accountNumber) || p.destinationAccount().equals(accountNumber))
            .filter(p -> p.status() == PaymentStatus.COMPLETED)
            .sorted((p1, p2) -> p2.createdAt().compareTo(p1.createdAt()))
            .limit(10)
            .map(p -> new TransactionSummaryDTO(
                p.id(),
                p.sourceAccount(),
                p.destinationAccount(),
                p.amount(),
                p.currency(),
                p.status(),
                p.createdAt(),
                p.errorCode()
            ))
            .collect(Collectors.toList());

        // Get last payment date and transaction count
        LocalDateTime lastPaymentDate = recentTransactions.isEmpty() ? null : recentTransactions.get(0).transactionDate();
        long transactionCount = allPayments.stream()
            .filter(p -> (p.sourceAccount().equals(accountNumber) || p.destinationAccount().equals(accountNumber))
                && p.status() == PaymentStatus.COMPLETED)
            .count();

        return new AccountDetailsDTO(
            account.accountId(),
            account.accountNumber(),
            account.accountName(),
            "SAVINGS",
            account.currency(),
            account.accountOpeningDate(),
            account.accountStatus(),
            account.ifscCode(),
            account.bankName(),
            account.accountLocation(),
            account.accountBalance(),
            account.accountBalance(), // Available balance = current for now
            BigDecimal.ZERO,
            recentTransactions,
            lastPaymentDate,
            transactionCount
        );
    }

    /**
     * Get paginated transaction history for a customer.
     */
    public List<TransactionSummaryDTO> getCustomerTransactions(
        Long customerId,
        PaymentStatus status,
        String accountFilter,
        LocalDateTime dateFrom,
        LocalDateTime dateTo,
        int page,
        int pageSize
    ) {
        // Verify customer exists
        if (customerRepository.findById(customerId).isEmpty()) {
            throw new CustomerNotFoundException("Customer not found: " + customerId);
        }

        // Get customer's account numbers for filtering
        List<AccountRecord> customerAccounts = accountRepository.findByCustomerId(customerId);
        List<String> customerAccountNumbers = customerAccounts.stream()
            .map(AccountRecord::accountNumber)
            .collect(Collectors.toList());

        if (customerAccountNumbers.isEmpty()) {
            return List.of();
        }

        // If specific account filter provided, verify it belongs to customer
        if (accountFilter != null && !accountFilter.isEmpty()) {
            if (!customerAccountNumbers.contains(accountFilter)) {
                throw new com.neueda.exception.AccountValidationException(
                    "Customer does not own this account",
                    "UNAUTHORIZED_ACCOUNT_ACCESS"
                );
            }
        }

        // Get paginated payments
        List<PaymentRecord> allPayments = paymentRepository.findAllFiltered(
            status,
            accountFilter,
            null, // no currency filter
            dateFrom,
            dateTo,
            null, // no failed rule filter
            pageSize,
            page * pageSize
        );

        // Filter to only customer's accounts and convert to DTOs
        return allPayments.stream()
            .filter(payment ->
                customerAccountNumbers.contains(payment.sourceAccount()) ||
                customerAccountNumbers.contains(payment.destinationAccount())
            )
            .map(p -> new TransactionSummaryDTO(
                p.id(),
                p.sourceAccount(),
                p.destinationAccount(),
                p.amount(),
                p.currency(),
                p.status(),
                p.createdAt(),
                p.errorCode()
            ))
            .collect(Collectors.toList());
    }

    /**
     * Get payment statistics for a customer.
     */
    public CustomerPaymentStatisticsDTO getPaymentStatistics(Long customerId) {
        // Verify customer exists
        if (customerRepository.findById(customerId).isEmpty()) {
            throw new CustomerNotFoundException("Customer not found: " + customerId);
        }

        // Get customer's account numbers
        List<AccountRecord> customerAccounts = accountRepository.findByCustomerId(customerId);
        List<String> customerAccountNumbers = customerAccounts.stream()
            .map(AccountRecord::accountNumber)
            .collect(Collectors.toList());

        if (customerAccountNumbers.isEmpty()) {
            return new CustomerPaymentStatisticsDTO(0L, 0L, 0L, 0L, BigDecimal.ZERO, BigDecimal.ZERO);
        }

        // Get all payments and filter to customer's accounts
        List<PaymentRecord> allPayments = paymentRepository.findAll();
        List<PaymentRecord> customerPayments = allPayments.stream()
            .filter(p ->
                customerAccountNumbers.contains(p.sourceAccount()) ||
                customerAccountNumbers.contains(p.destinationAccount())
            )
            .collect(Collectors.toList());

        long total = customerPayments.size();
        long successful = customerPayments.stream()
            .filter(p -> p.status() == PaymentStatus.COMPLETED)
            .count();
        long failed = customerPayments.stream()
            .filter(p -> p.status() == PaymentStatus.FAILED)
            .count();

        // Count rejected payments from fraud assessments
        long rejected = customerPayments.stream()
            .filter(p -> {
                // Check if this payment has a fraud assessment with REJECTED decision
                var assessment = fraudAssessmentRepository.findByPaymentId(p.id());
                return assessment.isPresent() && assessment.get().decision() == FraudDecision.REJECTED;
            })
            .count();

        // Calculate total amount
        BigDecimal totalAmount = customerPayments.stream()
            .map(PaymentRecord::amount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate average transaction amount
        BigDecimal averageAmount = total > 0 ? 
            totalAmount.divide(new BigDecimal(total), 2, java.math.RoundingMode.HALF_UP) : 
            BigDecimal.ZERO;

        return new CustomerPaymentStatisticsDTO(
            total,
            successful,
            failed,
            rejected,
            totalAmount,
            averageAmount
        );
    }

    /**
     * Get risk and security information for a customer.
     */
    public CustomerRiskDTO getCustomerRiskInformation(Long customerId) {
        // Verify customer exists
        if (customerRepository.findById(customerId).isEmpty()) {
            throw new CustomerNotFoundException("Customer not found: " + customerId);
        }

        // Get customer's account numbers
        List<AccountRecord> customerAccounts = accountRepository.findByCustomerId(customerId);
        List<String> customerAccountNumbers = customerAccounts.stream()
            .map(AccountRecord::accountNumber)
            .collect(Collectors.toList());

        if (customerAccountNumbers.isEmpty()) {
            return new CustomerRiskDTO("LOW", 0L, 0L, null);
        }

        // Get all payments for customer
        List<PaymentRecord> allPayments = paymentRepository.findAll();
        List<PaymentRecord> customerPayments = allPayments.stream()
            .filter(p ->
                customerAccountNumbers.contains(p.sourceAccount()) ||
                customerAccountNumbers.contains(p.destinationAccount())
            )
            .collect(Collectors.toList());

        // Count fraud assessment flags and rejected payments
        long fraudFlags = customerPayments.stream()
            .filter(p -> {
                var assessment = fraudAssessmentRepository.findByPaymentId(p.id());
                return assessment.isPresent() && assessment.get().decision() == FraudDecision.SUSPICIOUS;
            })
            .count();

        long rejectedTransactions = customerPayments.stream()
            .filter(p -> {
                var assessment = fraudAssessmentRepository.findByPaymentId(p.id());
                return assessment.isPresent() && assessment.get().decision() == FraudDecision.REJECTED;
            })
            .count();

        // Get last suspicious activity timestamp
        LocalDateTime lastSuspiciousActivity = customerPayments.stream()
            .filter(p -> {
                var assessment = fraudAssessmentRepository.findByPaymentId(p.id());
                return assessment.isPresent() && 
                    (assessment.get().decision() == FraudDecision.SUSPICIOUS || 
                     assessment.get().decision() == FraudDecision.REJECTED);
            })
            .map(PaymentRecord::updatedAt)
            .max(LocalDateTime::compareTo)
            .orElse(null);

        // Determine overall risk level based on fraud flags and rejections
        String riskLevel = "LOW";
        if (fraudFlags > 5 || rejectedTransactions > 3) {
            riskLevel = "HIGH";
        } else if (fraudFlags > 2 || rejectedTransactions > 1) {
            riskLevel = "MEDIUM";
        }

        return new CustomerRiskDTO(
            riskLevel,
            fraudFlags,
            rejectedTransactions,
            lastSuspiciousActivity
        );
    }

    /**
     * Get bulk payment batches for a customer.
     * Returns empty list if bulk payment tables don't exist or query fails.
     */
    public List<BulkPaymentSummaryDTO> getCustomerBulkPayments(Long customerId, int page, int pageSize) {
        // Verify customer exists
        if (customerRepository.findById(customerId).isEmpty()) {
            throw new CustomerNotFoundException("Customer not found: " + customerId);
        }

        // Get customer's account numbers
        List<AccountRecord> customerAccounts = accountRepository.findByCustomerId(customerId);
        List<String> customerAccountNumbers = customerAccounts.stream()
            .map(AccountRecord::accountNumber)
            .collect(Collectors.toList());

        if (customerAccountNumbers.isEmpty()) {
            return List.of();
        }

        try {
            // Get all batches where customer owns the source account
            List<BulkPaymentSummaryDTO> results = customerAccountNumbers.stream()
                .flatMap(accountNumber -> {
                    try {
                        return bulkPaymentBatchRepository.findBySourceAccount(accountNumber, 100, 0).stream();
                    } catch (Exception e) {
                        // Log but don't fail - bulk payment table might not exist or query error
                        return java.util.stream.Stream.empty();
                    }
                })
                .map(batch -> new BulkPaymentSummaryDTO(
                    batch.id(),
                    batch.batchReference(),
                    batch.sourceAccount(),
                    batch.totalTransactions(),
                    batch.successfulTransactions(),
                    batch.failedTransactions(),
                    batch.totalAmount(),
                    batch.status(),
                    batch.createdAt(),
                    batch.completedAt()
                ))
                .collect(Collectors.toList());

            // Return paginated results
            int start = page * pageSize;
            int end = Math.min(start + pageSize, results.size());
            return results.subList(start, Math.min(end, results.size()));
        } catch (Exception e) {
            // If bulk payment query completely fails, return empty list
            // This allows profile to still load even if bulk payment feature has issues
            return List.of();
        }
    }
}

