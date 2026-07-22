package ir.aut.logmonitor.evaluator.rules;

import java.util.List;

/**
 * Matches the top-level structure of rules.yml:
 *
 * rules:
 *   - name: ...
 *     type: ...
 *     ...
 */
public class RulesConfig {

    private List<RuleDefinition> rules;

    public RulesConfig() {
    }

    public List<RuleDefinition> getRules() {
        return rules;
    }

    public void setRules(List<RuleDefinition> rules) {
        this.rules = rules;
    }
}