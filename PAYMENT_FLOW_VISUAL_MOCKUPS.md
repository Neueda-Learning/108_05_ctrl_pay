# Payment Processing Flow - Visual Mockups

## User Interface Walkthrough

### Step 1: Create Payment Form
```
╔════════════════════════════════════════════════════════════════════╗
║                      Create New Payment                            ║
╠════════════════════════════════════════════════════════════════════╣
║                                                                    ║
║  ◉ Payment Details  ◌ Review  ◌ Confirmation                      ║
║  ─────────────────────────────────────────────────────────         ║
║                                                                    ║
║  Payment Information                                              ║
║                                                                    ║
║  ┌────────────────────────────┐  ┌────────────────────────────┐  ║
║  │ From Account    [__________]│  │ To Account      [__________] │  ║
║  └────────────────────────────┘  └────────────────────────────┘  ║
║                                                                    ║
║  ┌────────────────────────────┐  ┌────────────────────────────┐  ║
║  │ Amount          [__________]│  │ Currency        [USD ▼    ] │  ║
║  └────────────────────────────┘  └────────────────────────────┘  ║
║                                                                    ║
║                    ┌──────────┐  ┌──────────────┐  ┌──────────┐  ║
║                    │   Back   │  │   Cancel     │  │  Next →  │  ║
║                    └──────────┘  └──────────────┘  └──────────┘  ║
║                                                                    ║
║  ┌──────────────────────────────────────────────────────────────┐ ║
║  │ Summary                                                      │ ║
║  │                                                              │ ║
║  │ Status: Editing                                             │ ║
║  │ Step: 1 of 3                                                │ ║
║  │ Payment Details                                             │ ║
║  └──────────────────────────────────────────────────────────────┘ ║
║                                                                    ║
╚════════════════════════════════════════════════════════════════════╝
```

### Step 2: Review Payment Details
```
╔════════════════════════════════════════════════════════════════════╗
║                      Create New Payment                            ║
╠════════════════════════════════════════════════════════════════════╣
║                                                                    ║
║  ◉ Payment Details  ◉ Review  ◌ Confirmation                      ║
║  ─────────────────────────────────────────────────────────         ║
║                                                                    ║
║  Review Payment Details                                           ║
║                                                                    ║
║  ┌────────────────────────────────────────────────────────────┐  ║
║  │ From Account: 123456789012         To Account: 210987654321 │  ║
║  │ Amount: 1000.00 USD                                         │  ║
║  │ Status: Pending                                             │  ║
║  └────────────────────────────────────────────────────────────┘  ║
║                                                                    ║
║                    ┌──────────┐  ┌──────────────┐  ┌──────────┐  ║
║                    │ ← Back   │  │   Cancel     │  │ Next →   │  ║
║                    └──────────┘  └──────────────┘  └──────────┘  ║
║                                                                    ║
║  ┌──────────────────────────────────────────────────────────────┐ ║
║  │ Summary                                                      │ ║
║  │                                                              │ ║
║  │ Status: Review                                              │ ║
║  │ Step: 2 of 3                                                │ ║
║  │ Review                                                      │ ║
║  └──────────────────────────────────────────────────────────────┘ ║
║                                                                    ║
╚════════════════════════════════════════════════════════════════════╝
```

### Step 3: Confirm & Submit
```
╔════════════════════════════════════════════════════════════════════╗
║                      Create New Payment                            ║
╠════════════════════════════════════════════════════════════════════╣
║                                                                    ║
║  ◉ Payment Details  ◉ Review  ◉ Confirmation                      ║
║  ─────────────────────────────────────────────────────────────    ║
║                                                                    ║
║                    Confirm and Submit                             ║
║                                                                    ║
║              Click submit to create the payment                   ║
║                                                                    ║
║                    ┌──────────┐  ┌──────────────┐  ┌──────────┐  ║
║                    │ ← Back   │  │   Cancel     │  │ Submit   │  ║
║                    └──────────┘  └──────────────┘  └──────────┘  ║
║                                                                    ║
║  ┌──────────────────────────────────────────────────────────────┐ ║
║  │ Summary                                                      │ ║
║  │                                                              │ ║
║  │ Status: Submitting                                          │ ║
║  │ Step: 3 of 3                                                │ ║
║  │ Confirmation                                                │ ║
║  └──────────────────────────────────────────────────────────────┘ ║
║                                                                    ║
╚════════════════════════════════════════════════════════════════════╝
```

