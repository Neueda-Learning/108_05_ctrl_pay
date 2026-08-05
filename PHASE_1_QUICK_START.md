# Phase 1 Quick Start Guide for Developers

**For:** Backend developers integrating Phase 1 components  
**Duration:** 2-3 hours to complete all steps  
**Difficulty:** Intermediate

---

## ✅ Pre-Integration Checklist

Before starting, ensure you have:
- [ ] Java 17+ installed
- [ ] MySQL 8.0+ running locally
- [ ] Spring Boot 4.0+ project open
- [ ] Access to all files created in Phase 1
- [ ] 2-3 hours of uninterrupted development time

---

## Step 1: Database Setup (15 minutes)

### 1.1 Execute Migration
```bash
cd backend
mysql -u root -p ctrl_pay < src/main/resources/db/migration/V2.0__Phase1_ML_Model_Management.sql
```

### 1.2 Verify Tables Created
```sql
USE ctrl_pay;
SHOW TABLES LIKE 'ml_%';           -- Should show: ml_models, ml_model_predictions
SHOW COLUMNS FROM fraud_assessments;  -- Should show 8 new columns
SELECT * FROM ml_models;           -- Should show 1 initial record
```

### 1.3 Check Initial Data
```sql
SELECT model_name, model_version, is_active, deployment_date FROM ml_models;
-- Expected: xgboost_paysim | 1.0.0 | 1 (TRUE) | 2026-08-05...
```

---

## Step 2: Add Java Classes (30 minutes)

### 2.1 Copy Domain Models
Copy these 4 files to `backend/src/main/java/com/neueda/domain/`:
```
✓ MLModelRecord.java (197 lines)
✓ MLModelPredictionRecord.java (126 lines)
✓ ProcessingLane.java (52 lines)
✓ FraudAuditEventRecord.java (71 lines)
```

**Verify:** No compilation errors in IDE

### 2.2 Copy Repository Interfaces
Copy these 3 files to `backend/src/main/java/com/neueda/repository/`:
```
✓ MLModelRepository.java (34 lines)
✓ MLModelPredictionRepository.java (64 lines)
✓ FraudAuditEventRepository.java (39 lines)
```

### 2.3 Copy Fraud Detection Services
Copy these 3 files to `backend/src/main/java/com/neueda/fraud/`:
```
✓ MLModelRegistry.java (167 lines)
✓ MLModelService.java (172 lines)
✓ EnhancedFraudDecisionEngine.java (243 lines)
```

### 2.4 Copy Fraud Rules
Copy these 5 files to `backend/src/main/java/com/neueda/fraud/rules/`:
```
✓ UnusualTimePatternRule.java (85 lines)
✓ VelocityAnomalyRule.java (98 lines)
✓ CyclicalTransactionPatternRule.java (95 lines)
✓ BehavioralBaselineRule.java (105 lines)
✓ ContextualRiskAggregationRule.java (137 lines)
```

### 2.5 Update FraudRuleEngine
Edit `backend/src/main/java/com/neueda/fraud/rules/FraudRuleEngine.java`:
- Update constructor to include 5 new rule parameters
- Add 5 new rules to the rules List
- (See PHASE_1_REFERENCE_GUIDE.md for exact changes)

---

## Step 3: Implement Repository Classes (45 minutes)

### 3.1 Create JdbcMLModelRepository

Create file: `backend/src/main/java/com/neueda/repository/JdbcMLModelRepository.java`

