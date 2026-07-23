package ir.aut.logmonitor.evaluator.rules;

import ir.aut.logmonitor.alert.Alert;
import ir.aut.logmonitor.common.model.LogEntry;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class OverallRateRuleTest {

    private RuleDefinition definition(int windowSeconds, int threshold) {
        RuleDefinition def = new RuleDefinition();
        def.setName("overall-rate-rule");
        def.setType(RuleType.OVERALL_RATE);
        def.setWindowSeconds(windowSeconds);
        def.setThreshold(threshold);
        return def;
    }

    private LogEntry logAt(LocalDateTime timestamp, String level) {
        return new LogEntry(timestamp, "main", level, "some.Logger", "msg", "auth");
    }

    @Test
    void countsLogsRegardlessOfLevel() {
        OverallRateRule rule = new OverallRateRule(definition(300, 2));
        LocalDateTime now = LocalDateTime.now();

        rule.evaluate(logAt(now, "INFO"));
        rule.evaluate(logAt(now.plusSeconds(1), "ERROR"));
        Optional<Alert> result = rule.evaluate(logAt(now.plusSeconds(2), "WARNING"));

        assertThat(result).isPresent();
        assertThat(result.get().getDescription()).contains("auth");
    }

    @Test
    void doesNotAlertBelowThreshold() {
        OverallRateRule rule = new OverallRateRule(definition(300, 5));
        LocalDateTime now = LocalDateTime.now();

        Optional<Alert> result = rule.evaluate(logAt(now, "INFO"));

        assertThat(result).isEmpty();
    }

    @Test
    void entriesOutsideWindowAreEvicted() {
        OverallRateRule rule = new OverallRateRule(definition(60, 1));
        LocalDateTime now = LocalDateTime.now();

        rule.evaluate(logAt(now, "INFO"));
        Optional<Alert> result = rule.evaluate(logAt(now.plusSeconds(200), "INFO"));

        // The first entry should have been evicted, leaving only 1 -> not above threshold(1)
        assertThat(result).isEmpty();
    }
}