### Step 4: Submission Success (After Click)
```
╔════════════════════════════════════════════════════════════════════╗
║                      Create New Payment                            ║
╠════════════════════════════════════════════════════════════════════╣
║                                                                    ║
║  [✓ Payment 12345 created successfully! Redirecting...]           ║
║                                                                    ║
║                    Confirm and Submit                             ║
║                                                                    ║
║              Click submit to create the payment                   ║
║                                                                    ║
║                    ┌──────────┐  ┌──────────────┐  ┌──────────┐  ║
║                    │ ← Back   │  │   Cancel     │  │ Submit   │  ║
║                    └──────────┘  └──────────────┘  └──────────┘  ║
║                                                                    ║
║  ┌──────────────────────────────────────────────────────────────┐ ║
║  │ Summary                                                      │ ║
║  │                                                              │ ║
║  │ Status: Submitting                                          │ ║
║  │ Step: 3 of 3                                                │ ║
║  │ Confirmation                                                │ ║
║  └──────────────────────────────────────────────────────────────┘ ║
║                                                                    ║
║                  [Redirecting in 1.5 seconds...]                  ║
║                                                                    ║
╚════════════════════════════════════════════════════════════════════╝
```

### Step 5: Payment Processing - Initial State
```
╔════════════════════════════════════════════════════════════════════╗
║                      Payment Processing                            ║
╠════════════════════════════════════════════════════════════════════╣
║                              Back                                  ║
║                                                                    ║
║  ┌────────────────────────────────────────────────────────────┐  ║
║  │ Payment Processing Status                                  │  ║
║  │ Real-time progress tracking of your payment                │  ║
║  │                                                            │  ║
║  │  ┌───┐         ┌───┐         ┌───┐         ┌───┐           │  ║
║  │  │ ✓ │═════════│ ⏳│═════════│ ⌛│═════════│ ⌛│           │  ║
║  │  └───┘         └───┘         └───┘         └───┘           │  ║
║  │  Create       Validate        Send       Complete           │  ║
║  │  Payment      Validations   Gateway      Payment            │  ║
║  │  ✓            ✓              ⌛           ⌛               │  ║
║  │                                                            │  ║
║  │  ✓ Payment created successfully                            │  ║
║  │  ⏳ Currently validating your payment...                   │  ║
║  │     Please wait while we process your payment.             │  ║
║  │                                                            │  ║
║  │  Current Status: CREATED                                  │  ║
║  │  Processing Phase: validating                             │  ║
║  └────────────────────────────────────────────────────────────┘  ║
║                                                                    ║
║  ┌────────────────────────────────────────────────────────────┐  ║
║  │ Payment Details                                            │  ║
║  │                                                            │  ║
║  │ Payment ID: 12345      Amount: 1000.00 USD                │  ║
║  │ From: 123456789012     To: 210987654321                   │  ║
║  └────────────────────────────────────────────────────────────┘  ║
║                                                                    ║
║  ┌────────────────────────────────────────────────────────────┐  ║
║  │ Current Status                                             │  ║
║  │                                                            │  ║
║  │        ⏳ VALIDATING                                       │  ║
║  │                                                            │  ║
║  │ Processing Info                                            │  ║
║  │ Retry Attempts: 0/3                                        │  ║
║  │ Processing Status: Active                                  │  ║
║  └────────────────────────────────────────────────────────────┘  ║
║                                                                    ║
╚════════════════════════════════════════════════════════════════════╝
```

