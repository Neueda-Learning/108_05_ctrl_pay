# Bulk Payments API - Quick Reference & Testing Guide

## API Endpoints Summary

| Method | Endpoint | Purpose | Returns |
|--------|----------|---------|---------|
| POST | `/api/bulk-payments/validate-csv` | Validate CSV format before upload | CSVValidationResultDTO |
| POST | `/api/bulk-payments` | Create bulk payment batch | BulkPaymentResponseDTO |
| GET | `/api/bulk-payments/{batchId}` | Get batch details + results | BulkPaymentResponseDTO |
| GET | `/api/bulk-payments/by-reference/{batchReference}` | Get batch by reference | BulkPaymentResponseDTO |
| GET | `/api/bulk-payments/{batchId}/progress` | Real-time progress (for polling) | BulkPaymentProgressDTO |
| GET | `/api/bulk-payments/history` | Batch history for user | List<BulkPaymentResponseDTO> |

---

## Request/Response Examples

### 1. CSV Upload Validation

**Request:**
```bash
curl -X POST http://localhost:8080/api/bulk-payments/validate-csv \
  -F "file=@bulk_payments.csv"
```

**CSV File Format (Required):**
```csv
destinationAccount,amount,currency,description
987654321001,1000,USD,Rent payment
987654321002,500,USD,Invoice payment
987654321003,2000,EUR,Vendor payment
```

**Response (Valid CSV):**
```json
{
  "totalRecords": 3,
  "validRecords": 3,
  "invalidRecords": 0,
  "isValid": true,
  "errors": []
}
```

**Response (Invalid CSV - Example):**
```json
{
  "totalRecords": 3,
  "validRecords": 2,
  "invalidRecords": 1,
  "isValid": false,
  "errors": [
    {
      "rowNumber": 3,
      "fieldName": null,
      "errorMessage": "Invalid account number",
      "errorCode": "VALIDATION_FAILED"
    }
  ]
}
```

---

### 2. Create Bulk Payment (Manual Entry)

**Request:**
```bash
curl -X POST http://localhost:8080/api/bulk-payments \
  -H "Content-Type: application/json" \
  -d '{
    "sourceAccount": "123456789012",
    "idempotencyKey": "manual-1722882600000",
    "items": [
      {
        "destinationAccount": "987654321001",
        "amount": 1000.00,
        "currency": "USD",
        "description": "Rent payment"
      },
      {
        "destinationAccount": "987654321002",
        "amount": 500.00,
        "currency": "USD",
        "description": "Invoice payment"
      },
      {
        "destinationAccount": "987654321003",
        "amount": 2000.00,
        "currency": "EUR",
        "description": "Vendor payment"
      }
    ]
  }'
```

**Response (Immediate):**
```json
{
  "batchId": 42,
  "batchReference": "BP1722882603456",
  "sourceAccount": "123456789012",
  "totalTransactions": 3,
  "successfulCount": 0,
  "failedCount": 0,
  "status": "VALIDATING",
  "totalAmount": 3500.00,
  "createdAt": "2026-08-05T17:10:03.456Z",
  "completedAt": null,
  "transactionResults": []
}
```

---

### 3. Get Batch Details (After Processing)

**Request:**
```bash
curl -X GET "http://localhost:8080/api/bulk-payments/42"
```

**Response (Completed Batch):**
```json
{
  "batchId": 42,
  "batchReference": "BP1722882603456",
  "sourceAccount": "123456789012",
  "totalTransactions": 3,
  "successfulCount": 2,
  "failedCount": 1,
  "status": "PARTIALLY_COMPLETED",
  "totalAmount": 3500.00,
  "createdAt": "2026-08-05T17:10:03.456Z",
  "completedAt": "2026-08-05T17:10:45.123Z",
  "transactionResults": [
    {
      "lineNumber": 1,
      "paymentId": 501,
      "destinationAccount": "987654321001",
      "amount": 1000.00,
      "currency": "USD",
      "status": "SUCCESS",
      "failureReason": null,
      "errorCode": null,
      "fraudScore": 12.50,
      "fraudDecision": "APPROVED",
      "validationErrors": null,
      "rollbackStatus": null
    },
    {
      "lineNumber": 2,
      "paymentId": 502,
      "destinationAccount": "987654321002",
      "amount": 500.00,
      "currency": "USD",
      "status": "SUCCESS",
      "failureReason": null,
      "errorCode": null,
      "fraudScore": 8.30,
      "fraudDecision": "APPROVED",
      "validationErrors": null,
      "rollbackStatus": null
    },
    {
      "lineNumber": 3,
      "paymentId": 503,
      "destinationAccount": "987654321003",
      "amount": 2000.00,
      "currency": "EUR",
      "status": "FAILED",
      "failureReason": "Fraud score exceeded threshold: 78.50",
      "errorCode": "FRAUD_REJECTED",
      "fraudScore": 78.50,
      "fraudDecision": "REJECTED",
      "validationErrors": null,
      "rollbackStatus": null
    }
  ]
}
```