```java
package com.neueda.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import com.neueda.domain.MLModelRecord;

@Repository
public class JdbcMLModelRepository implements MLModelRepository {
    private final JdbcTemplate jdbcTemplate;
    
    public JdbcMLModelRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    @Override
    public MLModelRecord save(MLModelRecord model) {
        String sql = """
            INSERT INTO ml_models (
                model_name, model_version, description, model_type,
                model_path, training_date, training_dataset_name,
                training_dataset_size, accuracy_score, precision_score,
                recall_score, f1_score, auc_score, is_active,
                deployment_date, retirement_date, deployment_context,
                created_by, created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        jdbcTemplate.update(sql,
            model.modelName(),
            model.modelVersion(),
            model.description(),
            model.modelType(),
            model.modelPath(),
            model.trainingDate(),
            model.trainingDatasetName(),
            model.trainingDatasetSize(),
            model.accuracyScore(),
            model.precisionScore(),
            model.recallScore(),
            model.f1Score(),
            model.aucScore(),
            model.isActive(),
            model.deploymentDate(),
            model.retirementDate(),
            model.deploymentContext(),
            model.createdBy(),
            model.createdAt(),
            model.updatedAt()
        );
        
        // Return with generated ID
        Optional<MLModelRecord> saved = findByNameAndVersion(
            model.modelName(),
            model.modelVersion()
        );
        return saved.orElse(model);
    }
    
    @Override
    public MLModelRecord update(MLModelRecord model) {
        String sql = """
            UPDATE ml_models SET
                description = ?, model_path = ?, is_active = ?,
                deployment_date = ?, retirement_date = ?,
                updated_at = NOW()
            WHERE id = ?
            """;
        
        jdbcTemplate.update(sql,
            model.description(),
            model.modelPath(),
            model.isActive(),
            model.deploymentDate(),
            model.retirementDate(),
            model.id()
        );
        
        return model;
    }
    
    @Override
    public Optional<MLModelRecord> findById(Long id) {
        String sql = "SELECT * FROM ml_models WHERE id = ?";
        List<MLModelRecord> results = jdbcTemplate.query(sql, mlModelRowMapper(), id);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }
    
    @Override
    public Optional<MLModelRecord> findActiveModel() {
        String sql = """
            SELECT * FROM ml_models 
            WHERE is_active = TRUE 
            ORDER BY deployment_date DESC LIMIT 1
            """;
        List<MLModelRecord> results = jdbcTemplate.query(sql, mlModelRowMapper());
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }
    
    @Override
    public List<MLModelRecord> findAllActiveModels() {
        String sql = """
            SELECT * FROM ml_models 
            WHERE is_active = TRUE 
            ORDER BY deployment_date DESC
            """;
        return jdbcTemplate.query(sql, mlModelRowMapper());
    }
    
    @Override
    public List<MLModelRecord> findByModelName(String modelName) {
        String sql = "SELECT * FROM ml_models WHERE model_name = ? ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, mlModelRowMapper(), modelName);
    }
    
    @Override
    public Optional<MLModelRecord> findByNameAndVersion(String modelName, String version) {
        String sql = "SELECT * FROM ml_models WHERE model_name = ? AND model_version = ?";
        List<MLModelRecord> results = jdbcTemplate.query(sql, mlModelRowMapper(), modelName, version);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }
    
    @Override
    public List<MLModelRecord> findByDeploymentContext(String context) {
        String sql = "SELECT * FROM ml_models WHERE deployment_context = ? ORDER BY created_at DESC";
        return jdbcTemplate.query(sql, mlModelRowMapper(), context);
    }
    
    @Override
    public List<MLModelRecord> findAllDeployedModels() {
        String sql = "SELECT * FROM ml_models WHERE is_active = TRUE OR deployment_date IS NOT NULL ORDER BY deployment_date DESC";
        return jdbcTemplate.query(sql, mlModelRowMapper());
    }
    
    private RowMapper<MLModelRecord> mlModelRowMapper() {
        return (rs, rowNum) -> new MLModelRecord(
            rs.getLong("id"),
            rs.getString("model_name"),
            rs.getString("model_version"),
            rs.getString("description"),
            rs.getString("model_type"),
            rs.getString("model_path"),
            rs.getTimestamp("training_date").toLocalDateTime(),
            rs.getString("training_dataset_name"),
            rs.getObject("training_dataset_size", Integer.class),
            rs.getObject("accuracy_score", java.math.BigDecimal.class),
            rs.getObject("precision_score", java.math.BigDecimal.class),
            rs.getObject("recall_score", java.math.BigDecimal.class),
            rs.getObject("f1_score", java.math.BigDecimal.class),
            rs.getObject("auc_score", java.math.BigDecimal.class),
            rs.getObject("false_positive_rate", java.math.BigDecimal.class),
            rs.getObject("false_negative_rate", java.math.BigDecimal.class),
            rs.getBoolean("is_active"),
            rs.getTimestamp("deployment_date") != null ? rs.getTimestamp("deployment_date").toLocalDateTime() : null,
            rs.getTimestamp("retirement_date") != null ? rs.getTimestamp("retirement_date").toLocalDateTime() : null,
            rs.getString("deployment_context"),
            rs.getString("feature_columns_json"),
            rs.getString("hyperparameters_json"),
            rs.getString("created_by"),
            rs.getTimestamp("created_at").toLocalDateTime(),
            rs.getTimestamp("updated_at").toLocalDateTime()
        );
    }
}
```

### 3.2 Create JdbcMLModelPredictionRepository

Create file: `backend/src/main/java/com/neueda/repository/JdbcMLModelPredictionRepository.java`

