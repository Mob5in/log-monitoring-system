package ir.aut.logmonitor.evaluator;

import ir.aut.logmonitor.alert.Alert;
import ir.aut.logmonitor.alert.AlertRepository;
import ir.aut.logmonitor.common.model.LogEntry;
import ir.aut.logmonitor.evaluator.rules.RuleDefinition;
import ir.aut.logmonitor.evaluator.rules.RuleType;
import ir.aut.logmonitor.evaluator.rules.RulesConfigLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RuleEngine. AlertRepository and RulesConfigLoader are
 * mocked so we can test the wiring/orchestration logic (does it build the
 * right rules, does it save alerts when triggered) without a real database.
 */
@ExtendWith(MockitoExtension.class)
class RuleEngineTest {

    @Mock
    private RulesConfigLoader rulesConfigLoader;

    @Mock
    private AlertRepository alertRepository;

    private RuleDefinition logTypeRuleDefinition() {
        RuleDefinition def = new RuleDefinition();
        def.setName("error-log-rule");
        def.setType(RuleType.LOG_TYPE);
        def.setLogLevel("ERROR");
        return def;
    }

    private LogEntry logEntry(String level, String message, String component) {
        return new LogEntry(LocalDateTime.now(), "main", level, "some.Logger", message, component);
    }

    @Test
    void savesAlertWhenARuleTriggers() {
        when(rulesConfigLoader.getRuleDefinitions()).thenReturn(List.of(logTypeRuleDefinition()));
        RuleEngine ruleEngine = new RuleEngine(rulesConfigLoader, alertRepository);

        ruleEngine.process(logEntry("ERROR", "Something broke", "auth-service"));

        verify(alertRepository, times(1)).save(any(Alert.class));
    }

    @Test
    void doesNotSaveAlertWhenNoRuleTriggers() {
        when(rulesConfigLoader.getRuleDefinitions()).thenReturn(List.of(logTypeRuleDefinition()));
        RuleEngine ruleEngine = new RuleEngine(rulesConfigLoader, alertRepository);

        ruleEngine.process(logEntry("INFO", "All good", "auth-service"));

        verify(alertRepository, never()).save(any(Alert.class));
    }

    @Test
    void skipsRuleDefinitionsWithMissingTypeWithoutCrashing() {
        RuleDefinition invalidDefinition = new RuleDefinition();
        invalidDefinition.setName("broken-rule");
        // type intentionally left null, simulating a malformed rules.yml entry

        when(rulesConfigLoader.getRuleDefinitions()).thenReturn(List.of(invalidDefinition));
        RuleEngine ruleEngine = new RuleEngine(rulesConfigLoader, alertRepository);

        ruleEngine.process(logEntry("ERROR", "msg", "auth-service"));

        verify(alertRepository, never()).save(any(Alert.class));
    }

    @Test
    void evaluatesMultipleRulesIndependently() {
        RuleDefinition errorRule = logTypeRuleDefinition();

        RuleDefinition warningRule = new RuleDefinition();
        warningRule.setName("warning-log-rule");
        warningRule.setType(RuleType.LOG_TYPE);
        warningRule.setLogLevel("WARNING");

        when(rulesConfigLoader.getRuleDefinitions()).thenReturn(List.of(errorRule, warningRule));
        RuleEngine ruleEngine = new RuleEngine(rulesConfigLoader, alertRepository);

        ruleEngine.process(logEntry("WARNING", "careful", "auth-service"));

        // Only the warning rule should have matched -> exactly one alert saved
        verify(alertRepository, times(1)).save(any(Alert.class));
    }
}