### Step 6: Payment Processing - Validation Complete
```
╔════════════════════════════════════════════════════════════════════╗
║                      Payment Processing                            ║
╠════════════════════════════════════════════════════════════════════╣
║                              Back                                  ║
║                                                                    ║
║  ┌────────────────────────────────────────────────────────────┐  ║
║  │ Payment Processing Status                                  │  ║
║  │ Real-time progress tracking of your payment                │  ║
║  │                                                            │  ║
║  │  ┌───┐         ┌───┐         ┌───┐         ┌───┐           │  ║
║  │  │ ✓ │═════════│ ✓ │═════════│ ⏳│═════════│ ⌛│           │  ║
║  │  └───┘         └───┘         └───┘         └───┘           │  ║
║  │  Create       Validate        Send       Complete           │  ║
║  │  ✓            ✓              Sending     Pending           │  ║
║  │                                                            │  ║
║  │  ✓ Payment created successfully                            │  ║
║  │  ✓ Payment validated successfully                          │  ║
║  │  ⏳ Currently sending to gateway...                        │  ║
║  │     Please wait while we process your payment.             │  ║
║  │                                                            │  ║
║  │  Current Status: VALIDATED                                │  ║
║  │  Processing Phase: sending                                │  ║
║  └────────────────────────────────────────────────────────────┘  ║
║                                                                    ║
║  ┌────────────────────────────────────────────────────────────┐  ║
║  │ Payment Details                                            │  ║
║  │                                                            │  ║
║  │ Payment ID: 12345      Amount: 1000.00 USD                │  ║
║  │ From: 123456789012     To: 210987654321                   │  ║
║  └────────────────────────────────────────────────────────────┘  ║
║                                                                    ║
║  ┌────────────────────────────────────────────────────────────┐  ║
║  │ Current Status                                             │  ║
║  │                                                            │  ║
║  │        ⏳ SENDING                                          │  ║
║  │                                                            │  ║
║  │ Processing Info                                            │  ║
║  │ Retry Attempts: 0/3                                        │  ║
║  │ Processing Status: Active                                  │  ║
║  └────────────────────────────────────────────────────────────┘  ║
║                                                                    ║
╚════════════════════════════════════════════════════════════════════╝
```

### Step 7: Payment Processing - Send Fails, Retrying
```
╔════════════════════════════════════════════════════════════════════╗
║                      Payment Processing                            ║
╠════════════════════════════════════════════════════════════════════╣
║                              Back                                  ║
║                                                                    ║
║  ┌────────────────────────────────────────────────────────────┐  ║
║  │ Payment Processing Status                                  │  ║
║  │ Real-time progress tracking of your payment                │  ║
║  │                                                            │  ║
║  │  ┌───┐         ┌───┐         ┌───┐         ┌───┐           │  ║
║  │  │ ✓ │═════════│ ✓ │═════════│ ⏳│═════════│ ⌛│           │  ║
║  │  └───┘         └───┘         └───┘         └───┘           │  ║
║  │  Create       Validate        Send       Complete           │  ║
║  │  ✓            ✓              Retrying    Pending           │  ║
║  │                                                            │  ║
║  │  ✓ Payment created successfully                            │  ║
║  │  ✓ Payment validated successfully                          │  ║
║  │  🔄 Retry attempt 1 of 3                                   │  ║
║  │  ⏳ Currently sending to gateway...                        │  ║
║  │     Please wait while we process your payment.             │  ║
║  │                                                            │  ║
║  │  Current Status: VALIDATED                                │  ║
║  │  Processing Phase: sending                                │  ║
║  └────────────────────────────────────────────────────────────┘  ║
║                                                                    ║
║  ┌────────────────────────────────────────────────────────────┐  ║
║  │ Payment Details                                            │  ║
║  │                                                            │  ║
║  │ Payment ID: 12345      Amount: 1000.00 USD                │  ║
║  │ From: 123456789012     To: 210987654321                   │  ║
║  └────────────────────────────────────────────────────────────┘  ║
║                                                                    ║
║  ┌────────────────────────────────────────────────────────────┐  ║
║  │ Current Status                                             │  ║
║  │                                                            │  ║
║  │        ⏳ SENDING (RETRY 1/3)                              │  ║
║  │                                                            │  ║
║  │ Processing Info                                            │  ║
║  │ Retry Attempts: 1/3                                        │  ║
║  │ Processing Status: Active                                  │  ║
║  └────────────────────────────────────────────────────────────┘  ║
║                                                                    ║
╚════════════════════════════════════════════════════════════════════╝
```