```java
package com.neueda.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import com.neueda.domain.MLModelPredictionRecord;

@Repository
public class JdbcMLModelPredictionRepository implements MLModelPredictionRepository {
    private final JdbcTemplate jdbcTemplate;
    
    public JdbcMLModelPredictionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    @Override
    public MLModelPredictionRecord save(MLModelPredictionRecord prediction) {
        String sql = """
            INSERT INTO ml_model_predictions (
                ml_model_id, payment_id, assessment_id,
                predicted_fraud_probability, prediction_confidence,
                prediction_latency_ms, created_at
            ) VALUES (?, ?, ?, ?, ?, ?, NOW())
            """;
        
        jdbcTemplate.update(sql,
            prediction.mlModelId(),
            prediction.paymentId(),
            prediction.assessmentId(),
            prediction.predictedFraudProbability(),
            prediction.predictionConfidence(),
            prediction.predictionLatencyMs()
        );
        
        // TODO: Return with generated ID
        return prediction;
    }
    
    @Override
    public MLModelPredictionRecord update(MLModelPredictionRecord prediction) {
        String sql = """
            UPDATE ml_model_predictions SET
                ground_truth_fraud = ?, ground_truth_source = ?,
                ground_truth_date = ?, is_correct_prediction = ?,
                prediction_type = ?
            WHERE id = ?
            """;
        
        jdbcTemplate.update(sql,
            prediction.groundTruthFraud(),
            prediction.groundTruthSource(),
            prediction.groundTruthDate(),
            prediction.isCorrectPrediction(),
            prediction.predictionType(),
            prediction.id()
        );
        
        return prediction;
    }
    
    @Override
    public Optional<MLModelPredictionRecord> findById(Long id) {
        String sql = "SELECT * FROM ml_model_predictions WHERE id = ?";
        List<MLModelPredictionRecord> results = jdbcTemplate.query(sql, predictionRowMapper(), id);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }
    
    @Override
    public List<MLModelPredictionRecord> findByPaymentId(Long paymentId) {
        String sql = "SELECT * FROM ml_model_predictions WHERE payment_id = ?";
        return jdbcTemplate.query(sql, predictionRowMapper(), paymentId);
    }
    
    @Override
    public List<MLModelPredictionRecord> findByMLModelId(Long mlModelId) {
        String sql = "SELECT * FROM ml_model_predictions WHERE ml_model_id = ?";
        return jdbcTemplate.query(sql, predictionRowMapper(), mlModelId);
    }
    
    @Override
    public List<MLModelPredictionRecord> findByAssessmentId(Long assessmentId) {
        String sql = "SELECT * FROM ml_model_predictions WHERE assessment_id = ?";
        return jdbcTemplate.query(sql, predictionRowMapper(), assessmentId);
    }
    
    @Override
    public PredictionAccuracyMetrics getAccuracyMetrics(Long mlModelId) {
        String sql = """
            SELECT
                COUNT(*) as total,
                SUM(CASE WHEN is_correct_prediction = 1 THEN 1 ELSE 0 END) as correct,
                SUM(CASE WHEN prediction_type = 'FALSE_POSITIVE' THEN 1 ELSE 0 END) as fp,
                SUM(CASE WHEN prediction_type = 'FALSE_NEGATIVE' THEN 1 ELSE 0 END) as fn,
                SUM(CASE WHEN prediction_type = 'TRUE_POSITIVE' THEN 1 ELSE 0 END) as tp,
                SUM(CASE WHEN prediction_type = 'TRUE_NEGATIVE' THEN 1 ELSE 0 END) as tn
            FROM ml_model_predictions
            WHERE ml_model_id = ? AND is_correct_prediction IS NOT NULL
            """;
        
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
            long total = rs.getLong("total");
            long correct = rs.getLong("correct");
            long fp = rs.getLong("fp");
            long fn = rs.getLong("fn");
            long tp = rs.getLong("tp");
            long tn = rs.getLong("tn");
            
            double accuracy = total > 0 ? (double) correct / total : 0;
            double precision = (tp + fp) > 0 ? (double) tp / (tp + fp) : 0;
            double recall = (tp + fn) > 0 ? (double) tp / (tp + fn) : 0;
            double f1 = (precision + recall) > 0 ? 2 * (precision * recall) / (precision + recall) : 0;
            
            return new PredictionAccuracyMetrics(total, correct, fp, fn, tp, tn, accuracy, precision, recall, f1);
        }, mlModelId);
    }
    
    @Override
    public Double getPrecision(Long mlModelId) {
        String sql = """
            SELECT
                SUM(CASE WHEN prediction_type IN ('TRUE_POSITIVE') THEN 1 ELSE 0 END) as tp,
                SUM(CASE WHEN prediction_type IN ('FALSE_POSITIVE') THEN 1 ELSE 0 END) as fp
            FROM ml_model_predictions
            WHERE ml_model_id = ? AND is_correct_prediction IS NOT NULL
            """;
        
        Double result = jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
            long tp = rs.getLong("tp");
            long fp = rs.getLong("fp");
            return (tp + fp) > 0 ? (double) tp / (tp + fp) : 0;
        }, mlModelId);
        
        return result != null ? result : 0.0;
    }
    
    @Override
    public Double getRecall(Long mlModelId) {
        String sql = """
            SELECT
                SUM(CASE WHEN prediction_type IN ('TRUE_POSITIVE') THEN 1 ELSE 0 END) as tp,
                SUM(CASE WHEN prediction_type IN ('FALSE_NEGATIVE') THEN 1 ELSE 0 END) as fn
            FROM ml_model_predictions
            WHERE ml_model_id = ? AND is_correct_prediction IS NOT NULL
            """;
        
        Double result = jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
            long tp = rs.getLong("tp");
            long fn = rs.getLong("fn");
            return (tp + fn) > 0 ? (double) tp / (tp + fn) : 0;
        }, mlModelId);
        
        return result != null ? result : 0.0;
    }
    
    @Override
    public long countByModelId(Long mlModelId) {
        String sql = "SELECT COUNT(*) FROM ml_model_predictions WHERE ml_model_id = ?";
        Long result = jdbcTemplate.queryForObject(sql, Long.class, mlModelId);
        return result != null ? result : 0;
    }
    
    @Override
    public long countCorrectPredictions(Long mlModelId) {
        String sql = "SELECT COUNT(*) FROM ml_model_predictions WHERE ml_model_id = ? AND is_correct_prediction = 1";
        Long result = jdbcTemplate.queryForObject(sql, Long.class, mlModelId);
        return result != null ? result : 0;
    }
    
    private RowMapper<MLModelPredictionRecord> predictionRowMapper() {
        return (rs, rowNum) -> new MLModelPredictionRecord(
            rs.getLong("id"),
            rs.getLong("ml_model_id"),
            rs.getLong("payment_id"),
            rs.getObject("assessment_id", Long.class),
            rs.getObject("predicted_fraud_probability", java.math.BigDecimal.class),
            rs.getObject("prediction_confidence", java.math.BigDecimal.class),
            rs.getObject("prediction_latency_ms", Integer.class),
            rs.getObject("ground_truth_fraud", Boolean.class),
            rs.getString("ground_truth_source"),
            rs.getTimestamp("ground_truth_date") != null ? rs.getTimestamp("ground_truth_date").toLocalDateTime() : null,
            rs.getObject("is_correct_prediction", Boolean.class),
            rs.getString("prediction_type"),
            rs.getTimestamp("created_at").toLocalDateTime()
        );
    }
}
```

