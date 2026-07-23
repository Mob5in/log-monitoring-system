package ir.aut.logmonitor.evaluator.rules;

import ir.aut.logmonitor.alert.Alert;
import ir.aut.logmonitor.common.model.LogEntry;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Alerts on every single log entry whose level matches the configured
 * {@code logLevel} (e.g. every ERROR log generates an alert).
 *
 * Stateless — doesn't need to remember anything between calls.
 */
public class LogTypeRule implements Rule {

    private final RuleDefinition definition;

    public LogTypeRule(RuleDefinition definition) {
        this.definition = definition;
    }

    @Override
    public Optional<Alert> evaluate(LogEntry entry) {
        if (entry.getLevel() == null || !entry.getLevel().equalsIgnoreCase(definition.getLogLevel())) {
            return Optional.empty();
        }

        Alert alert = new Alert(
                definition.getName(),
                entry.getComponentName(),
                entry.getMessage(),
                LocalDateTime.now()
        );
        return Optional.of(alert);
    }
}