### Step 8: Payment Processing - Completed Successfully
```
╔════════════════════════════════════════════════════════════════════╗
║                      Payment Processing                            ║
╠════════════════════════════════════════════════════════════════════╣
║                              Back                                  ║
║                                                                    ║
║  ┌────────────────────────────────────────────────────────────┐  ║
║  │ Payment Processing Status                                  │  ║
║  │ Real-time progress tracking of your payment                │  ║
║  │                                                            │  ║
║  │  ┌───┐         ┌───┐         ┌───┐         ┌───┐           │  ║
║  │  │ ✓ │═════════│ ✓ │═════════│ ✓ │═════════│ ✓ │           │  ║
║  │  └───┘         └───┘         └───┘         └───┘           │  ║
║  │  Create       Validate        Send       Complete           │  ║
║  │  ✓            ✓              ✓           ✓               │  ║
║  │                                                            │  ║
║  │  ✓ Payment completed successfully!                         │  ║
║  │    Your transaction has been processed.                    │  ║
║  │                                                            │  ║
║  │  Current Status: COMPLETED                                │  ║
║  │  Processing Phase: idle                                   │  ║
║  └────────────────────────────────────────────────────────────┘  ║
║                                                                    ║
║  ┌────────────────────────────────────────────────────────────┐  ║
║  │ Payment Details                                            │  ║
║  │                                                            │  ║
║  │ Payment ID: 12345      Amount: 1000.00 USD                │  ║
║  │ From: 123456789012     To: 210987654321                   │  ║
║  └────────────────────────────────────────────────────────────┘  ║
║                                                                    ║
║  ┌────────────────────────────────────────────────────────────┐  ║
║  │ Current Status                                             │  ║
║  │                                                            │  ║
║  │         ✓ COMPLETED                                        │  ║
║  │                                                            │  ║
║  │   ┌──────────────────────────────────┐                    │  ║
║  │   │  ✓ Download Receipt              │                    │  ║
║  │   └──────────────────────────────────┘                    │  ║
║  │   ┌──────────────────────────────────┐                    │  ║
║  │   │      Back to Payments List       │                    │  ║
║  │   └──────────────────────────────────┘                    │  ║
║  │                                                            │  ║
║  │ Processing Info                                            │  ║
║  │ Retry Attempts: 0/3                                        │  ║
║  │ Processing Status: Completed                               │  ║
║  └────────────────────────────────────────────────────────────┘  ║
║                                                                    ║
╚════════════════════════════════════════════════════════════════════╝
```

### Step 9: Payment Processing - Failed (Max Retries Exceeded)
```
╔════════════════════════════════════════════════════════════════════╗
║                      Payment Processing                            ║
╠════════════════════════════════════════════════════════════════════╣
║                              Back                                  ║
║                                                                    ║
║  ┌────────────────────────────────────────────────────────────┐  ║
║  │ Payment Processing Status                                  │  ║
║  │ Real-time progress tracking of your payment                │  ║
║  │                                                            │  ║
║  │  ┌───┐         ┌───┐         ┌───┐         ┌───┐           │  ║
║  │  │ ✓ │═════════│ ✓ │═════════│ ✗ │═════════│ ⌛│           │  ║
║  │  └───┘         └───┘         └───┘         └───┘           │  ║
║  │  Create       Validate        Send       Complete           │  ║
║  │  ✓            ✓              ✗ (Failed)  Pending          │  ║
║  │                                                            │  ║
║  │  ✓ Payment created successfully                            │  ║
║  │  ✓ Payment validated successfully                          │  ║
║  │  ✗ Payment failed: NETWORK_ERROR                           │  ║
║  │  🔄 Retry attempt 3 of 3                                   │  ║
║  │                                                            │  ║
║  │  Current Status: FAILED                                   │  ║
║  │  Processing Phase: idle                                   │  ║
║  └────────────────────────────────────────────────────────────┘  ║
║                                                                    ║
║  ┌────────────────────────────────────────────────────────────┐  ║
║  │ Payment Details                                            │  ║
║  │                                                            │  ║
║  │ Payment ID: 12345      Amount: 1000.00 USD                │  ║
║  │ From: 123456789012     To: 210987654321                   │  ║
║  └────────────────────────────────────────────────────────────┘  ║
║                                                                    ║
║  ┌────────────────────────────────────────────────────────────┐  ║
║  │ Current Status                                             │  ║
║  │                                                            │  ║
║  │           ✗ FAILED                                         │  ║
║  │                                                            │  ║
║  │   ┌──────────────────────────────────┐                    │  ║
║  │   │ ↻ Retry Payment                  │                    │  ║
║  │   └──────────────────────────────────┘                    │  ║
║  │   ┌──────────────────────────────────┐                    │  ║
║  │   │      Back to Payments List       │                    │  ║
║  │   └──────────────────────────────────┘                    │  ║
║  │                                                            │  ║
║  │ Processing Info                                            │  ║
║  │ Retry Attempts: 3/3                                        │  ║
║  │ Processing Status: Completed                               │  ║
║  │ Error Details                                              │  ║
║  │ [NETWORK_ERROR] Payment gateway rejected the transaction   │  ║
║  └────────────────────────────────────────────────────────────┘  ║
║                                                                    ║
╚════════════════════════════════════════════════════════════════════╝
```

