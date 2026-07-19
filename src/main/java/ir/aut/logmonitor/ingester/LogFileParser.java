package ir.aut.logmonitor.ingester;

import ir.aut.logmonitor.common.model.LogEntry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses raw log file lines into {@link LogEntry} objects.
 *
 * Expected default line format:
 *   2021-07-12 01:22:42,114 [ThreadName] INFO package.name.ClassName – msg
 *
 * The regex pattern is configurable via `app.logs.line-pattern` in
 * application.properties, so the expected log format is not hardcoded.
 */
@Component
public class LogFileParser {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss,SSS");

    // Default pattern with 5 capture groups: timestamp, thread, level, logger, message.
    // Note: uses an en dash (–) before the message, matching the spec's example format.
    private static final String DEFAULT_PATTERN =
            "^(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2},\\d{3})\\s+\\[([^\\]]*)\\]\\s+(\\S+)\\s+(\\S+)\\s+[\\u2013-]\\s+(.*)$";

    private final Pattern linePattern;

    public LogFileParser(@Value("${app.logs.line-pattern:" + DEFAULT_PATTERN + "}") String pattern) {
        this.linePattern = Pattern.compile(pattern);
    }

    /** Convenience constructor using the default pattern (handy for unit tests). */
    public LogFileParser() {
        this(DEFAULT_PATTERN);
    }

    /**
     * Attempts to parse a single raw log line.
     *
     * @param rawLine       the raw text line read from the log file
     * @param componentName the component this log file belongs to (derived from the file name)
     * @return a populated LogEntry if the line matched the expected format,
     *         or Optional.empty() if the line is malformed/unparseable (e.g. blank lines,
     *         multi-line stack traces continuing a previous entry, etc.)
     */
    public Optional<LogEntry> parseLine(String rawLine, String componentName) {
        if (rawLine == null || rawLine.isBlank()) {
            return Optional.empty();
        }

        Matcher matcher = linePattern.matcher(rawLine.trim());
        if (!matcher.matches()) {
            return Optional.empty();
        }

        try {
            LocalDateTime timestamp = LocalDateTime.parse(matcher.group(1), TIMESTAMP_FORMATTER);
            String threadName = matcher.group(2);
            String level = matcher.group(3);
            String loggerName = matcher.group(4);
            String message = matcher.group(5);

            return Optional.of(new LogEntry(timestamp, threadName, level, loggerName, message, componentName));
        } catch (Exception e) {
            // Timestamp didn't parse or some other unexpected issue — treat as unparseable line.
            return Optional.empty();
        }
    }
}