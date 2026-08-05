# Ctrl+Pay Hybrid Fraud Detection System Enhancement Roadmap

**Phase:** Comprehensive Production-Grade Implementation  
**Version:** 1.0  
**Date:** August 5, 2026  
**Status:** In Progress - Phase 1 Foundation

---

## Executive Summary

This document outlines a comprehensive, multi-phase enhancement to Ctrl+Pay's hybrid fraud detection system. The current implementation combines rule-based detection (10 rules) with XGBoost ML models, executing within the payment lifecycle. We are enhancing this system to production-grade standards with advanced risk management, observability, and resilience patterns.

**Current State:**
- ✅ Hybrid scoring (rules + ML) in FraudDetectionService
- ✅ 10 fraud rules with weighted scoring
- ✅ FraudDecisionEngine with configurable thresholds
- ✅ Fraud assessment audit trail in database
- ⚠️ Limited manual review workflow
- ⚠️ No ML model versioning/tracking
- ⚠️ Limited observability and metrics
- ⚠️ No risk-based payment routing

---

## Phase 1: Foundation & Advanced Detection Layer

**Timeline:** 2-3 weeks  
**Priority:** CRITICAL  
**Status:** Starting

### Objectives

1. Implement ML model versioning and lifecycle management
2. Add 10+ specialized fraud rules (behavioral, correlation patterns)
3. Enhance decision engine with dynamic thresholds
4. Expand audit trail for fraud assessment analysis
5. Create comprehensive logging and error handling framework

### Components

#### 1.1 ML Model Versioning & Management

**Problem:** Current implementation uses single XGBoost model without version tracking.

**Solution:**

```
ml_models table (new)
├─ id: Primary key
├─ model_name: String (e.g., "xgboost_paysim_v1")
├─ model_version: String (semantic versioning)
├─ model_path: Path to pickled model file
├─ training_date: When model was trained
├─ training_dataset: Name/description of training data
├─ model_metrics: JSON {accuracy, precision, recall, f1, auc}
├─ is_active: Boolean (currently deployed)
├─ deployment_date: When activated
├─ retire_date: When superseded
├─ deployment_context: JSON {prod, staging, test}
├─ created_at, updated_at

ml_model_predictions table (new) - for tracking model performance
├─ id: Primary key
├─ ml_model_id: FK to ml_models
├─ payment_id: FK to payments
├─ predicted_fraud_probability: Score from model
├─ actual_fraud_outcome: True/false (verified later)
├─ is_correct_prediction: Boolean
├─ prediction_confidence: Confidence score
├─ created_at
```

**Implementation:**

1. Create `MLModelRegistry` component
   - Load active model on startup
   - Cache in memory with version metadata
   - Support hot-swap to new model version
   - Implement model evaluation metrics

2. Create `MLModelService`
   - Manage model versions
   - Track prediction accuracy
   - Support model rollback
   - Generate performance reports

3. Update `FraudDetectionService`
   - Use MLModelRegistry for predictions
   - Log model version with each assessment
   - Handle model unavailability gracefully

#### 1.2 Advanced Fraud Rules (10+ Rules)

**Current Rules:** 10 built-in rules
**Enhanced Rules:** Adding 12+ specialized detectors

**New Rule Categories:**

1. **Behavioral Anomaly Rules:**
   - `UnusualTimePattern` - Transactions at unusual times
   - `UnusualLocationPattern` - Payment from new geographic region
   - `DeviceFingerprint` - Device ID/IP changes
   - `BehaviorDrift` - Customer behavior deviation from baseline

2. **Account Correlation Rules:**
   - `MultipleAccountsSourceRule` - Same source used for multiple destinations
   - `CyclicalTransactionPattern` - Round-trip circular transfers
   - `FundsCyclingRule` - Rapid fund flows between accounts
   - `AccountLinkageAnomalyRule` - Unusual account relationships

3. **Transaction Pattern Rules:**
   - `RapidSequentialTransactions` - Multiple txns in short timeframe
   - `SuspiciousBeneficiaryRule` - High-risk receiving accounts
   - `TransactionChainRule` - Txn chains suggesting layering

4. **Integration Rules:**
   - `CombinedRiskScoring` - Aggregate multiple rule signals
   - `ContextualAnomalyDetection` - Industry-specific patterns

**Rule Engine Enhancements:**