---

### 4. Get Real-time Progress (for Polling)

**Request:**
```bash
curl -X GET "http://localhost:8080/api/bulk-payments/42/progress"
```

**Response (Still Processing):**
```json
{
  "batchId": 42,
  "batchReference": "BP1722882603456",
  "status": "PROCESSING",
  "totalTransactions": 3,
  "validatedCount": 3,
  "successfulCount": 1,
  "failedCount": 1,
  "progressPercentage": 66,
  "lastUpdatedAt": "2026-08-05T17:10:35.789Z",
  "lastErrorMessage": null
}
```

---

### 5. Get Batch History

**Request:**
```bash
curl -X GET "http://localhost:8080/api/bulk-payments/history?limit=10&offset=0"
```

**Response:**
```json
[
  {
    "batchId": 42,
    "batchReference": "BP1722882603456",
    "sourceAccount": "123456789012",
    "totalTransactions": 3,
    "successfulCount": 2,
    "failedCount": 1,
    "status": "PARTIALLY_COMPLETED",
    "totalAmount": 3500.00,
    "createdAt": "2026-08-05T17:10:03.456Z",
    "completedAt": "2026-08-05T17:10:45.123Z",
    "transactionResults": [...]
  },
  {
    "batchId": 41,
    "batchReference": "BP1722881503456",
    "sourceAccount": "123456789012",
    "totalTransactions": 5,
    "successfulCount": 5,
    "failedCount": 0,
    "status": "COMPLETED",
    "totalAmount": 7500.00,
    "createdAt": "2026-08-05T16:45:03.456Z",
    "completedAt": "2026-08-05T16:46:12.123Z",
    "transactionResults": [...]
  }
]
```

---

## Testing Scenarios

### Scenario 1: All Successful (Happy Path)

**Setup:**
```csv
destinationAccount,amount,currency,description
987654321001,100,USD,Payment 1
987654321002,200,USD,Payment 2
987654321003,150,USD,Payment 3
```

**Expected Result:**
- Status: COMPLETED
- Successful: 3, Failed: 0
- All transactions show fraudScore < 50% and fraudDecision = APPROVED

---

### Scenario 2: Partial Success (Mixed Results)

**Setup:**
```csv
destinationAccount,amount,currency,description
987654321001,100,USD,Small payment
987654321002,5000,USD,Large payment (might trigger fraud)
987654321003,150,EUR,Cross-currency
```

**Expected Result:**
- Status: PARTIALLY_COMPLETED
- Some successful, some failed
- Failed transactions show errorCode (e.g., FRAUD_REJECTED or INSUFFICIENT_FUNDS)
- Fraud scores vary based on amount and pattern

---

### Scenario 3: Validation Errors

**Setup:**
```csv
destinationAccount,amount,currency,description
INVALID_ACCOUNT,100,USD,Bad account format
123456789012,abc,USD,Invalid amount
12345678901,100,INVALID,Bad currency (< 3 chars)
```

**Expected Result:**
- Status: FAILED or VALIDATING (depending on implementation)
- validation_errors field shows specific rule failures
- Error codes: VALIDATION_FAILED
- Items remain in PENDING or VALIDATED_FAILED state

---

### Scenario 4: Insufficient Funds

**Setup:**
```csv
destinationAccount,amount,currency,description
987654321001,1000000,USD,Amount exceeds account balance
```

**Prerequisite:** Source account has balance < 1,000,000

**Expected Result:**
- Status: FAILED
- failureReason: "Insufficient funds at settlement"
- errorCode: INSUFFICIENT_FUNDS_AT_SETTLEMENT

---

### Scenario 5: Same Source and Destination

**Setup:**
```csv
destinationAccount,amount,currency,description
123456789012,100,USD,Same account as source
```

**Expected Result:**
- Status: FAILED
- errorCode: VALIDATION_FAILED
- validationErrors: "Source and destination accounts must be different"

---

## curl Testing Cheat Sheet

### Test CSV Validation:
```bash
# Create test CSV file
cat > /tmp/test_bulk.csv << 'EOF'
destinationAccount,amount,currency,description
987654321001,1000,USD,Test payment
987654321002,500,EUR,Test payment 2
EOF

# Validate
curl -X POST http://localhost:8080/api/bulk-payments/validate-csv \
  -F "file=@/tmp/test_bulk.csv"
```

### Test Manual Bulk Entry:
```bash
# Create batch from JSON
curl -X POST http://localhost:8080/api/bulk-payments \
  -H "Content-Type: application/json" \
  -d '{
    "sourceAccount": "123456789012",
    "items": [
      {"destinationAccount": "987654321001", "amount": 1000, "currency": "USD", "description": "Test"}
    ]
  }' | jq '.'
```

