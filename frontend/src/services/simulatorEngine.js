/**
 * simulatorEngine.js — Payment Lifecycle Replay & Simulation Engine
 *
 * Provides:
 * 1. Duration calculation & event formatting from audit records
 * 2. ReplayEngine for step-by-step or timed replay of historical audit events
 * 3. SimulationEngine for client-side sandbox execution with failure injection & retry handling
 */

import { paymentAPI } from './api';

/* ═══════════════════════════════════════════════════════════════════
   §1  FAILURE TYPES & DEFINITIONS
   ═══════════════════════════════════════════════════════════════════ */

export const FAILURE_TYPES = {
  NONE: {
    id: 'NONE',
    label: 'None (Happy Path)',
    stage: null,
    errorCode: null,
    errorMessage: null,
  },
  VALIDATION_FAILED: {
    id: 'VALIDATION_FAILED',
    label: 'Validation Failure',
    stage: 'CREATED',
    errorCode: 'VALIDATION_FAILED',
    errorMessage: 'Payment failed validation checks (Rule #2: Amount exceeds single-transaction limit)',
  },
  DUPLICATE_PAYMENT: {
    id: 'DUPLICATE_PAYMENT',
    label: 'Duplicate Payment',
    stage: 'CREATED',
    errorCode: 'DUPLICATE_PAYMENT',
    errorMessage: 'Payment with same idempotency key exists with different details',
  },
  INSUFFICIENT_FUNDS: {
    id: 'INSUFFICIENT_FUNDS',
    label: 'Insufficient Funds',
    stage: 'CREATED',
    errorCode: 'INSUFFICIENT_FUNDS',
    errorMessage: 'Source account has insufficient funds to fulfill payment',
  },
  NETWORK_TIMEOUT: {
    id: 'NETWORK_TIMEOUT',
    label: 'Network Timeout / Retry',
    stage: 'VALIDATED',
    errorCode: 'NETWORK_ERROR',
    errorMessage: 'Payment gateway timeout during transmission (simulated connection reset)',
  },
  GATEWAY_REJECTION: {
    id: 'GATEWAY_REJECTION',
    label: 'Gateway Rejection',
    stage: 'VALIDATED',
    errorCode: 'NETWORK_ERROR',
    errorMessage: 'Payment gateway rejected the transaction payload',
  },
  PROCESSING_ERROR: {
    id: 'PROCESSING_ERROR',
    label: 'Processing Error',
    stage: 'SENT',
    errorCode: 'PROCESSING_ERROR',
    errorMessage: 'Internal error during settlement confirmation',
  },
};

/* ═══════════════════════════════════════════════════════════════════
   §2  TIME & DURATION HELPERS
   ═══════════════════════════════════════════════════════════════════ */

export function calculateDurations(history) {
  if (!Array.isArray(history) || history.length === 0) return [];

  return history.map((item, index) => {
    let durationMs = 0;
    if (index > 0 && history[index - 1].timestamp && item.timestamp) {
      const prev = new Date(history[index - 1].timestamp).getTime();
      const curr = new Date(item.timestamp).getTime();
      if (!isNaN(prev) && !isNaN(curr) && curr >= prev) {
        durationMs = curr - prev;
      }
    }

    return {
      ...item,
      durationMs,
      formattedDuration: formatDuration(durationMs),
    };
  });
}

export function formatDuration(ms) {
  if (ms == null || isNaN(ms) || ms === 0) return '< 1ms';
  if (ms < 1000) return `${ms}ms`;
  if (ms < 60000) return `${(ms / 1000).toFixed(2)}s`;
  const mins = Math.floor(ms / 60000);
  const secs = ((ms % 60000) / 1000).toFixed(1);
  return `${mins}m ${secs}s`;
}

/* ═══════════════════════════════════════════════════════════════════
   §3  REPLAY ENGINE (Historical Audit Trail Playback)
   ═══════════════════════════════════════════════════════════════════ */

export class ReplayEngine {
  constructor(historyEvents, onUpdate, onComplete) {
    this.rawEvents = historyEvents || [];
    this.events = calculateDurations(this.rawEvents);
    this.onUpdate = onUpdate || (() => {});
    this.onComplete = onComplete || (() => {});

    this.currentIndex = -1;
    this.speed = 1;
    this.isPlaying = false;
    this.timer = null;
  }

  setSpeed(newSpeed) {
    this.speed = newSpeed;
  }

