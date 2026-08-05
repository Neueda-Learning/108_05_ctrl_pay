package com.neueda.fraud;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neueda.domain.AccountRecord;
import com.neueda.domain.FraudAssessmentRecord;
import com.neueda.domain.FraudRiskLevel;
import com.neueda.domain.PaymentRecord;
import com.neueda.fraud.FraudDecisionEngine.FraudDecisionResult;
import com.neueda.fraud.rules.FraudRuleEngine;
import com.neueda.fraud.rules.FraudRuleEngine.FraudDetectionResult;
import com.neueda.repository.AccountRepository;
import com.neueda.repository.FraudAssessmentRepository;
import com.neueda.service.FraudRiskService;

/**
 * Fraud Detection Service — orchestrates rule engine + ML model to produce a fraud assessment.
 */
@Service
@Transactional
public class FraudDetectionService {

    private static final Logger logger = LoggerFactory.getLogger(FraudDetectionService.class);

    @Value("${fraud.detection.enabled:true}")
    private boolean fraudDetectionEnabled;

    private final FraudRuleEngine ruleEngine;
    private final FraudDecisionEngine decisionEngine;
    private final FraudAssessmentRepository assessmentRepository;
    private final AccountRepository accountRepository;
    private final ObjectMapper objectMapper;
    private final FraudRiskService fraudRiskService;

    public FraudDetectionService(
        FraudRuleEngine ruleEngine,
        FraudDecisionEngine decisionEngine,
        FraudAssessmentRepository assessmentRepository,
        AccountRepository accountRepository,
        ObjectMapper objectMapper,
        FraudRiskService fraudRiskService
    ) {
        this.ruleEngine = ruleEngine;
        this.decisionEngine = decisionEngine;
        this.assessmentRepository = assessmentRepository;
        this.accountRepository = accountRepository;
        this.objectMapper = objectMapper;
        this.fraudRiskService = fraudRiskService;
    }

    /**
     * Perform complete fraud detection for a payment.
     * Executes rule engine, gets ML probability, combines results via decision engine.
     */
    public FraudAssessmentRecord assessPayment(PaymentRecord payment) {
        if (!fraudDetectionEnabled) {
            return FraudAssessmentRecord.create(
                payment.id(),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "[]",
                "{}",
                com.neueda.domain.FraudDecision.APPROVED,
                FraudRiskLevel.LOW,
                "Fraud detection disabled"
            );
        }

        // Idempotency: return existing assessment if already assessed
        Optional<FraudAssessmentRecord> existing = assessmentRepository.findByPaymentId(payment.id());
        if (existing.isPresent()) {
            return existing.get();
        }

        // Load accounts
        AccountRecord sourceAccount = accountRepository.findByAccountNumber(payment.sourceAccount())
            .orElseThrow(() -> new IllegalArgumentException("Source account not found: " + payment.sourceAccount()));
        AccountRecord destinationAccount = accountRepository.findByAccountNumber(payment.destinationAccount())
            .orElseThrow(() -> new IllegalArgumentException("Destination account not found: " + payment.destinationAccount()));

        // Execute rule engine (all registered fraud rules)
        FraudDetectionResult ruleResult = ruleEngine.evaluatePayment(payment, sourceAccount, destinationAccount);

        // Get ML fraud probability (0–100 scale, 0 if unavailable)
        BigDecimal mlProbability = getMlFraudProbability(payment);

        // Combine scores into final decision
        FraudDecisionResult decisionResult = decisionEngine.makeDecision(ruleResult.ruleEngineScore(), mlProbability);

        // Build serializable rule scores
        Map<String, Object> ruleScoresMap = new HashMap<>();
        ruleResult.ruleScoresBreakdown().forEach((ruleName, detail) -> {
            Map<String, Object> ruleDetail = new HashMap<>();
            ruleDetail.put("name", detail.ruleName());
            ruleDetail.put("score", detail.score());
            ruleDetail.put("triggered", detail.triggered());
            ruleDetail.put("explanation", detail.explanation());
            ruleDetail.put("weight", detail.weight());
            ruleScoresMap.put(ruleName, ruleDetail);
        });

        String ruleScoresJson = serializeToJson(ruleScoresMap);
        String triggeredRulesJson = serializeToJson(ruleResult.triggeredRuleNames());

        FraudAssessmentRecord assessment = FraudAssessmentRecord.create(
            payment.id(),
            decisionResult.hybridScore(),
            decisionResult.ruleEngineScore(),
            decisionResult.mlScore(),
            triggeredRulesJson,
            ruleScoresJson,
            decisionResult.decision(),
            decisionResult.riskLevel(),
            decisionResult.explanation()
        );

        FraudAssessmentRecord saved = assessmentRepository.save(assessment);
        logger.info("Fraud assessment for payment {}: decision={} score={} riskLevel={}",
            payment.id(), decisionResult.decision(), decisionResult.hybridScore(), decisionResult.riskLevel());
        return saved;
    }

    /**
     * Get ML fraud probability for a payment (0–100 scale).
     * Returns ZERO if ML service is unavailable — rule-based scoring still applies.
     */
    private BigDecimal getMlFraudProbability(PaymentRecord payment) {
        try {
            FraudRiskService.PaymentRisk risk = fraudRiskService.assessPaymentRiskForCreation(payment);
            if (risk.fraudProbability() != null) {
                // fraudProbability from FraudRiskService is already on 0-100 scale
                return BigDecimal.valueOf(risk.fraudProbability());
            }
        } catch (Exception e) {
            logger.warn("ML service unavailable for payment {}, using rules-only scoring: {}", payment.id(), e.getMessage());
        }
        return BigDecimal.ZERO;
    }

    private String serializeToJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return "{}";
        }
    }

    public boolean shouldAssess(PaymentRecord payment) {
        return payment != null && payment.id() != null;
    }
}
