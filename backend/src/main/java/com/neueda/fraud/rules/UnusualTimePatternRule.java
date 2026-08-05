package com.neueda.fraud.rules;

import java.time.DayOfWeek;

import org.springframework.stereotype.Component;

import com.neueda.domain.AccountRecord;
import com.neueda.domain.PaymentRecord;

/**
 * Unusual Time Pattern Rule
 * Detects transactions occurring at unusual times (e.g., 3 AM, weekends)
 */
@Component
public class UnusualTimePatternRule implements FraudRule {

    private static final String RULE_NAME = "UNUSUAL_TIME_PATTERN";
    private static final String DESCRIPTION = "Detects payments at unusual times (nights, weekends)";
    private static final double WEIGHT = 0.05;

    @Override
    public String getRuleName() { return RULE_NAME; }

    @Override
    public String getDescription() { return DESCRIPTION; }

    @Override
    public double getWeight() { return WEIGHT; }

    @Override
    public FraudRuleResult evaluate(
        PaymentRecord payment,
        AccountRecord sourceAccount,
        AccountRecord destinationAccount
    ) {
        int hour = payment.createdAt().getHour();
        DayOfWeek day = payment.createdAt().getDayOfWeek();

        boolean isWeekend = day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
        boolean isOffHours = hour < 6 || hour >= 22;

        if (isOffHours && isWeekend) {
            return FraudRuleResult.triggered(RULE_NAME, 30,
                String.format("Payment at %d:00 on %s — off-hours weekend transaction", hour, day));
        } else if (isOffHours) {
            return FraudRuleResult.triggered(RULE_NAME, 15,
                String.format("Payment at %d:00 — off-hours on %s", hour, day));
        } else if (isWeekend) {
            return FraudRuleResult.triggered(RULE_NAME, 10,
                String.format("Payment on %s during business hours", day));
        }
        return FraudRuleResult.notTriggered(RULE_NAME, "Payment during normal business hours");
    }
}
