package ir.aut.logmonitor.evaluator;

import ir.aut.logmonitor.alert.Alert;
import ir.aut.logmonitor.alert.AlertRepository;
import ir.aut.logmonitor.common.model.LogEntry;
import ir.aut.logmonitor.evaluator.rules.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the active set of {@link Rule}s from the definitions loaded by
 * {@link RulesConfigLoader}, and evaluates every incoming {@link LogEntry}
 * against all of them. Any generated {@link Alert}s are persisted via
 * {@link AlertRepository}.
 */
@Component
@Profile({"evaluator", "default"})
public class RuleEngine {

    private static final Logger log = LoggerFactory.getLogger(RuleEngine.class);

    private final List<Rule> rules;
    private final AlertRepository alertRepository;

    public RuleEngine(RulesConfigLoader rulesConfigLoader, AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
        this.rules = buildRules(rulesConfigLoader.getRuleDefinitions());
        log.info("RuleEngine initialized with {} active rule(s)", rules.size());
    }

    /**
     * Evaluates a single log entry against every active rule, saving any
     * alerts that get generated.
     */
    public void process(LogEntry entry) {
        for (Rule rule : rules) {
            rule.evaluate(entry).ifPresent(alert -> {
                alertRepository.save(alert);
                log.info("Alert generated: rule='{}', component='{}'", alert.getRuleName(), alert.getComponentName());
            });
        }
    }

    private List<Rule> buildRules(List<RuleDefinition> definitions) {
        List<Rule> result = new ArrayList<>();

        for (RuleDefinition definition : definitions) {
            if (definition.getType() == null) {
                log.warn("Skipping rule '{}': missing 'type'", definition.getName());
                continue;
            }

            switch (definition.getType()) {
                case LOG_TYPE -> result.add(new LogTypeRule(definition));
                case TYPE_RATE -> result.add(new TypeRateRule(definition));
                case OVERALL_RATE -> result.add(new OverallRateRule(definition));
            }
        }

        return result;
    }
}