```java
// Enhanced FraudRule interface
public interface FraudRule {
    String getRuleName();
    String getDescription();
    RuleCategory getCategory();  // new
    double getWeight();
    double getThreshold();        // new - configurable trigger threshold
    boolean isEnabled();          // new - DB-backed enable/disable
    long getLastExecutionMs();    // new - performance tracking
    
    FraudRuleResult evaluate(
        PaymentRecord payment,
        AccountRecord sourceAccount,
        AccountRecord destinationAccount,
        PaymentHistoryContext context  // new - historical data
    );
}

// Rule result enhanced with confidence
public record FraudRuleResult(
    String ruleName,
    double score,                 // 0-100
    boolean triggered,
    String explanation,
    double confidence,            // 0-1 confidence in this result
    Map<String, Object> metrics   // rule-specific diagnostics
) {}
```

#### 1.3 Enhanced Decision Engine

**Current:** 3-threshold binary logic (APPROVED/SUSPICIOUS/REJECTED)
**Enhanced:** Dynamic thresholds with risk-based routing

**Configuration:**

```yaml
fraud:
  # Hybrid scoring weights
  rule-weight: 0.4              # Rule engine contribution
  ml-weight: 0.6                # ML model contribution
  
  # Decision thresholds (configurable per merchant/region)
  thresholds:
    auto-approve: 0.30          # Below = auto-approve
    manual-review: 0.75         # 0.30-0.75 = manual review
    auto-reject: 0.95           # Above = auto-reject
  
  # Risk-based routing
  risk-routing:
    low-risk:
      threshold-max: 0.30
      processing-lane: "FAST_TRACK"
      sla-minutes: 5
    medium-risk:
      threshold-min: 0.30
      threshold-max: 0.75
      processing-lane: "MANUAL_REVIEW"
      sla-minutes: 60
    high-risk:
      threshold-min: 0.75
      threshold-max: 0.95
      processing-lane: "ESCALATION"
      sla-minutes: 120
    critical-risk:
      threshold-min: 0.95
      processing-lane: "REJECTION"
      
  # Model fallback strategy
  ml-fallback:
    use-rule-only-on-failure: true
    retry-attempts: 2
    timeout-ms: 2000
```

**Decision Result Enhanced:**

```java
public record FraudDecisionResult(
    FraudDecision decision,           // APPROVED, SUSPICIOUS, REJECTED
    FraudRiskLevel riskLevel,         // LOW, MEDIUM, HIGH, CRITICAL
    BigDecimal hybridScore,           // 0-100
    BigDecimal ruleEngineScore,       // 0-100
    BigDecimal mlScore,               // 0-100
    String explanation,
    // NEW FIELDS:
    String processingLane,            // FAST_TRACK, MANUAL_REVIEW, ESCALATION, REJECTION
    long slaMinutes,                  // SLA for manual review
    double confidence,                // Overall confidence (0-1)
    Map<String, Double> ruleBreakdown, // Score per rule (for transparency)
    String mlModelVersion,            // Which model made prediction
    Optional<String> recommendedAction // For system operators
) {}
```

#### 1.4 Enhanced Audit Trail

**New fraud_assessment_v2 table:**

```sql
fraud_assessments
├─ id: BIGINT PRIMARY KEY
├─ payment_id: BIGINT NOT NULL UNIQUE
├─ assessment_timestamp: TIMESTAMP
├─ ml_model_version: VARCHAR (which model made prediction)
├─ ml_fraud_probability: DECIMAL(5,2)
├─ rule_engine_score: DECIMAL(5,2)
├─ hybrid_score: DECIMAL(5,2)
├─
├─ triggered_rules_json: JSON              # Enhanced: includes confidence
├─ rule_scores_json: JSON                  # Enhanced: detailed breakdown
├─ rule_performance_metrics_json: JSON     # NEW: timing, confidence
├─
├─ decision: VARCHAR(20)                   # APPROVED, SUSPICIOUS, REJECTED
├─ risk_level: VARCHAR(20)                 # LOW, MEDIUM, HIGH, CRITICAL
├─ processing_lane: VARCHAR(20)            # NEW: routing information
├─
├─ explanation: TEXT
├─ ml_model_explanation: TEXT              # NEW: model-specific explanation
├─
├─ confidence_score: DECIMAL(5,2)          # NEW: overall confidence
├─ confidence_factors: JSON                # NEW: why confident/not confident
├─
├─ reviewed_by: VARCHAR(255) NULL          # Admin who reviewed
├─ reviewed_at: TIMESTAMP NULL             # When reviewed
├─ reviewer_notes: TEXT NULL               # Admin review notes
├─ review_decision: VARCHAR(20) NULL       # OVERRIDE_APPROVE, OVERRIDE_REJECT
├─
├─ performance_metrics: JSON               # NEW: assessment performance
├─ created_at: TIMESTAMP
├─ updated_at: TIMESTAMP
```

