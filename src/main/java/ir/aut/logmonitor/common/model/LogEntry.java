package ir.aut.logmonitor.common.model;

import java.time.LocalDateTime;

/**
 * Represents a single parsed log line, extracted from a raw log file.
 *
 * Raw log line format:
 *   2021-07-12 01:22:42,114 [ThreadName] INFO package.name.ClassName – msg
 *
 * This object is what gets published to Kafka by the File Ingester,
 * and consumed by the Rule Evaluator.
 */
public class LogEntry {

    /** Exact date/time the log line was produced. */
    private LocalDateTime timestamp;

    /** Name of the thread that produced the log line. */
    private String threadName;

    /** Log level, e.g. INFO, WARNING, ERROR. */
    private String level;

    /** Fully qualified class/package name that logged the message. */
    private String loggerName;

    /** The actual log message content. */
    private String message;

    /**
     * Name of the component (system) this log belongs to.
     * NOT part of the raw log line itself — it's derived from the
     * log file name (see the file naming convention in the spec).
     */
    private String componentName;

    // Required no-arg constructor (needed by Jackson for JSON deserialization)
    public LogEntry() {
    }

    public LogEntry(LocalDateTime timestamp, String threadName, String level,
                    String loggerName, String message, String componentName) {
        this.timestamp = timestamp;
        this.threadName = threadName;
        this.level = level;
        this.loggerName = loggerName;
        this.message = message;
        this.componentName = componentName;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getThreadName() {
        return threadName;
    }

    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getLoggerName() {
        return loggerName;
    }

    public void setLoggerName(String loggerName) {
        this.loggerName = loggerName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getComponentName() {
        return componentName;
    }

    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    @Override
    public String toString() {
        return "LogEntry{" +
                "timestamp=" + timestamp +
                ", threadName='" + threadName + '\'' +
                ", level='" + level + '\'' +
                ", loggerName='" + loggerName + '\'' +
                ", message='" + message + '\'' +
                ", componentName='" + componentName + '\'' +
                '}';
    }
}