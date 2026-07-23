package ir.aut.logmonitor.evaluator.rules;

import ir.aut.logmonitor.alert.Alert;
import ir.aut.logmonitor.common.model.LogEntry;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class TypeRateRuleTest {

    private RuleDefinition definition(int windowSeconds, int threshold) {
        RuleDefinition def = new RuleDefinition();
        def.setName("warning-rate-rule");
        def.setType(RuleType.TYPE_RATE);
        def.setLogLevel("WARNING");
        def.setWindowSeconds(windowSeconds);
        def.setThreshold(threshold);
        return def;
    }

    private LogEntry warningAt(LocalDateTime timestamp, String message, String component) {
        return new LogEntry(timestamp, "main", "WARNING", "some.Logger", message, component);
    }

    @Test
    void doesNotAlertBelowThreshold() {
        TypeRateRule rule = new TypeRateRule(definition(300, 3));
        LocalDateTime now = LocalDateTime.now();

        assertThat(rule.evaluate(warningAt(now, "msg1", "auth"))).isEmpty();
        assertThat(rule.evaluate(warningAt(now.plusSeconds(1), "msg2", "auth"))).isEmpty();
        assertThat(rule.evaluate(warningAt(now.plusSeconds(2), "msg3", "auth"))).isEmpty();
    }

    @Test
    void alertsWhenThresholdExceededWithinWindow() {
        TypeRateRule rule = new TypeRateRule(definition(300, 3));
        LocalDateTime now = LocalDateTime.now();

        rule.evaluate(warningAt(now, "msg1", "auth"));
        rule.evaluate(warningAt(now.plusSeconds(1), "msg2", "auth"));
        rule.evaluate(warningAt(now.plusSeconds(2), "msg3", "auth"));
        Optional<Alert> result = rule.evaluate(warningAt(now.plusSeconds(3), "msg4", "auth"));

        assertThat(result).isPresent();
        assertThat(result.get().getDescription()).contains("msg3").contains("msg4");
    }

    @Test
    void ignoresLogsOfOtherLevels() {
        TypeRateRule rule = new TypeRateRule(definition(300, 1));
        LocalDateTime now = LocalDateTime.now();

        LogEntry infoEntry = new LogEntry(now, "main", "INFO", "some.Logger", "info msg", "auth");
        Optional<Alert> result = rule.evaluate(infoEntry);

        assertThat(result).isEmpty();
    }

    @Test
    void entriesOutsideWindowAreEvictedAndDoNotCountTowardThreshold() {
        TypeRateRule rule = new TypeRateRule(definition(60, 2));
        LocalDateTime now = LocalDateTime.now();

        // Two entries close together
        rule.evaluate(warningAt(now, "old1", "auth"));
        rule.evaluate(warningAt(now.plusSeconds(1), "old2", "auth"));

        // A new entry, well beyond the 60s window relative to the first two
        Optional<Alert> result = rule.evaluate(warningAt(now.plusSeconds(200), "new1", "auth"));

        // The two old entries should have been evicted, leaving only 1 -> below threshold(2)
        assertThat(result).isEmpty();
    }

    @Test
    void tracksDifferentComponentsIndependently() {
        TypeRateRule rule = new TypeRateRule(definition(300, 1));
        LocalDateTime now = LocalDateTime.now();

        rule.evaluate(warningAt(now, "msg-a1", "service-a"));
        Optional<Alert> resultB = rule.evaluate(warningAt(now, "msg-b1", "service-b"));

        // service-b's window only has 1 entry so far (its own), threshold is 1 -> not exceeded
        assertThat(resultB).isEmpty();
    }
}