**New audit_events table:**

```sql
fraud_audit_events
├─ id: BIGINT PRIMARY KEY
├─ assessment_id: BIGINT FK
├─ event_type: VARCHAR(50)  # RULE_TRIGGERED, DECISION_MADE, REVIEW_STARTED, etc.
├─ event_data: JSON         # Event-specific data
├─ triggered_by: VARCHAR(50) # SYSTEM, USER_ADMIN, API_CALL
├─ created_at: TIMESTAMP
```

### 1.5 Logging & Observability

**Enhanced logging framework:**

```java
@Component
public class FraudDetectionLogger {
    private static final Logger logger = LoggerFactory.getLogger(FraudDetectionLogger.class);
    
    // Structured logging for analysis
    public void logRuleExecution(FraudRuleResult result, long executionMs) {
        // JSON format: {timestamp, rule_name, score, triggered, execution_ms, ...}
    }
    
    public void logFraudAssessment(FraudAssessmentRecord assessment) {
        // Complete assessment with all metadata
    }
    
    public void logMLPrediction(MLPredictionRecord prediction) {
        // ML model performance tracking
    }
    
    public void logFraudDecision(FraudDecisionResult decision, PaymentRecord payment) {
        // Decision rationale
    }
    
    public void logAnomalyDetected(String anomalyType, Object context) {
        // Anomaly alerts
    }
}
```

### Database Schema Migration

```sql
-- New tables
CREATE TABLE ml_models (...)
CREATE TABLE ml_model_predictions (...)
CREATE TABLE fraud_audit_events (...)

-- Add columns to fraud_assessments
ALTER TABLE fraud_assessments 
ADD COLUMN ml_model_version VARCHAR(50),
ADD COLUMN processing_lane VARCHAR(20),
ADD COLUMN confidence_score DECIMAL(5,2),
ADD COLUMN confidence_factors JSON,
ADD COLUMN ml_model_explanation TEXT,
ADD COLUMN rule_performance_metrics_json JSON;

-- Indexes
CREATE INDEX idx_ml_models_active ON ml_models(is_active, deployment_date);
CREATE INDEX idx_ml_predictions_accuracy ON ml_model_predictions(ml_model_id, is_correct_prediction);
CREATE INDEX idx_audit_events_assessment ON fraud_audit_events(assessment_id, event_type);
```

### API Enhancements

**Enhanced POST /api/payments response:**

```json
{
  "id": 1,
  "status": "CREATED",
  "amount": 1000.00,
  "currency": "USD",
  "fraudAssessment": {
    "decision": "APPROVED",
    "riskLevel": "LOW",
    "processingLane": "FAST_TRACK",
    "confidence": 0.95,
    "hybridScore": 15.5,
    "scoreBreakdown": {
      "ruleEngine": 20.0,
      "mlModel": 12.0,
      "rules": {
        "LargeTransaction": 0,
        "AccountDrain": 0,
        "VelocityAnomaly": 5,
        ...
      }
    },
    "explanation": "Payment approved. Low fraud risk detected.",
    "modelVersion": "xgboost_paysim_v2",
    "requiredAction": "NONE"
  }
}
```

**New GET /api/fraud-assessments/{paymentId} endpoint:**

```json
{
  "assessment": {
    "id": 123,
    "paymentId": 1,
    "decision": "APPROVED",
    "riskLevel": "LOW",
    "hybridScore": 15.5,
    "modelVersion": "xgboost_paysim_v2",
    "ruleBreakdown": [...],
    "confidence": 0.95,
    "explanation": "...",
    "assessedAt": "2026-08-05T10:30:00Z",
    "reviewedBy": null,
    "auditTrail": [
      {
        "event": "ASSESSMENT_CREATED",
        "timestamp": "2026-08-05T10:30:00Z",
        "data": {...}
      }
    ]
  }
}
```

---