### Test Batch Retrieval (assume batchId=42):
```bash
# Get full details
curl http://localhost:8080/api/bulk-payments/42 | jq '.'

# Get progress
curl http://localhost:8080/api/bulk-payments/42/progress | jq '.'

# Get by reference (replace BP... with actual reference)
curl 'http://localhost:8080/api/bulk-payments/by-reference/BP1722882603456' | jq '.'

# Get history
curl 'http://localhost:8080/api/bulk-payments/history?limit=10&offset=0' | jq '.'
```

---

## Database Queries for Manual Verification

```sql
-- Check batches created
SELECT id, batch_reference, status, successful_transactions, failed_transactions, created_at
FROM bulk_payment_batches
ORDER BY created_at DESC
LIMIT 10;

-- Check items in a specific batch
SELECT id, line_number, destination_account, status, fraud_score, fraud_decision, error_code
FROM bulk_payment_items
WHERE batch_id = 42
ORDER BY line_number;

-- Check error log for batch
SELECT batch_id, item_id, line_number, error_type, error_code, error_message
FROM bulk_payment_error_log
WHERE batch_id = 42;

-- Check audit events
SELECT id, batch_id, event_type, event_timestamp, triggered_by
FROM bulk_payment_audit_events
WHERE batch_id = 42
ORDER BY event_timestamp;

-- Verify payments were created
SELECT id, source_account, destination_account, status, fraud_decision 
FROM payments
WHERE created_at >= DATE_SUB(NOW(), INTERVAL 1 HOUR)
AND source_account = '123456789012'
ORDER BY created_at DESC;

-- Count batch statistics
SELECT 
  status,
  COUNT(*) as count,
  SUM(total_transactions) as total_items,
  SUM(successful_transactions) as successful_items,
  SUM(failed_transactions) as failed_items,
  SUM(total_amount) as total_value
FROM bulk_payment_batches
GROUP BY status;
```

---

## Performance Testing

### For 100-item batch:
```bash
# Generate CSV with 100 rows
python3 << 'EOF'
print("destinationAccount,amount,currency,description")
for i in range(1, 101):
  account = str(900000000000 + i).rjust(12, '0')
  amount = 100 + (i % 50)
  print(f"{account},{amount},USD,Payment {i}")
EOF > /tmp/bulk_100.csv

# Upload and track time
time curl -X POST http://localhost:8080/api/bulk-payments/validate-csv \
  -F "file=@/tmp/bulk_100.csv" | jq '.'
```

---

## Troubleshooting

### Issue: Batch stuck in VALIDATING
- Check `bulk_payment_audit_events` table for last event
- Check application logs for stack traces
- Verify `bulk_payment_items` all have status != PENDING

### Issue: Payments created but not settled
- Check PaymentSettlementService logs
- Verify account balances before/after in `accounts` table
- Check `bulk_payment_error_log` for SETTLEMENT errors

### Issue: Fraud detection not working
- Verify FraudDetectionService is enabled (fraud.detection.enabled=true)
- Check fraud_assessments table for payment
- Verify fraud_rules table has active rules

---

## Frontend Testing Checklist

- [ ] CSV upload with valid file works
- [ ] CSV upload with invalid file shows errors
- [ ] Download sample CSV works
- [ ] Manual entry mode: add/remove rows works
- [ ] Manual entry: field validation works (account format, amount, currency)
- [ ] Submit batch: success message shows batch reference
- [ ] Results page: summary cards show correct counts
- [ ] Results page: successful and failed transactions separated
- [ ] Results page: error details modal works
- [ ] Results page: download results CSV works
- [ ] Progress polling updates during processing
- [ ] Batch history shows all batches with filters
- [ ] UI responsive on mobile (test 375px width)

---

## Security Considerations

1. **Authentication:** Replace "DEMO_USER" with real AuthContext
2. **Authorization:** Validate user owns the source account
3. **Rate Limiting:** Consider rate limit on /api/bulk-payments endpoint
4. **CSV Injection:** All CSV values sanitized/escaped before display
5. **SQL Injection:** All SQL uses parameterized queries (JDBC prepared statements)
6. **File Upload:** Limit CSV file size (recommend < 10MB)

---

## Performance Optimization Notes

- Batch insert for items: 100+ items per second (JDBC batch)
- Validation: ~100 items/second (rule engine overhead)
- Settlement: Limited by payment service + fraud detection (~10-20 items/second)
- Database indexes on batch_id, status, created_at for fast queries
- Consider async processing for batches > 1000 items

---

**Last Updated:** August 5, 2026  
**Status:** Ready for Testing ✅

