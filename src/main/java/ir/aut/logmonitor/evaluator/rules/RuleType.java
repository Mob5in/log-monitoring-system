package ir.aut.logmonitor.evaluator.rules;

/**
 * The kinds of rules our engine supports. Each corresponds to one of the
 * rule categories required by the spec.
 */
public enum RuleType {
    /** Alert on every log of a specific level (e.g. every ERROR). */
    LOG_TYPE,
    /** Alert if a component produces more than N logs of a specific level within a time window. */
    TYPE_RATE,
    /** Alert if a component's overall log rate (any level) exceeds a threshold within a time window. */
    OVERALL_RATE
}

