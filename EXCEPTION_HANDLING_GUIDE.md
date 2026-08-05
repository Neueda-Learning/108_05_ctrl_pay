# Exception Handling & User-Friendly Error Messages Guide

## Overview

All exceptions in the Ctrl-Pay application are now mapped to user-friendly error messages through a centralized **GlobalExceptionHandler**. This prevents technical/database errors from being exposed to end users while maintaining detailed logging for debugging.

---

## Architecture

### Exception Flow

```
Controller Method
    ↓
Throws Custom Exception or Generic Exception
    ↓
GlobalExceptionHandler@ControllerAdvice
    ↓
Masks Technical Details, Logs Full Details
    ↓
Returns StandardizedErrorResponse (JSON)
    ↓
Client Receives User-Friendly Message
```

### Response Format

All error responses follow this standardized structure:

```json
{
  "errorCode": "PAYMENT_NOT_FOUND",
  "message": "The payment you are looking for does not exist.",
  "status": 404,
  "path": "/api/payments/123",
  "timestamp": "2026-08-05T18:45:30"
}
```

**Fields:**
- **errorCode** - Machine-readable error code for programmatic handling
- **message** - User-friendly error description (NO technical details)
- **status** - HTTP status code
- **path** - API endpoint where error occurred
- **timestamp** - ISO 8601 timestamp of when error occurred

---

## Exception Types & Handlers

### 1. **PaymentValidationException** (400 Bad Request)
**When:** Payment validation rules fail (amount out of range, missing fields, etc.)

**User Message:** Original business validation message  
**Logs:** Full details for debugging
**Example:**
```json
{
  "errorCode": "AMOUNT_OUT_OF_RANGE",
  "message": "Payment amount must be between $0.01 and $1,000,000",
  "status": 400
}
```

---

### 2. **PaymentNotFoundException** (404 Not Found)
**When:** Requested payment doesn't exist

**Raw Message:** ❌ `"Payment not found: 999"`  
**User Message:** ✅ `"The payment you are looking for does not exist."`
```json
{
  "errorCode": "PAYMENT_NOT_FOUND",
  "message": "The payment you are looking for does not exist.",
  "status": 404
}
```

---

### 3. **AccountValidationException** (400 Bad Request)
**When:** Account validation fails (invalid PIN, account locked, etc.)

**User Message:** Original business validation message  
**Examples:**
- "PIN verification failed: Invalid PIN"
- "Account is suspended"
- "Insufficient funds for this transaction"

---

### 4. **AccountNotFoundException** (404 Not Found)
**When:** Requested account doesn't exist

**Raw Message:** ❌ `"Account not found: 123456789012"`  
**User Message:** ✅ `"The account you are looking for does not exist."`

---

### 5. **CustomerNotFoundException** (404 Not Found)
**When:** Requested customer doesn't exist

**Raw Message:** ❌ `"Customer not found: 42"`  
**User Message:** ✅ `"The customer you are looking for does not exist."`

---

### 6. **BulkPaymentCSVValidationException** (400 Bad Request)
**When:** CSV file validation fails

**User Message:** Specific error about CSV format  
**Examples:**
- "File is empty. Please select a valid CSV file."
- "Unable to read CSV file. Please ensure the file is not corrupted and try again."
- "Row 5: Destination account must be 12 digits"

---

### 7. **BulkPaymentBatchNotFoundException** (404 Not Found)
**When:** Requested bulk payment batch doesn't exist

**Raw Message:** ❌ `"Bulk payment batch not found: BP1722873392456"`  
**User Message:** ✅ `"The requested bulk payment batch does not exist."`

---

### 8. **PaymentProcessingException** (500 Internal Server Error)
**When:** Unexpected error during payment processing

**Raw Message:** ❌ `"Error creating payment: NullPointerException at line 42"`  
**User Message:** ✅ `"An error occurred while processing your payment. Please try again later or contact support."`

---

### 9. **SQLException** (500 Internal Server Error)
**When:** Database operations fail

**How It's Handled:**
- Logs: Full SQL error for debugging
- User Sees: Generic message based on error type

**Examples:**
```
Database Error: Unknown database 'ctrl_pay'
→ User Message: "System configuration error. Please contact support."

Database Error: Connection refused
→ User Message: "Unable to connect to database. Please try again later."

Database Error: FOREIGN KEY constraint violation
→ User Message: "Cannot complete operation: referenced data does not exist."

Database Error: Duplicate entry
→ User Message: "This record already exists. Please use a different value."
```