### Step 10: Payment Processing - After Manual Retry
```
╔════════════════════════════════════════════════════════════════════╗
║                      Payment Processing                            ║
╠════════════════════════════════════════════════════════════════════╣
║                              Back                                  ║
║                                                                    ║
║  ┌────────────────────────────────────────────────────────────┐  ║
║  │ Payment Processing Status                                  │  ║
║  │ Real-time progress tracking of your payment                │  ║
║  │                                                            │  ║
║  │  ┌───┐         ┌───┐         ┌───┐         ┌───┐           │  ║
║  │  │ ✓ │═════════│ ✓ │═════════│ ⏳│═════════│ ⌛│           │  ║
║  │  └───┘         └───┘         └───┘         └───┘           │  ║
║  │  Create       Validate        Send       Complete           │  ║
║  │  ✓            ✓              Retrying    Pending           │  ║
║  │                                                            │  ║
║  │  ✓ Payment created successfully                            │  ║
║  │  ✓ Payment validated successfully                          │  ║
║  │  🔄 Retry attempt 1 of 3  [RESET]                          │  ║
║  │  ⏳ Currently sending to gateway...                        │  ║
║  │     Please wait while we process your payment.             │  ║
║  │                                                            │  ║
║  │  Current Status: VALIDATED                                │  ║
║  │  Processing Phase: sending                                │  ║
║  └────────────────────────────────────────────────────────────┘  ║
║                                                                    ║
║  ┌────────────────────────────────────────────────────────────┐  ║
║  │ Payment Details                                            │  ║
║  │                                                            │  ║
║  │ Payment ID: 12345      Amount: 1000.00 USD                │  ║
║  │ From: 123456789012     To: 210987654321                   │  ║
║  └────────────────────────────────────────────────────────────┘  ║
║                                                                    ║
║  ┌────────────────────────────────────────────────────────────┐  ║
║  │ Current Status                                             │  ║
║  │                                                            │  ║
║  │        ⏳ SENDING (RETRY 1/3 - FRESH)                      │  ║
║  │                                                            │  ║
║  │ Processing Info                                            │  ║
║  │ Retry Attempts: 1/3                                        │  ║
║  │ Processing Status: Active                                  │  ║
║  │ Error Details                                              │  ║
║  │ Retrying after user manual retry...                        │  ║
║  └────────────────────────────────────────────────────────────┘  ║
║                                                                    ║
╚════════════════════════════════════════════════════════════════════╝
```

## Key Visual Elements

### Status Icons
- **✓ (Green Checkmark)**: Completed successfully
- **⏳ (Spinner)**: Currently processing
- **⌛ (Hourglass)**: Waiting to start
- **✗ (Red X)**: Failed

### Color Coding
- **Green (#2e7d32)**: Success, completed steps
- **Blue (#1976d2)**: Info, processing
- **Orange (#f57c00)**: Warning, in progress
- **Red (#d32f2f)**: Error, failed

### Alert Message Types
- **✓ Green**: Success messages
- **✗ Red**: Error messages
- **ℹ️ Blue**: Information/processing
- **🔄 Orange**: Retry attempts

## Animation Sequence

### Creating to Processing (1.5 seconds)
```
T=0.0s: Submission Success Alert → Show
T=0.5s: Page remains visible, user notices alert
T=1.0s: "Redirecting in 1.5 seconds..." showing
T=1.5s: Navigate to /payment/process/{id}
```

### Processing Phase Movement (1.5 seconds per phase)
```
T=0s:   Phase starts, spinner appears on current step
T=1.5s: Color transitions to success, connector lights up
T=2.0s: Next phase begins (without external delay for smooth feel)
```

### Each API Call
```
T=0.0s: Processing phase shows spinner
T=0.5-2.0s: API call in progress
T=2.0s+: Response receives and state updates
T=2.5s: Visual feedback updated with new status
```

---

## UI Responsive Behavior

### Mobile (< 600px)
- Stepper becomes vertical
- Buttons stack vertically
- Status card takes full width
- Summary card below main content

### Tablet (600px - 900px)
- Stepper remains horizontal
- 2-column layout for content/sidebar
- Buttons side by side
- Summary card on the right

### Desktop (> 900px)
- Full horizontal stepper
- 2-column layout maintained
- All buttons visible and spaced
- Summary card on the right side

---

**Last Updated:** August 2, 2026  
**All Mockups Implemented:** ✅