### 3.3 Create JdbcFraudAuditEventRepository

Create file: `backend/src/main/java/com/neueda/repository/JdbcFraudAuditEventRepository.java`

```java
package com.neueda.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import com.neueda.domain.FraudAuditEventRecord;

@Repository
public class JdbcFraudAuditEventRepository implements FraudAuditEventRepository {
    private final JdbcTemplate jdbcTemplate;
    
    public JdbcFraudAuditEventRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }
    
    @Override
    public FraudAuditEventRecord save(FraudAuditEventRecord event) {
        String sql = """
            INSERT INTO fraud_audit_events (
                assessment_id, event_type, event_timestamp,
                triggered_by, triggered_by_user_id, event_data, created_at
            ) VALUES (?, ?, ?, ?, ?, ?, NOW())
            """;
        
        jdbcTemplate.update(sql,
            event.assessmentId(),
            event.eventType(),
            event.eventTimestamp(),
            event.triggeredBy(),
            event.triggeredByUserId(),
            event.eventDataJson()
        );
        
        return event;
    }
    
    @Override
    public Optional<FraudAuditEventRecord> findById(Long id) {
        String sql = "SELECT * FROM fraud_audit_events WHERE id = ?";
        List<FraudAuditEventRecord> results = jdbcTemplate.query(sql, auditEventRowMapper(), id);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }
    
    @Override
    public List<FraudAuditEventRecord> findByAssessmentId(Long assessmentId) {
        String sql = "SELECT * FROM fraud_audit_events WHERE assessment_id = ? ORDER BY event_timestamp ASC";
        return jdbcTemplate.query(sql, auditEventRowMapper(), assessmentId);
    }
    
    @Override
    public List<FraudAuditEventRecord> findByEventType(String eventType) {
        String sql = "SELECT * FROM fraud_audit_events WHERE event_type = ? ORDER BY event_timestamp DESC";
        return jdbcTemplate.query(sql, auditEventRowMapper(), eventType);
    }
    
    @Override
    public List<FraudAuditEventRecord> findByTriggeredByUser(String userId) {
        String sql = "SELECT * FROM fraud_audit_events WHERE triggered_by_user_id = ? ORDER BY event_timestamp DESC";
        return jdbcTemplate.query(sql, auditEventRowMapper(), userId);
    }
    
    @Override
    public long countByEventType(String eventType) {
        String sql = "SELECT COUNT(*) FROM fraud_audit_events WHERE event_type = ?";
        Long result = jdbcTemplate.queryForObject(sql, Long.class, eventType);
        return result != null ? result : 0;
    }
    
    private RowMapper<FraudAuditEventRecord> auditEventRowMapper() {
        return (rs, rowNum) -> new FraudAuditEventRecord(
            rs.getLong("id"),
            rs.getLong("assessment_id"),
            rs.getString("event_type"),
            rs.getTimestamp("event_timestamp").toLocalDateTime(),
            rs.getString("triggered_by"),
            rs.getString("triggered_by_user_id"),
            rs.getString("event_data"),
            rs.getTimestamp("created_at").toLocalDateTime()
        );
    }
}
```