  start() {
    if (this.events.length === 0) return;
    if (this.currentIndex >= this.events.length - 1) {
      this.currentIndex = -1;
    }
    this.isPlaying = true;
    this.nextStep();
  }

  pause() {
    this.isPlaying = false;
    if (this.timer) {
      clearTimeout(this.timer);
      this.timer = null;
    }
  }

  stop() {
    this.pause();
    this.currentIndex = -1;
    this.notify();
  }

  stepForward() {
    this.pause();
    if (this.currentIndex < this.events.length - 1) {
      this.currentIndex++;
      this.notify();
    }
  }

  stepBackward() {
    this.pause();
    if (this.currentIndex > 0) {
      this.currentIndex--;
      this.notify();
    }
  }

  jumpTo(index) {
    this.pause();
    if (index >= 0 && index < this.events.length) {
      this.currentIndex = index;
      this.notify();
    }
  }

  nextStep() {
    if (!this.isPlaying) return;

    this.currentIndex++;
    this.notify();

    if (this.currentIndex >= this.events.length - 1) {
      this.isPlaying = false;
      this.onComplete();
      return;
    }

    const nextEvent = this.events[this.currentIndex + 1];
    const baseDelay = Math.max(800, Math.min(nextEvent.durationMs || 1200, 3000));
    const actualDelay = baseDelay / this.speed;

    this.timer = setTimeout(() => {
      this.nextStep();
    }, actualDelay);
  }

  notify() {
    const currentEvent = this.events[this.currentIndex] || null;
    const historySoFar = this.currentIndex >= 0 ? this.events.slice(0, this.currentIndex + 1) : [];
    this.onUpdate({
      currentIndex: this.currentIndex,
      totalSteps: this.events.length,
      currentEvent,
      historySoFar,
      currentStatus: currentEvent ? currentEvent.newStatus : 'INITIAL',
      isPlaying: this.isPlaying,
    });
  }
}

/* ═══════════════════════════════════════════════════════════════════
   §4  SIMULATION ENGINE (Sandbox with Failure Injection & Retries)
   ═══════════════════════════════════════════════════════════════════ */

export class SimulationEngine {
  constructor(config = {}, onLogEvent, onStateChange, onComplete) {
    this.config = {
      sourceAccount: config.sourceAccount || '100000000001',
      destinationAccount: config.destinationAccount || '200000000002',
      amount: config.amount || 250.00,
      currency: config.currency || 'USD',
      failureType: config.failureType || 'NONE',
      maxRetries: config.maxRetries ?? 3,
      retryDelayMs: config.retryDelayMs ?? 1500,
      exponentialBackoff: config.exponentialBackoff ?? true,
      speed: config.speed || 1,
    };

    this.onLogEvent = onLogEvent || (() => {});
    this.onStateChange = onStateChange || (() => {});
    this.onComplete = onComplete || (() => {});

    this.isRunning = false;
    this.isPaused = false;
    this.timer = null;
    this.logs = [];
    this.currentStatus = 'IDLE';
    this.retryCount = 0;
    this.startTime = null;
  }

  log(level, stage, message, details = {}) {
    const event = {
      id: Date.now() + Math.random(),
      timestamp: new Date().toISOString(),
      level, // 'info' | 'success' | 'warn' | 'error' | 'retry'
      stage,
      message,
      details,
    };
    this.logs.push(event);
    this.onLogEvent(event, [...this.logs]);
  }

  setStatus(status) {
    this.currentStatus = status;
    this.onStateChange({
      status: this.currentStatus,
      retryCount: this.retryCount,
      maxRetries: this.config.maxRetries,
      logs: this.logs,
      isRunning: this.isRunning,
    });
  }

