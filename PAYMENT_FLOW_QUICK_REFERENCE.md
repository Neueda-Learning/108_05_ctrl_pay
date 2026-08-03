# Payment Flow - Quick Reference Guide

## 🎯 One-Page Summary

### User Action Flow
```
1. CLICK CREATE PAYMENT
        ↓
2. FILL PAYMENT DETAILS
        ↓
3. REVIEW PAYMENT
        ↓
4. CONFIRM & SUBMIT
        ↓
5. AUTOMATIC PROCESSING STARTS
   ├─ Validates & shows status
   ├─ Sends & shows status
   ├─ Completes & shows status
   └─ Shows result
        ↓
6. SUCCESS or FAILURE SCREEN
   ├─ Success: Download receipt
   └─ Failure: Retry or go back
```

## 📊 Status Flow Diagram

```
CREATE PAYMENT
    │
    ├─ [ ] VALIDATE         (Auto)
    │  │
    │  ├─ Success → [ ] SEND
    │  │
    │  └─ Fail → [✗] FAILED (No retry for validation)
    │
    ├─ [ ] SEND             (Auto, retry 3x)
    │  │
    │  ├─ Success → [ ] COMPLETE
    │  │
    │  └─ Fail → RETRY 1 → RETRY 2 → RETRY 3 → [✗] FAILED
    │
    └─ [ ] COMPLETE         (Auto, retry 3x)
       │
       ├─ Success → [✓] COMPLETED (SUCCESS SCREEN)
       │
       └─ Fail → RETRY 1 → RETRY 2 → RETRY 3 → [✗] FAILED

FAILED STATE OPTIONS:
├─ [↻] Manual Retry (resets everything)
└─ [◀️] Back to Payments List
```

## 🎨 Visual Indicators

### Step Status Indicators
| Icon | Meaning | Color |
|------|---------|-------|
| ✓ | Step completed | Green |
| ⏳ | Processing now | Blue (spinner) |
| ⌛ | Waiting to start | Gray |
| ✗ | Step failed | Red |

### Alert Types
| Type | Message | Action |
|------|---------|--------|
| ℹ️ Info | Currently validating... | Wait |
| ✓ Success | Payment completed! | Download receipt |
| ✗ Error | Payment failed | Retry or go back |
| 🔄 Retry | Retry attempt 2 of 3 | Auto-retrying |

## 📍 Menu Structure

```
MAIN MENU
├─ Dashboard → Overview
├─ Payments
│  ├─ List → All payments
│  ├─ Create → New payment
│  │           ↓
│  │     [Fill Details]
│  │           ↓
│  │     [Review]
│  │           ↓
│  │     [Confirm]
│  │           ↓
│  │     → PaymentProcessing (Auto-processing)
│  │           ↓
│  │     [Success/Failure Screen]
│  │
│  └─ Details → View payment info
├─ Rules → Manage validation rules
└─ Analytics → View statistics
```

## ⏱️ Timeline Example

```
T=0s    Payment Created (CREATED)
        ↓
T=0.5s  Auto-redirect to processing page
        ↓
T=1s    Validation starts (shows spinner on step 1)
        ↓
T=2s    Validation completes (status: VALIDATED)
        [1.5s UI delay for visual effect]
        ↓
T=3.5s  Sending starts (shows spinner on step 2)
        ↓
T=4s    Send succeeds → status: SENT
        [1.5s UI delay]
        ↓
T=5.5s  Completion starts (shows spinner on step 3)
        ↓
T=6s    Completion succeeds → status: COMPLETED
        [Shows success screen with green checkmarks]
        ↓
T=6.5s  User sees:
        ✓ Create ✓ Validate ✓ Send ✓ Complete
        "Payment completed successfully!"
        [Download Receipt] [Back to Payments]

Total time: ~6.5 seconds (normal flow)
With 1 retry: ~11 seconds
With 3 retries: ~20 seconds
```

## 🔄 Retry Behavior

### When Retries Happen
```
SEND fails?
└─ RETRY 1 after 2 seconds
   ├─ Success? → Continue to COMPLETE
   └─ Fail? → RETRY 2 after 2 seconds
             ├─ Success? → Continue to COMPLETE
             └─ Fail? → RETRY 3 after 2 seconds
                       ├─ Success? → Continue to COMPLETE
                       └─ Fail? → SHOW FAILURE SCREEN
```

### Retry Attempts Display
```
NO RETRIES YET
└─ [Retry Attempt Counter Hidden]

FIRST RETRY
├─ Message: "Retry attempt 1 of 3"
├─ Counter: 1/3
└─ Payment reverts to previous state

SECOND RETRY
├─ Message: "Retry attempt 2 of 3"
├─ Counter: 2/3
└─ Payment reverts to previous state

THIRD RETRY
├─ Message: "Retry attempt 3 of 3"
├─ Counter: 3/3
└─ Payment reverts to previous state

ALL RETRIES FAILED
├─ Counter: 3/3 (still visible)
├─ Status: FAILED
├─ [Retry Payment] Button appears
└─ Clicking resets counter to 0/3
```

## 🔗 API Endpoints Reference