---

## Step 4: Add Configuration (10 minutes)

Copy `frauddetection.properties` to:
```
backend/src/main/resources/frauddetection.properties
```

Add to `application.properties`:
```properties
# Include fraud detection properties
spring.config.import=classpath:frauddetection.properties
```

---

## Step 5: Verify Integration (20 minutes)

### 5.1 Build Project
```bash
cd backend
./mvnw clean compile
```

**Expected:** No compilation errors

### 5.2 Run Application
```bash
./mvnw spring-boot:run
```

**Expected Output (first 100ms):**
```
✅ ML Model Registry initialized with model: xgboost_paysim v1.0.0
✅ Components registered:
  - MLModelRegistry (bean name: mlModelRegistry)
  - MLModelService (bean name: mlModelService)
  - EnhancedFraudDecisionEngine (bean name: enhancedFraudDecisionEngine)
  - JdbcMLModelRepository (bean name: jdbcMLModelRepository)
  - 5 Fraud Rules registered in FraudRuleEngine
```

### 5.3 Test Endpoint
```bash
curl -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "idempotencyKey": "test-1",
    "sourceAccount": "123456789012",
    "destinationAccount": "210987654321",
    "amount": 1000.00,
    "currency": "USD"
  }'
```

**Expected Response:** Payment with fraud assessment in response body

### 5.4 Verify Database
```sql
SELECT id, payment_id, hybrid_fraud_score, decision, processing_lane FROM fraud_assessments LIMIT 1;
-- Should return 1 row with assessment data
```

---

## Step 6: Run Tests (15 minutes)

### 6.1 Unit Tests
```bash
./mvnw test -Dtest=*MLModelRegistry* -q
./mvnw test -Dtest=*EnhancedFraudDecisionEngine* -q
./mvnw test -Dtest=*UnusualTimePattern* -q
```

### 6.2 Integration Tests
```bash
./mvnw test -Dtest=*FraudDetectionIT* -q
```

---

## Troubleshooting

### Issue: "MLModelRegistry bean not found"
**Solution:** Ensure `@ComponentScan` includes `com.neueda.fraud`

### Issue: "No active ML model found"
**Solution:** Check that initial record was inserted into `ml_models` table

### Issue: "Database migration failed"
**Solution:** Run `DESC fraud_assessments` to verify columns were added

---

## Success Criteria

Once complete, verify:
- [ ] Application starts without errors
- [ ] ML model registry logs initialization
- [ ] Test payment creates fraud assessment
- [ ] Database contains assessment record
- [ ] All 15 fraud rules execute
- [ ] Processing lane assigned correctly
- [ ] Tests pass 90%+

---

## Next Steps After Integration

1. Run full QA test suite
2. Performance testing (latency, load)
3. Deploy to staging
4. Validate with stakeholders
5. Prepare for production deployment

---

**Estimated Total Time: 2-3 hours for complete integration**

If you encounter issues, refer to:
- PHASE_1_REFERENCE_GUIDE.md - Component integration details
- Components' class-level JavaDoc - API documentation
- PHASE_1_IMPLEMENTATION_SUMMARY.md - Architecture overview