  async start() {
    this.isRunning = true;
    this.isPaused = false;
    this.logs = [];
    this.retryCount = 0;
    this.startTime = Date.now();

    this.log('info', 'START', `Initiating payment simulation: ${this.config.amount} ${this.config.currency}`);
    this.log('info', 'CONFIG', `Config: Failure = ${this.config.failureType}, Max Retries = ${this.config.maxRetries}, Backoff = ${this.config.exponentialBackoff}`);

    await this.delay(600);

    // Step 1: CREATED
    this.setStatus('CREATED');
    this.log('info', 'CREATED', `Payment record initialized for ${this.config.sourceAccount} -> ${this.config.destinationAccount}`);

    const failureDef = FAILURE_TYPES[this.config.failureType] || FAILURE_TYPES.NONE;

    if (failureDef.stage === 'CREATED') {
      await this.delay(1000);
      this.setStatus('FAILED');
      this.log('error', 'FAILED', `Payment creation failed: ${failureDef.errorMessage}`, {
        errorCode: failureDef.errorCode,
      });
      this.finish(false, failureDef.errorCode, failureDef.errorMessage);
      return;
    }

    await this.delay(1200);

    // Step 2: VALIDATED
    this.setStatus('VALIDATED');
    this.log('success', 'VALIDATED', 'Validation rules passed: Amount, account state & balance verified.');

    if (failureDef.stage === 'VALIDATED') {
      await this.handleValidationStageFailure(failureDef);
      return;
    }

    await this.delay(1200);

    // Step 3: SENT
    this.setStatus('SENT');
    this.log('info', 'SENT', 'Transmitted transaction payload to mock payment gateway.');

    if (failureDef.stage === 'SENT') {
      await this.delay(1000);
      this.setStatus('FAILED');
      this.log('error', 'FAILED', `Gateway settlement confirmation failed: ${failureDef.errorMessage}`, {
        errorCode: failureDef.errorCode,
      });
      this.finish(false, failureDef.errorCode, failureDef.errorMessage);
      return;
    }

    await this.delay(1200);

    // Step 4: COMPLETED
    this.setStatus('COMPLETED');
    this.log('success', 'COMPLETED', 'Payment successfully settled and confirmed by destination ledger!');
    this.finish(true, null, null);
  }

  async handleValidationStageFailure(failureDef) {
    let successOnRetry = false;

    while (this.retryCount < this.config.maxRetries) {
      this.setStatus('FAILED');
      this.log('warn', 'FAILED', `Transmission failed: ${failureDef.errorMessage} (Attempt ${this.retryCount + 1}/${this.config.maxRetries + 1})`, {
        errorCode: failureDef.errorCode,
      });

      this.retryCount++;

      let delayMs = this.config.retryDelayMs;
      if (this.config.exponentialBackoff) {
        delayMs = this.config.retryDelayMs * Math.pow(2, this.retryCount - 1);
      }

      this.log('retry', 'RETRY_WAIT', `Scheduling retry attempt #${this.retryCount} in ${formatDuration(delayMs)}...`);
      await this.delay(delayMs);

      this.log('info', 'RETRY_EXEC', `Executing retry #${this.retryCount}: Resetting status to VALIDATED...`);
      this.setStatus('VALIDATED');
      await this.delay(1000);

      // 50% chance of recovery on subsequent retries if it was a network timeout
      if (failureDef.id === 'NETWORK_TIMEOUT' && this.retryCount >= 2) {
        successOnRetry = true;
        break;
      }
    }

    if (successOnRetry) {
      this.log('success', 'SENT', 'Gateway re-transmission succeeded after retry recovery!');
      this.setStatus('SENT');
      await this.delay(1000);
      this.setStatus('COMPLETED');
      this.log('success', 'COMPLETED', 'Payment completed successfully after retry recovery!');
      this.finish(true, null, null);
    } else {
      this.setStatus('FAILED');
      this.log('error', 'FAILED', `Max retries (${this.config.maxRetries}) exhausted. Payment permanently marked as FAILED.`, {
        errorCode: failureDef.errorCode,
      });
      this.finish(false, failureDef.errorCode, failureDef.errorMessage);
    }
  }

  delay(ms) {
    const adjustedMs = Math.max(100, ms / (this.config.speed || 1));
    return new Promise((resolve) => {
      this.timer = setTimeout(resolve, adjustedMs);
    });
  }

  stop() {
    this.isRunning = false;
    if (this.timer) {
      clearTimeout(this.timer);
      this.timer = null;
    }
    this.setStatus('IDLE');
    this.log('info', 'STOP', 'Simulation stopped by user.');
  }

  finish(success, errorCode, errorMessage) {
    this.isRunning = false;
    const totalDurationMs = Date.now() - this.startTime;
    this.onComplete({
      success,
      finalStatus: this.currentStatus,
      totalDurationMs,
      formattedTotalDuration: formatDuration(totalDurationMs),
      retryAttempts: this.retryCount,
      errorCode,
      errorMessage,
      logs: this.logs,
    });
  }
}
