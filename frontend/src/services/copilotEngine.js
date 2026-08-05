/**
 * copilotEngine.js — Payment Intelligence Center AI Copilot Engine
 *
 * Deterministic intent classification + entity extraction that maps
 * natural-language queries to existing REST API calls.
 * No external LLM or API key required.
 */

import { paymentAPI, analyticsAPI, adminFraudAPI } from './api';

/* ═══════════════════════════════════════════════════════════════════
   §1  INTENT DEFINITIONS
   ═══════════════════════════════════════════════════════════════════ */

const INTENTS = {
  SEARCH_PAYMENT:     'SEARCH_PAYMENT',
  PAYMENT_DETAIL:     'PAYMENT_DETAIL',
  EXPLAIN_FAILURE:    'EXPLAIN_FAILURE',
  AUDIT_TRAIL:        'AUDIT_TRAIL',
  DASHBOARD_INSIGHTS: 'DASHBOARD_INSIGHTS',
  RISK_ASSESSMENT:    'RISK_ASSESSMENT',
  FILTER_PAYMENTS:    'FILTER_PAYMENTS',
  GENERAL_HELP:       'GENERAL_HELP',
};

/* ═══════════════════════════════════════════════════════════════════
   §2  PATTERN RULES  (order matters — first match wins)
   ═══════════════════════════════════════════════════════════════════ */

const INTENT_RULES = [
  // — Failure explanation (must come before generic detail) —
  {
    intent: INTENTS.EXPLAIN_FAILURE,
    patterns: [
      /why\s+(did|has|was|is)\s+payment/i,
      /explain\s+(the\s+)?fail/i,
      /what\s+went\s+wrong\s+with\s+payment/i,
      /payment\s+#?\d+\s+(fail|error|issue)/i,
      /fail(ure|ed)?\s+(reason|explanation|detail)/i,
      /why\s+fail/i,
      /what\s+happened\s+to\s+payment/i,
      /diagnose\s+payment/i,
    ],
  },

  // — Audit trail —
  {
    intent: INTENTS.AUDIT_TRAIL,
    patterns: [
      /audit\s+trail/i,
      /status\s+history/i,
      /show\s+(me\s+)?(the\s+)?history\s+(of|for)/i,
      /transition\s+history/i,
      /payment\s+#?\d+\s+history/i,
      /timeline\s+(of|for)\s+payment/i,
      /trace\s+payment/i,
    ],
  },

  // — Risk assessment —
  {
    intent: INTENTS.RISK_ASSESSMENT,
    patterns: [
      /risk\s+(score|level|assessment|check)/i,
      /is\s+payment\s+#?\d+\s+risk/i,
      /fraud\s+(check|score|risk|probability)/i,
      /how\s+risky/i,
      /risk\s+(for|of)\s+payment/i,
      /assess\s+(risk|fraud)/i,
    ],
  },

  // — Dashboard insights —
  {
    intent: INTENTS.DASHBOARD_INSIGHTS,
    patterns: [
      /dashboard/i,
      /success\s+rate/i,
      /how\s+are\s+(payments?|things|we)\s+doing/i,
      /overview/i,
      /payment\s+stats/i,
      /payment\s+statistics/i,
      /summary\s+of\s+payments/i,
      /insights/i,
      /give\s+me\s+(a\s+)?summary/i,
      /overall\s+performance/i,
      /processing\s+time/i,
      /failure\s+rate/i,
      /what.s\s+the\s+(success|failure|completion)/i,
    ],
  },

  // — Payment detail (single) —
  {
    intent: INTENTS.PAYMENT_DETAIL,
    patterns: [
      /(?:details?|info|information)\s+(?:for|of|about|on)\s+payment/i,
      /payment\s+#?\d+\s*$/i,
      /show\s+(me\s+)?payment\s+#?\d+/i,
      /get\s+payment\s+#?\d+/i,
      /look\s+up\s+payment/i,
      /tell\s+me\s+about\s+payment/i,
    ],
  },

  // — Filtered / search payments —
  {
    intent: INTENTS.FILTER_PAYMENTS,
    patterns: [
      /show\s+(me\s+)?(all\s+)?(?:failed|completed|pending|created|validated|sent|suspicious)\s+payments/i,
      /payments?\s+in\s+[A-Z]{3}/i,
      /payments?\s+(today|yesterday|this\s+week|this\s+month|last\s+\d+\s+days?)/i,
      /list\s+(all\s+)?payments/i,
      /payments?\s+(?:from|to|for)\s+account/i,
      /payments?\s+(?:above|over|greater|more\s+than|below|under|less\s+than)\s+\d/i,
      /how\s+many\s+payments/i,
      /count\s+payments/i,
      /recent\s+payments/i,
    ],
  },

  // — Generic search —
  {
    intent: INTENTS.SEARCH_PAYMENT,
    patterns: [
      /find\s+payment/i,
      /search\s+(for\s+)?payment/i,
      /where\s+is\s+payment/i,
      /locate\s+payment/i,
    ],
  },

  // — Help —
  {
    intent: INTENTS.GENERAL_HELP,
    patterns: [
      /^help$/i,
      /what\s+can\s+you\s+do/i,
      /how\s+do\s+I/i,
      /capabilities/i,
      /^hi$|^hello$|^hey$/i,
      /getting\s+started/i,
    ],
  },
];