| Endpoint | Method | Purpose | Status Change |
|----------|--------|---------|----------------|
| `/api/payments` | POST | Create new payment | → CREATED |
| `/api/payments/{id}` | GET | Get payment status | - (no change) |
| `/api/payments/{id}/validate` | POST | Validate payment | CREATED → VALIDATED |
| `/api/payments/{id}/send` | POST | Send to gateway | VALIDATED → SENT |
| `/api/payments/{id}/complete` | POST | Complete payment | SENT → COMPLETED |
| `/api/payments/{id}/fail` | POST | Mark as failed | any → FAILED |

## 📱 Page Routes

| Route | Component | Purpose |
|-------|-----------|---------|
| `/` | Dashboard | Overview and statistics |
| `/payments` | PaymentsList | List all payments |
| `/payments/create` | CreatePayment | Create new payment |
| `/payment/process/{id}` | **PaymentProcessing** | **Process payment (NEW)** |
| `/payments/{id}` | PaymentDetail | View payment details |
| `/rules` | RulesManagement | Manage validation rules |
| `/analytics` | Analytics | View analytics |

## ⚙️ Configuration Values

```javascript
// In PaymentProcessing.jsx

MAX_RETRIES = 3                    // Retry attempts
POLLING_INTERVAL = 2000            // 2 seconds between polls
AUTO_TRANSITION_DELAY = 1500       // 1.5 seconds between step display

// In App.jsx Theme
success.main = '#2e7d32'           // Green
warning.main = '#f57c00'           // Orange
error.main = '#d32f2f'             // Red
info.main = '#1976d2'              // Blue
```

## 🎯 Component Purpose

### PaymentStatusFlow.jsx
**What it does:** Displays visual payment progress
**Shows:**
- 4-step stepper
- Current step with spinner
- Completed steps with checkmarks
- Failed step with X
- Error messages
- Retry counter

### PaymentProcessing.jsx
**What it does:** Orchestrates entire payment workflow
**Handles:**
- Auto-transition through states
- Retry logic
- Error management
- Real-time polling
- State updates
- User actions (manual retry)

### CreatePayment.jsx (Modified)
**What it does:** Collects payment details
**Changes:** Now redirects to PaymentProcessing instead of PaymentDetail

## 💾 State Structure

```javascript
// PaymentProcessing State
{
  payment: {                  // Current payment object
    id: 123,
    status: "VALIDATED",      // CREATED|VALIDATED|SENT|COMPLETED|FAILED
    sourceAccount: "123456789012",
    destinationAccount: "210987654321",
    amount: 1000,
    currency: "USD",
    errorCode: null,
    errorMessage: null,
  },
  loading: false,             // Initial load state
  processingPhase: "sending", // validating|sending|completing|null
  errorDetails: null,         // {code, message} if error
  retryCount: 1,              // 0-3
  autoProcessing: true,       // Is workflow still running?
}
```

## 🎨 Color Scheme

```
Success (Green):   #2e7d32 (main), #4caf50 (light), #c8e6c9 (lighter)
Warning (Orange):  #f57c00 (main), #ffb74d (light), #ffe0b2 (lighter)
Error (Red):       #d32f2f (main), #ef5350 (light), #ffcdd2 (lighter)
Info (Blue):       #1976d2 (main), #42a5f5 (light), #bbdefb (lighter)
```

## 🔍 Debugging Tips

### Check Payment Status
```bash
# Open browser DevTools → Network tab
# Look for: POST /api/payments - returns created payment
#          POST /api/payments/{id}/validate
#          POST /api/payments/{id}/send
#          POST /api/payments/{id}/complete
#          GET /api/payments/{id} - polling requests
```

### Check Console Logs
```bash
# Open Console tab
# Look for error messages from PaymentProcessing
# API call errors will show response details
```

### Check Local State
```javascript
// In browser console
// Search for React DevTools to inspect component state
// Or add console.logs in PaymentProcessing.jsx:
console.log('Payment:', payment);
console.log('Processing Phase:', processingPhase);
console.log('Retry Count:', retryCount);
```

## ❌ Common Issues & Fixes

| Issue | Cause | Solution |
|-------|-------|----------|
| Payment stuck in CREATED | Validation failed | Check backend logs, validate payment structure |
| Redirect not working | createPayment returns error | Check form validation, network error |
| No auto-transition | Polling not running | Check browser Network tab for /api/payments calls |
| Retry not working | History endpoint failing | Check `/api/payments/{id}/history` works |
| UI not updating | State not changing | Check React DevTools, component mounted? |
| Colors look wrong | Theme not applied | Clear browser cache, restart dev server |

## 📞 Quick Support

**Payment stuck?**
- Clear browser cache and refresh
- Check backend is running (port 8080)
- Check browser console for errors

**Retry not working?**
- Verify payment history API works
- Check maximum retry attempts not exceeded
- Try manual page refresh

**Colors wrong?**
- Clear localStorage and cache
- Restart dev server
- Check App.jsx theme config

**Need help?**
- See `PAYMENT_FLOW_DESIGN.md` for detailed docs
- See `PAYMENT_PROCESSING_SETUP.md` for setup help
- See `IMPLEMENTATION_SUMMARY.md` for architecture

---

**Last Updated:** August 2, 2026  
**Status:** Ready to Use ✅

