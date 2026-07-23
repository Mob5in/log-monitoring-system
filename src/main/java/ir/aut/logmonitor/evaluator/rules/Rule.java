package ir.aut.logmonitor.evaluator.rules;

import ir.aut.logmonitor.alert.Alert;
import ir.aut.logmonitor.common.model.LogEntry;

import java.util.Optional;

/**
 * A single monitoring rule. Implementations are given each incoming
 * {@link LogEntry} one at a time and decide whether it (combined with
 * whatever history the rule itself tracks) should produce an {@link Alert}.
 *
 * Implementations are plain classes (not Spring beans) so they can be
 * unit tested in isolation, and so multiple rule instances (one per
 * RuleDefinition loaded from rules.yml) can coexist independently.
 */
public interface Rule {

    /**
     * Evaluates a single log entry against this rule's condition.
     *
     * @param entry the incoming log entry
     * @return an Alert if the rule's condition was triggered, otherwise empty
     */
    Optional<Alert> evaluate(LogEntry entry);
}