---

### 10. **DataAccessException** (500 Internal Server Error)
**When:** Spring Data access operations fail

**How It's Handled:**
- Logs: Full exception with stack trace
- User Sees: Generic message

**Examples:**
```
"Connection timeout after 30 seconds"
→ User Message: "Database operation timed out. Please try again."
```

---

### 11. **IllegalArgumentException** (400 Bad Request)
**When:** Invalid method arguments or parameters

**User Message:** Original error message from validation  
**Examples:**
- "Source account is required."
- "Valid batch ID is required."
- "Limit must be between 1 and 1000."

---

### 12. **IllegalStateException** (409 Conflict)
**When:** Operation is invalid for current object state

**User Message:** Original error message  
**Example:** "Cannot cancel payment in COMPLETED status"

---

### 13. **NumberFormatException** (400 Bad Request)
**When:** Numeric parameter cannot be parsed

**Raw Message:** ❌ `"For input string: 'abc123'"`  
**User Message:** ✅ `"One or more numeric values are invalid. Please check your input."`

---

### 14. **Generic Exception** (500 Internal Server Error)
**When:** Any other unexpected exception

**Logs:** Full class name and stack trace for debugging  
**User Message:** `"An unexpected error occurred. Please try again later or contact support if the problem persists."`

---

## Best Practices

### ✅ DO:

1. **Throw specific exceptions** with business-meaningful messages
   ```java
   throw new AccountValidationException("Insufficient funds", "INSUFFICIENT_FUNDS");
   ```

2. **Log with appropriate levels**
   ```java
   logger.warn("Validation failed: {}", ex.getMessage());    // Validation warnings
   logger.error("Processing error: {}", ex.getMessage(), ex); // Errors with stack trace
   ```

3. **Never expose technical details** to users
   ```java
   // ❌ DON'T do this:
   throw new PaymentProcessingException(ex.getClass().getName() + ": " + ex.getMessage());
   
   // ✅ DO this:
   throw new PaymentProcessingException("An error occurred while processing your request.");
   ```

4. **Use appropriate HTTP status codes**
   - 400 - Client error (bad request, validation failed)
   - 404 - Resource not found
   - 409 - Conflict (operation invalid for current state)
   - 500 - Server error (unexpected exception)

5. **Validate input early** in controllers
   ```java
   if (limit <= 0 || limit > 1000) {
       throw new IllegalArgumentException("Limit must be between 1 and 1000.");
   }
   ```

---

### ❌ DON'T:

1. **Don't catch and silently ignore exceptions**
   ```java
   // ❌ BAD:
   try {
       accountService.verify(pin);
   } catch (Exception e) {
       // Silent fail - user won't know what happened!
   }
   ```

2. **Don't return raw database errors to users**
   ```java
   // ❌ BAD:
   catch (SQLException e) {
       return ResponseEntity.serverError().body(
           new ErrorResponse("SQL_ERROR", e.getMessage(), ...)
       );
   }
   ```

3. **Don't handle exceptions in controllers when GlobalExceptionHandler will do it**
   ```java
   // ❌ BAD (redundant):
   try {
       return bulkPaymentService.getBatch(id);
   } catch (BulkPaymentException e) {
       return ResponseEntity.badRequest().build(); // No details to user!
   }
   
   // ✅ GOOD (let handler deal with it):
   return bulkPaymentService.getBatch(id); // Exception thrown, handler catches it
   ```

4. **Don't expose stack traces or internal details**
   ```java
   // ❌ BAD:
   "Error: " + ex.getCause().getMessage()
   
   // ✅ GOOD:
   "An error occurred. Please try again later."
   ```

---

## Logging Guidelines

### Log Levels

| Level | When | Example |
|-------|------|---------|
| **DEBUG** | Detailed flow info for debugging | `logger.debug("Processing batch: {}", batchId);` |
| **INFO** | Important business events | `logger.info("Bulk payment batch created: {}", reference);` |
| **WARN** | Potential issues that don't block operation | `logger.warn("Validation warning: {}", message);` |
| **ERROR** | Errors that prevent operation success | `logger.error("Processing failed: {}", ex.getMessage(), ex);` |

### Examples

