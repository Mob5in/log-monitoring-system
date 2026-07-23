package ir.aut.logmonitor.evaluator.rules;

import ir.aut.logmonitor.alert.Alert;
import ir.aut.logmonitor.common.model.LogEntry;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class LogTypeRuleTest {

    private RuleDefinition definitionFor(String logLevel) {
        RuleDefinition def = new RuleDefinition();
        def.setName("error-log-rule");
        def.setType(RuleType.LOG_TYPE);
        def.setLogLevel(logLevel);
        return def;
    }

    private LogEntry logEntry(String level, String message, String component) {
        return new LogEntry(LocalDateTime.now(), "main", level, "some.Logger", message, component);
    }

    @Test
    void generatesAlertWhenLevelMatches() {
        LogTypeRule rule = new LogTypeRule(definitionFor("ERROR"));

        Optional<Alert> result = rule.evaluate(logEntry("ERROR", "Something broke", "auth-service"));

        assertThat(result).isPresent();
        assertThat(result.get().getRuleName()).isEqualTo("error-log-rule");
        assertThat(result.get().getComponentName()).isEqualTo("auth-service");
        assertThat(result.get().getDescription()).isEqualTo("Something broke");
    }

    @Test
    void doesNotGenerateAlertWhenLevelDoesNotMatch() {
        LogTypeRule rule = new LogTypeRule(definitionFor("ERROR"));

        Optional<Alert> result = rule.evaluate(logEntry("INFO", "All good", "auth-service"));

        assertThat(result).isEmpty();
    }

    @Test
    void matchIsCaseInsensitive() {
        LogTypeRule rule = new LogTypeRule(definitionFor("error"));

        Optional<Alert> result = rule.evaluate(logEntry("ERROR", "Something broke", "auth-service"));

        assertThat(result).isPresent();
    }
}