## Phase 2: Risk-Based Case Management

**Timeline:** 2-3 weeks  
**Depends on:** Phase 1 foundation

### Objectives

1. Build SUSPICIOUS case workflow system
2. Implement risk-based payment routing (AUTO_APPROVE/MANUAL_REVIEW/AUTO_REJECT lanes)
3. Create administrative review dashboard
4. Build appeal workflow with evidence submission
5. Implement SLA tracking and escalation

### Key Components

- `FraudCaseService` - Manage case lifecycle
- `ManualReviewQueue` - Queue management with priority/SLA
- `CaseAssignmentEngine` - Intelligent routing to reviewers
- `AppealWorkflow` - Evidence submission and reappraisal
- `FraudAdminController` - Review dashboard endpoints
- `SLATracker` - Monitoring task completion times

### Database Additions

```sql
fraud_cases table
├─ id, assessment_id, status (OPEN, RESOLVED), case_type
├─ assigned_to, assigned_at, resolved_at
├─ priority, sla_expires_at

case_evidence table
├─ id, case_id, evidence_type, data
├─ submitted_by, submitted_at

case_appeals table
├─ id, case_id, appeal_status
├─ appellant_notes, reviewer_notes
```

---

## Phase 3: Monitoring & Analytics Stack

**Timeline:** 2 weeks  
**Depends on:** Phase 1

### Objectives

1. Real-time fraud metrics collection
2. ML model performance dashboard
3. Anomaly detection on fraud patterns
4. Compliance reporting suite

### Components

- `MetricsCollector` - Centralized metrics
- `FraudAnalyticsService` - Analytics queries
- `PerformanceMonitor` - ML model tracking
- `AnomalyDetector` - Pattern anomalies
- Grafana/Prometheus integration

---

## Phase 4: Performance & Resilience

**Timeline:** 1-2 weeks  
**Depends on:** Phase 1-2

### Objectives

1. Caching layer for rule evaluation
2. Async fraud scoring
3. Circuit breaker for ML service
4. Query optimization

### Components

- `ScoringCache` - In-memory caching
- `AsyncScoringExecutor` - Background processing
- `MLServiceCircuitBreaker` - Resilience pattern
- Database query optimization

---

## Implementation Roadmap Summary

| Phase | Duration | Key Features | Dependencies |
|-------|----------|--------------|--------------|
| 1 | 2-3 weeks | ML versioning, advanced rules, enhanced decision engine, audit trail | None |
| 2 | 2-3 weeks | Case management, manual review workflow, SLA tracking | Phase 1 |
| 3 | 2 weeks | Monitoring dashboards, model performance tracking | Phase 1 |
| 4 | 1-2 weeks | Caching, async processing, resilience patterns | Phases 1-2 |

**Total Timeline:** 7-10 weeks for complete production deployment

---

## Success Criteria

### Phase 1
- [ ] ML model versioning system operational
- [ ] 12+ fraud rules implemented and running
- [ ] Enhanced audit trail capturing all assessments
- [ ] New API endpoints returning expected data
- [ ] Logging framework capturing all fraud operations
- [ ] 90%+ unit test coverage for new components

### Phase 2
- [ ] Manual review workflow functional
- [ ] SLA tracking working with alerts
- [ ] Admin dashboard usable by operations team
- [ ] Appeal workflow tested end-to-end

### Phase 3
- [ ] Real-time dashboards showing fraud metrics
- [ ] Model performance tracking automated
- [ ] 95%+ dashboard uptime

### Phase 4
- [ ] Fraud scoring latency reduced by 40%+
- [ ] ML service failures handled gracefully
- [ ] 99%+ system availability

---

## Risk Mitigation

| Risk | Impact | Mitigation |
|------|--------|-----------|
| ML model performance degradation | High | Versioning system, automatic fallback to rules |
| Database performance under load | High | Query optimization, caching layer |
| Feature parity with legacy system | Medium | Comprehensive testing, gradual migration |
| Compliance/audit trail gaps | High | Rigorous audit logging, compliance review |

---

## Configuration Strategy

All thresholds, weights, and rules configurable via:
1. `application.properties` - Dev/staging/prod defaults
2. Database tables - Runtime updates without restart
3. Admin API - On-the-fly adjustments
4. Feature flags - Gradual rollout of new rules/features

---

**Next Steps:** Begin Phase 1 implementation with ML model versioning framework.