```java
// ✅ GOOD: Clear, non-sensitive, includes context
logger.info("Creating payment from {} to {}", request.sourceAccount(), request.destinationAccount());
logger.warn("Validation failed for account: {}", accountNumber);
logger.error("Database operation failed", exception);

// ❌ BAD: Vague or sensitive
logger.error("Error: " + exception.toString());
logger.info("Processing " + customer.getSocialSecurity() + "'s payment");
```

---

## Testing Exception Handling

### Test Cases to Implement

1. **Valid Request → Success Response**
2. **Invalid Input → 400 Bad Request** with errorCode and message
3. **Resource Not Found → 404 Not Found** with generic message
4. **Database Error → 500 Error** without SQL details
5. **Validation Failure → 400 Bad Request** with business message

### Example Test

```java
@Test
void testPaymentNotFound() {
    // When
    var response = testClient.get()
        .uri("/api/payments/999")
        .exchange();
    
    // Then
    assertThat(response.getStatus().value()).isEqualTo(404);
    JsonNode body = response.getBody(JsonNode.class);
    assertThat(body.get("errorCode").asText()).isEqualTo("PAYMENT_NOT_FOUND");
    assertThat(body.get("message").asText())
        .doesNotContain("null", "exception", "stack");
}
```

---

## Controller Best Practices

### BulkPaymentController Example

```java
@PostMapping
public ResponseEntity<BulkPaymentResponseDTO> createBulkPayment(
    @RequestBody CreateBulkPaymentRequest request
) {
    // Validate input early
    if (request.sourceAccount() == null || request.sourceAccount().isEmpty()) {
        throw new IllegalArgumentException("Source account is required.");
    }
    if (request.pin() == null || request.pin().isEmpty()) {
        throw new IllegalArgumentException("PIN is required for bulk payments.");
    }
    if (request.items() == null || request.items().isEmpty()) {
        throw new IllegalArgumentException("At least one payment item is required.");
    }
    
    // Let exceptions propagate to GlobalExceptionHandler
    BulkPaymentResponseDTO response = bulkPaymentService.createBulkPayment(request, userId);
    return ResponseEntity.accepted().body(response);
}
```

**Key Points:**
- ✅ Validate early and throw appropriate exceptions
- ✅ Don't catch exceptions that GlobalExceptionHandler will handle
- ✅ Let exceptions with proper error codes propagate

---

## Monitoring & Support

### Server Logs (Application Error Details)

Server logs include full error information for support team:
```
2026-08-05T18:45:30.123 ERROR BulkPaymentController - Unexpected error occurred: java.lang.NullPointerException
    at com.neueda.service.BulkPaymentService.processBatch(BulkPaymentService.java:234)
    at com.neueda.controller.BulkPaymentController.createBulkPayment(BulkPaymentController.java:82)
    ...
```

### API Responses (User-Friendly)

Users see only:
```json
{
  "errorCode": "INTERNAL_ERROR",
  "message": "An unexpected error occurred. Please try again later or contact support if the problem persists.",
  "status": 500,
  "timestamp": "2026-08-05T18:45:30"
}
```

**For Support:** Provide timestamp + errorCode + path. Support can match this to detailed server logs.

---

## Summary Table

| Exception | HTTP Code | User Message | Log Level |
|-----------|-----------|--------------|-----------|
| PaymentValidationException | 400 | Business validation message | WARN |
| PaymentNotFoundException | 404 | "...does not exist" | INFO |
| AccountValidationException | 400 | Business validation message | WARN |
| BulkPaymentCSVValidationException | 400 | Specific CSV error | WARN |
| BulkPaymentBatchNotFoundException | 404 | "...does not exist" | INFO |
| SQLException | 500 | Generic database error | ERROR |
| DataAccessException | 500 | "Operation timed out/failed" | ERROR |
| IllegalArgumentException | 400 | Parameter validation error | WARN |
| IllegalStateException | 409 | State validation error | WARN |
| Any Exception | 500 | "Unexpected error occurred" | ERROR |

---

## Files Modified

1. **`exception/GlobalExceptionHandler.java`** - Added comprehensive exception handling with 14 handlers
2. **`controller/BulkPaymentController.java`** - Removed try-catch blocks, let GlobalExceptionHandler handle exceptions
3. **`exception/ValidationRuleRepositoryImpl.java`** - Added null safety for database mappings

---

## Summary

✅ **All technical details are masked** from users  
✅ **Full details are logged** for debugging  
✅ **User-friendly messages** are always returned  
✅ **Consistent error format** across all endpoints  
✅ **Proper HTTP status codes** for all scenarios  


