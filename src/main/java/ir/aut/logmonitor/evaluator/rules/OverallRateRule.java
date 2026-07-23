package ir.aut.logmonitor.evaluator.rules;

import ir.aut.logmonitor.alert.Alert;
import ir.aut.logmonitor.common.model.LogEntry;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Alerts if a component's overall log rate (any level) exceeds
 * {@code threshold} logs within a {@code windowSeconds}-second sliding
 * window. Unlike {@link TypeRateRule}, this doesn't filter by log level —
 * it tracks total log volume per component.
 */
public class OverallRateRule implements Rule {

    private final RuleDefinition definition;

    // One sliding window (timestamps only, oldest first) per component.
    private final ConcurrentHashMap<String, Deque<LocalDateTime>> windowsByComponent = new ConcurrentHashMap<>();

    public OverallRateRule(RuleDefinition definition) {
        this.definition = definition;
    }

    @Override
    public Optional<Alert> evaluate(LogEntry entry) {
        Deque<LocalDateTime> window = windowsByComponent.computeIfAbsent(
                entry.getComponentName(), name -> new ArrayDeque<>());

        synchronized (window) {
            window.addLast(entry.getTimestamp());
            evictEntriesOutsideWindow(window, entry.getTimestamp());

            if (window.size() > definition.getThreshold()) {
                double ratePerSecond = window.size() / (double) definition.getWindowSeconds();
                String description = String.format(
                        "Component '%s' overall log rate is %.2f logs/sec (%d logs in the last %d seconds, threshold: %d)",
                        entry.getComponentName(), ratePerSecond, window.size(),
                        definition.getWindowSeconds(), definition.getThreshold()
                );
                Alert alert = new Alert(definition.getName(), entry.getComponentName(), description, LocalDateTime.now());
                return Optional.of(alert);
            }
        }

        return Optional.empty();
    }

    private void evictEntriesOutsideWindow(Deque<LocalDateTime> window, LocalDateTime referenceTime) {
        LocalDateTime cutoff = referenceTime.minusSeconds(definition.getWindowSeconds());
        while (!window.isEmpty() && window.peekFirst().isBefore(cutoff)) {
            window.pollFirst();
        }
    }
}

