package ir.aut.logmonitor.evaluator.rules;

import ir.aut.logmonitor.alert.Alert;
import ir.aut.logmonitor.common.model.LogEntry;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Alerts if a component produces more than {@code threshold} logs of a
 * specific {@code logLevel} within a {@code windowSeconds}-second sliding
 * window.
 *
 * Maintains an in-memory sliding window of recent matching log entries per
 * component. Not persisted — resets if the application restarts, which is
 * acceptable for this project's scope.
 */
public class TypeRateRule implements Rule {

    private final RuleDefinition definition;

    // One sliding window (deque of matching entries, oldest first) per component.
    private final ConcurrentHashMap<String, Deque<LogEntry>> windowsByComponent = new ConcurrentHashMap<>();

    public TypeRateRule(RuleDefinition definition) {
        this.definition = definition;
    }

    @Override
    public Optional<Alert> evaluate(LogEntry entry) {
        if (entry.getLevel() == null || !entry.getLevel().equalsIgnoreCase(definition.getLogLevel())) {
            return Optional.empty();
        }

        Deque<LogEntry> window = windowsByComponent.computeIfAbsent(
                entry.getComponentName(), name -> new ArrayDeque<>());

        synchronized (window) {
            window.addLast(entry);
            evictEntriesOutsideWindow(window, entry.getTimestamp());

            if (window.size() > definition.getThreshold()) {
                String description = buildDescription(entry.getComponentName(), window);
                Alert alert = new Alert(definition.getName(), entry.getComponentName(), description, LocalDateTime.now());
                return Optional.of(alert);
            }
        }

        return Optional.empty();
    }

    private void evictEntriesOutsideWindow(Deque<LogEntry> window, LocalDateTime referenceTime) {
        LocalDateTime cutoff = referenceTime.minusSeconds(definition.getWindowSeconds());
        while (!window.isEmpty() && window.peekFirst().getTimestamp().isBefore(cutoff)) {
            window.pollFirst();
        }
    }

    private String buildDescription(String componentName, Deque<LogEntry> window) {
        List<String> lastTwoMessages = window.stream()
                .skip(Math.max(0, window.size() - 2))
                .map(LogEntry::getMessage)
                .collect(Collectors.toList());

        return String.format(
                "Component '%s' produced %d %s logs in the last %d seconds (threshold: %d). Last messages: %s",
                componentName, window.size(), definition.getLogLevel(),
                definition.getWindowSeconds(), definition.getThreshold(), lastTwoMessages
        );
    }

}