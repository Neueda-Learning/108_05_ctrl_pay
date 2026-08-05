-- ========================================
-- Phase 1: ML Model Management & Advanced Fraud Detection
-- Database Schema Migration V2.0
-- ========================================

-- Table: ml_models
-- Tracks trained ML model versions and their deployment status
CREATE TABLE IF NOT EXISTS ml_models (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Unique model version ID',
    model_name VARCHAR(100) NOT NULL COMMENT 'Model identifier (e.g., xgboost_paysim)',
    model_version VARCHAR(50) NOT NULL COMMENT 'Semantic version (e.g., 1.0.0, 1.1.0-rc1)',

    -- Model metadata
    description TEXT NULL COMMENT 'Human-readable description',
    model_type VARCHAR(50) NOT NULL COMMENT 'ML framework: XGBOOST, RANDOM_FOREST, NEURAL_NET',
    model_path VARCHAR(500) NOT NULL COMMENT 'Path to serialized model file (pickle, joblib, etc.)',

    -- Training information
    training_date TIMESTAMP NOT NULL COMMENT 'When model was trained',
    training_dataset_name VARCHAR(255) NOT NULL COMMENT 'Training dataset identifier',
    training_dataset_size INT UNSIGNED COMMENT 'Number of samples used in training',
    training_duration_seconds INT UNSIGNED COMMENT 'Time taken to train (seconds)',

    -- Model performance metrics (0-100 or 0-1 scale)
    accuracy_score DECIMAL(5, 2) COMMENT 'Overall accuracy (0-100)',
    precision_score DECIMAL(5, 2) COMMENT 'Precision for fraud class (0-100)',
    recall_score DECIMAL(5, 2) COMMENT 'Recall/Sensitivity (0-100)',
    f1_score DECIMAL(5, 2) COMMENT 'F1 score (0-100)',
    auc_score DECIMAL(5, 2) COMMENT 'Area under ROC curve (0-100)',
    false_positive_rate DECIMAL(5, 2) COMMENT 'FPR on test set',
    false_negative_rate DECIMAL(5, 2) COMMENT 'FNR on test set',

    -- Deployment status
    is_active BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Currently deployed and used',
    deployment_date TIMESTAMP NULL COMMENT 'When activated for production',
    retirement_date TIMESTAMP NULL COMMENT 'When replaced or deprecated',
    deployment_context VARCHAR(100) COMMENT 'Deployment environment: PROD, STAGING, TEST, DEV',

    -- Configuration
    feature_columns_json JSON COMMENT 'Expected input features as JSON array',
    hyperparameters_json JSON COMMENT 'Model hyperparameters for reproducibility',

    -- Audit trail
    created_by VARCHAR(255) COMMENT 'User who created this model record',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uk_model_name_version UNIQUE (model_name, model_version),
    CONSTRAINT chk_model_type CHECK (model_type IN ('XGBOOST', 'RANDOM_FOREST', 'NEURAL_NET', 'ENSEMBLE')),
    CONSTRAINT chk_deployment_context CHECK (deployment_context IN ('PROD', 'STAGING', 'TEST', 'DEV')),

    INDEX idx_active_model (is_active, deployment_date) COMMENT 'Find currently active models',
    INDEX idx_model_name (model_name) COMMENT 'Lookup by model name',
    INDEX idx_created_at (created_at) COMMENT 'Timeline queries'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='ML model versioning and deployment tracking';

-- Table: ml_model_predictions
-- Tracks individual predictions for model performance monitoring
CREATE TABLE IF NOT EXISTS ml_model_predictions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Prediction record ID',
    ml_model_id BIGINT NOT NULL COMMENT 'FK to ml_models',
    payment_id BIGINT NOT NULL COMMENT 'FK to payments',
    assessment_id BIGINT COMMENT 'FK to fraud_assessments',

    -- Prediction data
    predicted_fraud_probability DECIMAL(5, 2) NOT NULL COMMENT 'Model output (0-100 scale)',
    prediction_confidence DECIMAL(5, 2) COMMENT 'Model confidence in prediction (0-100)',
    prediction_latency_ms INT UNSIGNED COMMENT 'ML service response time',

    -- Ground truth (populated later through feedback)
    ground_truth_fraud BOOLEAN COMMENT 'Actual fraud outcome (from chargeback, report, etc.)',
    ground_truth_source VARCHAR(100) COMMENT 'How we know ground truth: CHARGEBACK, CUSTOMER_REPORT, AUDIT',
    ground_truth_date TIMESTAMP COMMENT 'When ground truth became known',

    -- Prediction accuracy
    is_correct_prediction BOOLEAN COMMENT 'Did model predict correctly?',
    prediction_type VARCHAR(50) COMMENT 'TRUE_POSITIVE, TRUE_NEGATIVE, FALSE_POSITIVE, FALSE_NEGATIVE',

    -- Audit
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_ml_model FOREIGN KEY (ml_model_id) REFERENCES ml_models(id),
    CONSTRAINT fk_payment FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE CASCADE,
    CONSTRAINT fk_assessment FOREIGN KEY (assessment_id) REFERENCES fraud_assessments(id) ON DELETE SET NULL,

    INDEX idx_model_id (ml_model_id) COMMENT 'Model performance analysis',
    INDEX idx_payment_id (payment_id) COMMENT 'Find predictions for payment',
    INDEX idx_prediction_type (prediction_type) COMMENT 'Accuracy metrics by type',
    INDEX idx_accuracy_date (created_at, is_correct_prediction) COMMENT 'Time-series accuracy tracking'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='ML model prediction tracking for performance monitoring';

-- Table: fraud_audit_events
-- Detailed audit trail of fraud assessment workflow
CREATE TABLE IF NOT EXISTS fraud_audit_events (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'Event record ID',
    assessment_id BIGINT NOT NULL COMMENT 'FK to fraud_assessments',

    event_type VARCHAR(100) NOT NULL COMMENT 'Event classification: RULE_TRIGGERED, DECISION_MADE, REVIEW_STARTED, OVERRIDDEN',
    event_timestamp TIMESTAMP NOT NULL COMMENT 'When event occurred',

    -- Event context
    triggered_by VARCHAR(50) NOT NULL COMMENT 'Who triggered: SYSTEM, ADMIN, SCHEDULER',
    triggered_by_user_id VARCHAR(255) COMMENT 'User ID if triggered by admin',

    -- Event data structure
    event_data JSON COMMENT 'Event-specific data: score, rule_name, decision, notes, etc.',

    -- Audit trail
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_assessment FOREIGN KEY (assessment_id) REFERENCES fraud_assessments(id) ON DELETE CASCADE,
    CONSTRAINT chk_event_type CHECK (event_type IN (
        'ASSESSMENT_CREATED', 'RULE_TRIGGERED', 'DECISION_MADE',
        'REVIEW_ASSIGNED', 'REVIEW_ESCALATED', 'REVIEW_COMPLETED',
        'DECISION_OVERRIDDEN', 'APPEAL_SUBMITTED', 'APPEAL_RESOLVED'
    )),

    INDEX idx_assessment_id (assessment_id) COMMENT 'Get audit trail for assessment',
    INDEX idx_event_type (event_type) COMMENT 'Filter by event type',
    INDEX idx_event_timestamp (event_timestamp) COMMENT 'Timeline queries',
    INDEX idx_triggered_by (triggered_by, event_timestamp) COMMENT 'Audit actions by actor'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Fraud assessment audit event log for compliance and forensics';

-- ========================================
-- Enhancements to existing fraud_assessments table
-- ========================================

-- Add columns to track ML model version and processing routing
ALTER TABLE fraud_assessments
ADD COLUMN IF NOT EXISTS ml_model_version VARCHAR(50) COMMENT 'Model version used for this assessment',
ADD COLUMN IF NOT EXISTS processing_lane VARCHAR(20) COMMENT 'Processing routing: FAST_TRACK, MANUAL_REVIEW, ESCALATION, REJECTION',
ADD COLUMN IF NOT EXISTS confidence_score DECIMAL(5, 2) COMMENT 'Overall confidence in assessment (0-100)',
ADD COLUMN IF NOT EXISTS confidence_factors JSON COMMENT 'Breakdown of why we are/not confident',
ADD COLUMN IF NOT EXISTS ml_model_explanation TEXT COMMENT 'Detailed explanation from ML model',
ADD COLUMN IF NOT EXISTS rule_performance_metrics_json JSON COMMENT 'Rule execution performance data',
ADD COLUMN IF NOT EXISTS is_manually_reviewed BOOLEAN DEFAULT FALSE COMMENT 'Was this reviewed by human?',
ADD COLUMN IF NOT EXISTS review_sla_ms BIGINT COMMENT 'SLA for manual review in milliseconds';

-- Add indexes for new columns
ALTER TABLE fraud_assessments
ADD INDEX IF NOT EXISTS idx_ml_model_version (ml_model_version),
ADD INDEX IF NOT EXISTS idx_processing_lane (processing_lane),
ADD INDEX IF NOT EXISTS idx_is_manually_reviewed (is_manually_reviewed);

-- ========================================
-- Sample configuration data
-- ========================================

-- Insert sample ML model record (v1 active)
INSERT IGNORE INTO ml_models (
    model_name, model_version, description, model_type,
    model_path, training_date, training_dataset_name, training_dataset_size,
    accuracy_score, precision_score, recall_score, f1_score, auc_score,
    is_active, deployment_date, deployment_context, created_by
) VALUES (
    'xgboost_paysim',
    '1.0.0',
    'XGBoost model trained on PaySim dataset - Initial production model',
    'XGBOOST',
    '/models/XGBoostModel.pkl',
    NOW(),
    'paysim_v1',
    6000000,
    96.50,
    92.30,
    94.80,
    93.50,
    97.20,
    TRUE,  -- Currently active
    NOW(),
    'PROD',
    'SYSTEM'
);

-- ========================================
-- Verification queries
-- ========================================
-- RUN AFTER MIGRATION:
-- SELECT table_name FROM information_schema.tables WHERE table_schema = 'ctrl_pay' AND table_name IN ('ml_models', 'ml_model_predictions', 'fraud_audit_events');
-- SELECT COUNT(*) as ml_model_count FROM ml_models WHERE is_active = TRUE;
-- DESC fraud_assessments;