/* ═══════════════════════════════════════════════════════════════════
   §3  ENTITY EXTRACTION
   ═══════════════════════════════════════════════════════════════════ */

function extractPaymentId(text) {
  const m = text.match(/payment\s*#?\s*(\d+)/i) || text.match(/#(\d+)/);
  return m ? parseInt(m[1], 10) : null;
}

function extractStatus(text) {
  const statuses = ['CREATED', 'VALIDATED', 'SENT', 'COMPLETED', 'FAILED', 'SUSPICIOUS'];
  const lower = text.toLowerCase();
  for (const s of statuses) {
    if (lower.includes(s.toLowerCase())) return s;
  }
  if (/pending/i.test(lower)) return null;        // pending = NOT completed/failed (handled specially)
  return null;
}

function extractCurrency(text) {
  const m = text.match(/\b([A-Z]{3})\b/);
  if (m) {
    const known = ['USD', 'EUR', 'GBP', 'JPY', 'CAD', 'AUD', 'CHF', 'INR', 'CNY', 'BRL', 'KRW', 'SGD'];
    if (known.includes(m[1])) return m[1];
  }
  return null;
}

function extractAccountNumber(text) {
  const m = text.match(/\b(\d{12})\b/);
  return m ? m[1] : null;
}

function extractDateRange(text) {
  const lower = text.toLowerCase();
  const now = new Date();
  let from = null;
  let to = null;

  if (/today/i.test(lower)) {
    from = new Date(now.getFullYear(), now.getMonth(), now.getDate());
    to = now;
  } else if (/yesterday/i.test(lower)) {
    from = new Date(now.getFullYear(), now.getMonth(), now.getDate() - 1);
    to = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  } else if (/this\s+week/i.test(lower)) {
    const day = now.getDay();
    from = new Date(now.getFullYear(), now.getMonth(), now.getDate() - day);
    to = now;
  } else if (/this\s+month/i.test(lower)) {
    from = new Date(now.getFullYear(), now.getMonth(), 1);
    to = now;
  } else {
    const m = lower.match(/last\s+(\d+)\s+days?/i);
    if (m) {
      from = new Date(now.getTime() - parseInt(m[1], 10) * 86400000);
      to = now;
    }
  }

  if (from) {
    return {
      from: from.toISOString().slice(0, 19),
      to: to.toISOString().slice(0, 19),
    };
  }
  return null;
}


/* ═══════════════════════════════════════════════════════════════════
   §4  ERROR CODE → PLAIN ENGLISH
   ═══════════════════════════════════════════════════════════════════ */

const ERROR_EXPLANATIONS = {
  VALIDATION_FAILED: {
    title: 'Validation Failed',
    explanation: "The payment didn't pass one or more validation rules. Check the amount, account numbers, and currency code — one of them likely didn't meet the system's requirements.",
    remediation: 'Review the payment details, correct any invalid fields, and resubmit.',
  },
  INSUFFICIENT_FUNDS: {
    title: 'Insufficient Funds',
    explanation: "The source account doesn't have enough balance to cover this payment amount.",
    remediation: 'Ensure the source account has sufficient funds before attempting the payment again.',
  },
  INVALID_ACCOUNT: {
    title: 'Invalid Account',
    explanation: "One of the account numbers doesn't exist in the system or is formatted incorrectly.",
    remediation: 'Double-check both the source and destination account numbers (must be 12 digits).',
  },
  INVALID_CURRENCY: {
    title: 'Unsupported Currency',
    explanation: 'The currency code provided is not supported by the system.',
    remediation: 'Use a valid 3-letter ISO 4217 currency code (e.g., USD, EUR, GBP).',
  },
  INVALID_AMOUNT: {
    title: 'Invalid Amount',
    explanation: 'The payment amount is zero, negative, or exceeds the maximum allowed limit (1,000,000).',
    remediation: 'Enter a valid amount between 0.01 and 1,000,000.',
  },
  DUPLICATE_PAYMENT: {
    title: 'Duplicate Payment',
    explanation: 'A payment with the same idempotency key already exists but with different details.',
    remediation: 'Use a unique idempotency key or verify the existing payment with the same key.',
  },
  INVALID_STATUS_TRANSITION: {
    title: 'Invalid Status Transition',
    explanation: "The system tried to move this payment to a status that isn't allowed from its current state.",
    remediation: 'Follow the payment lifecycle: CREATED → VALIDATED → SENT → COMPLETED.',
  },
  PAYMENT_NOT_FOUND: {
    title: 'Payment Not Found',
    explanation: "The requested payment ID doesn't exist in the database.",
    remediation: 'Verify the payment ID is correct.',
  },
  PROCESSING_ERROR: {
    title: 'Processing Error',
    explanation: 'Something went wrong on the server during payment processing. This could be a temporary internal issue.',
    remediation: 'Try again in a few minutes. If it persists, contact system support.',
  },
  NETWORK_ERROR: {
    title: 'Network / Gateway Error',
    explanation: "The payment gateway couldn't be reached or rejected the connection. This is usually a temporary issue.",
    remediation: 'Wait a moment and retry. The gateway may be experiencing downtime.',
  },
  FRAUD_DETECTED: {
    title: 'Fraud Detected',
    explanation: 'The fraud detection engine flagged this transaction as potentially fraudulent and it was automatically rejected.',
    remediation: 'Contact the fraud review team or check the Fraud Dashboard for details.',
  },
  MANUAL_FAILURE: {
    title: 'Manually Failed',
    explanation: 'An administrator manually marked this payment as failed.',
    remediation: 'Contact the administrator who failed this payment for more context.',
  },
};

function explainErrorCode(code) {
  return ERROR_EXPLANATIONS[code] || {
    title: code || 'Unknown Error',
    explanation: `The payment failed with error code "${code || 'unknown'}". This is not a recognized standard error code.`,
    remediation: 'Contact system support for assistance.',
  };
}


/* ═══════════════════════════════════════════════════════════════════
   §5  RISK SCORE INTERPRETATION
   ═══════════════════════════════════════════════════════════════════ */

function interpretRisk(fraudProbability, highRisk) {
  if (fraudProbability == null) {
    return {
      level: 'UNKNOWN',
      label: 'Not Assessed',
      color: '#6B7280',
      description: 'Fraud probability has not been calculated for this payment.',
      recommendations: ['Process this payment through the standard lifecycle to generate a fraud assessment.'],
    };
  }

  const score = Number(fraudProbability);

  if (score < 30) {
    return {
      level: 'LOW',
      label: 'Low Risk',
      color: '#10B981',
      description: `Fraud probability is ${score.toFixed(1)}% — this payment appears safe.`,
      recommendations: ['Safe to process normally.', 'No additional review needed.'],
    };
  }
  if (score < 60) {
    return {
      level: 'MEDIUM',
      label: 'Medium Risk',
      color: '#F59E0B',
      description: `Fraud probability is ${score.toFixed(1)}% — exercise caution.`,
      recommendations: [
        'Verify the sender and recipient account details.',
        'Check if the transaction amount is unusual for this account.',
        'Consider a brief manual review before processing.',
      ],
    };
  }
  if (score < 90) {
    return {
      level: 'HIGH',
      label: 'High Risk',
      color: '#EF4444',
      description: `Fraud probability is ${score.toFixed(1)}% — manual review strongly recommended.`,
      recommendations: [
        'Do NOT process without manual review.',
        'Verify account ownership and transaction legitimacy.',
        'Check the account\'s recent transaction history for anomalies.',
        'Consider contacting the account holder directly.',
      ],
    };
  }
  return {
    level: 'CRITICAL',
    label: 'Critical Risk',
    color: '#DC2626',
    description: `Fraud probability is ${score.toFixed(1)}% — extremely high risk!`,
    recommendations: [
      'STOP — do not process this payment.',
      'Escalate to the fraud investigation team immediately.',
      'Freeze the source account pending investigation.',
      'Document all available transaction details for audit.',
    ],
  };
}


/* ═══════════════════════════════════════════════════════════════════
   §6  INTENT HANDLERS  (each returns a structured response)
   ═══════════════════════════════════════════════════════════════════ */

async function handlePaymentDetail(text) {
  const id = extractPaymentId(text);
  if (!id) {
    return { type: 'text', content: "I need a payment ID to look that up. Try something like **\"details for payment #5\"**." };
  }
  try {
    const { data } = await paymentAPI.getPayment(id);
    return { type: 'payment-detail', content: data };
  } catch (err) {
    if (err.response?.status === 404) {
      return { type: 'text', content: `Payment **#${id}** was not found. Double-check the ID and try again.` };
    }
    return { type: 'error', content: `Failed to fetch payment #${id}: ${err.message}` };
  }
}


async function handleExplainFailure(text) {
  const id = extractPaymentId(text);
  if (!id) {
    return { type: 'text', content: "I need a payment ID to explain a failure. Try **\"why did payment #3 fail?\"**" };
  }

  try {
    const [paymentRes, historyRes, validationsRes] = await Promise.all([
      paymentAPI.getPayment(id),
      paymentAPI.getPaymentHistory(id).catch(() => ({ data: [] })),
      paymentAPI.getPaymentValidations(id).catch(() => ({ data: [] })),
    ]);

    const payment = paymentRes.data;
    if (payment.status !== 'FAILED') {
      return {
        type: 'text',
        content: `Payment **#${id}** is currently in **${payment.status}** status — it hasn't failed. Would you like to see its details instead?`,
      };
    }

    const errorInfo = explainErrorCode(payment.errorCode);
    const failedValidations = (validationsRes.data || []).filter(v => !v.passed);

    return {
      type: 'failure-explanation',
      content: {
        payment,
        errorInfo,
        history: historyRes.data || [],
        failedValidations,
      },
    };
  } catch (err) {
    if (err.response?.status === 404) {
      return { type: 'text', content: `Payment **#${id}** was not found.` };
    }
    return { type: 'error', content: `Error analysing payment #${id}: ${err.message}` };
  }
}


async function handleAuditTrail(text) {
  const id = extractPaymentId(text);
  if (!id) {
    return { type: 'text', content: "I need a payment ID to show the audit trail. Try **\"audit trail for payment #1\"**." };
  }

  try {
    const [paymentRes, historyRes, validationsRes] = await Promise.all([
      paymentAPI.getPayment(id),
      paymentAPI.getPaymentHistory(id),
      paymentAPI.getPaymentValidations(id).catch(() => ({ data: [] })),
    ]);

    return {
      type: 'audit-trail',
      content: {
        payment: paymentRes.data,
        history: historyRes.data,
        validations: validationsRes.data || [],
      },
    };
  } catch (err) {
    if (err.response?.status === 404) {
      return { type: 'text', content: `Payment **#${id}** was not found.` };
    }
    return { type: 'error', content: `Error fetching audit trail: ${err.message}` };
  }
}


async function handleDashboardInsights() {
  try {
    const [successRes, distRes, volumeRes] = await Promise.all([
      analyticsAPI.getSuccessRate(),
      analyticsAPI.getStatusDistribution(),
      analyticsAPI.getVolume(),
    ]);

    let fraudStats = null;
    try {
      const fraudRes = await adminFraudAPI.getStats();
      fraudStats = fraudRes.data;
    } catch (_) {
      // fraud service may be unavailable — that's fine
    }

    return {
      type: 'dashboard-insights',
      content: {
        successRate: successRes.data,
        distribution: distRes.data,
        volume: volumeRes.data,
        fraud: fraudStats,
      },
    };
  } catch (err) {
    return { type: 'error', content: `Error fetching dashboard data: ${err.message}` };
  }
}


async function handleRiskAssessment(text) {
  const id = extractPaymentId(text);
  if (!id) {
    return { type: 'text', content: "I need a payment ID to assess risk. Try **\"risk score for payment #5\"**." };
  }

  try {
    const { data } = await paymentAPI.getPayment(id);
    const risk = interpretRisk(data.fraudProbability, data.highRisk);
    return {
      type: 'risk-assessment',
      content: { payment: data, risk },
    };
  } catch (err) {
    if (err.response?.status === 404) {
      return { type: 'text', content: `Payment **#${id}** was not found.` };
    }
    return { type: 'error', content: `Error assessing risk: ${err.message}` };
  }
}


async function handleFilterPayments(text) {
  const params = {};

  const status = extractStatus(text);
  if (status) params.status = status;

  const currency = extractCurrency(text);
  if (currency) params.currency = currency;

  const account = extractAccountNumber(text);
  if (account) params.account = account;

  const dates = extractDateRange(text);
  if (dates) {
    params['date-from'] = dates.from;
    params['date-to'] = dates.to;
  }

  params.limit = 20;
  params.offset = 0;

  // Build a human-readable filter description
  const filterParts = [];
  if (status) filterParts.push(`status = **${status}**`);
  if (currency) filterParts.push(`currency = **${currency}**`);
  if (account) filterParts.push(`account = **${account}**`);
  if (dates) filterParts.push(`date range applied`);

  const filterDesc = filterParts.length
    ? `Filtering by ${filterParts.join(', ')}:`
    : 'Showing recent payments:';

  try {
    const { data } = await paymentAPI.listPayments(params);
    return {
      type: 'payment-table',
      content: { payments: data, filterDescription: filterDesc, filters: params },
    };
  } catch (err) {
    return { type: 'error', content: `Error fetching payments: ${err.message}` };
  }
}


async function handleSearchPayment(text) {
  const id = extractPaymentId(text);
  if (id) {
    return handlePaymentDetail(text);
  }
  // Fall back to filtered list
  return handleFilterPayments(text);
}


function handleHelp() {
  return {
    type: 'help',
    content: {
      greeting: "Hi! I'm your **Payment Intelligence Copilot**. Here's what I can do:",
      capabilities: [
        { icon: '🔍', label: 'Search Payments', example: '"Find payment #42" or "Show all failed payments"' },
        { icon: '❌', label: 'Explain Failures', example: '"Why did payment #3 fail?"' },
        { icon: '📜', label: 'Audit Trails', example: '"Show audit trail for payment #7"' },
        { icon: '📊', label: 'Dashboard Insights', example: '"What\'s the success rate?" or "Dashboard"' },
        { icon: '🛡️', label: 'Risk Assessment', example: '"Risk score for payment #5"' },
        { icon: '🔎', label: 'Filter Payments', example: '"Show completed payments in USD today"' },
      ],
    },
  };
}


/* ═══════════════════════════════════════════════════════════════════
   §7  MAIN ENTRY POINT
   ═══════════════════════════════════════════════════════════════════ */

/**
 * Process a natural-language message and return a structured response.
 * @param {string} message — the user's query
 * @returns {Promise<{type: string, content: any}>}
 */
export async function processMessage(message) {
  const trimmed = (message || '').trim();
  if (!trimmed) {
    return { type: 'text', content: "I didn't catch that. Type **help** to see what I can do." };
  }

  // Classify intent
  let matchedIntent = INTENTS.GENERAL_HELP;
  for (const rule of INTENT_RULES) {
    if (rule.patterns.some(p => p.test(trimmed))) {
      matchedIntent = rule.intent;
      break;
    }
  }

  // Dispatch
  switch (matchedIntent) {
    case INTENTS.PAYMENT_DETAIL:     return handlePaymentDetail(trimmed);
    case INTENTS.EXPLAIN_FAILURE:    return handleExplainFailure(trimmed);
    case INTENTS.AUDIT_TRAIL:        return handleAuditTrail(trimmed);
    case INTENTS.DASHBOARD_INSIGHTS: return handleDashboardInsights();
    case INTENTS.RISK_ASSESSMENT:    return handleRiskAssessment(trimmed);
    case INTENTS.FILTER_PAYMENTS:    return handleFilterPayments(trimmed);
    case INTENTS.SEARCH_PAYMENT:     return handleSearchPayment(trimmed);
    case INTENTS.GENERAL_HELP:
    default:
      return handleHelp();
  }
}


/* ═══════════════════════════════════════════════════════════════════
   §8  SUGGESTED PROMPTS  (context-aware)
   ═══════════════════════════════════════════════════════════════════ */

export function getSuggestedPrompts(recentPaymentId) {
  const base = [
    'Show dashboard insights',
    'List recent failed payments',
    "What's the success rate?",
  ];
  if (recentPaymentId) {
    base.unshift(`Explain payment #${recentPaymentId}`);
    base.push(`Risk score for payment #${recentPaymentId}`);
    base.push(`Audit trail for payment #${recentPaymentId}`);
  }
  return base;
}

export { INTENTS, interpretRisk, explainErrorCode };
