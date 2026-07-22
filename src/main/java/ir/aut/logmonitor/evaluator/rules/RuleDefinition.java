package ir.aut.logmonitor.evaluator.rules;

/**
 * Represents one rule definition as read from the rules config file
 * (rules.yml). Not every field applies to every rule type:
 *
 *  - LOG_TYPE:     name, type, logLevel
 *  - TYPE_RATE:    name, type, logLevel, windowSeconds, threshold
 *  - OVERALL_RATE: name, type, windowSeconds, threshold
 *
 * This is a plain bean (public no-arg constructor + getters/setters) because
 * SnakeYAML (used to parse the file) populates objects via reflection using
 * that convention.
 */
public class RuleDefinition {

    private String name;
    private RuleType type;
    private String logLevel;
    private Integer windowSeconds;
    private Integer threshold;

    public RuleDefinition() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public RuleType getType() {
        return type;
    }

    public void setType(RuleType type) {
        this.type = type;
    }

    public String getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }

    public Integer getWindowSeconds() {
        return windowSeconds;
    }

    public void setWindowSeconds(Integer windowSeconds) {
        this.windowSeconds = windowSeconds;
    }

    public Integer getThreshold() {
        return threshold;
    }

    public void setThreshold(Integer threshold) {
        this.threshold = threshold;
    }
}