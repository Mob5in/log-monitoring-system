package ir.aut.logmonitor.ingester;

import ir.aut.logmonitor.common.model.LogEntry;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class LogFileParserTest {

    private final LogFileParser parser = new LogFileParser();

    @Test
    void parsesAValidLogLineCorrectly() {
        String rawLine = "2021-07-12 01:22:42,114 [ThreadName] INFO package.name.ClassName – msg";

        Optional<LogEntry> result = parser.parseLine(rawLine, "auth-service");

        assertThat(result).isPresent();
        LogEntry entry = result.get();
        assertThat(entry.getTimestamp()).isEqualTo(LocalDateTime.of(2021, 7, 12, 1, 22, 42, 114_000_000));
        assertThat(entry.getThreadName()).isEqualTo("ThreadName");
        assertThat(entry.getLevel()).isEqualTo("INFO");
        assertThat(entry.getLoggerName()).isEqualTo("package.name.ClassName");
        assertThat(entry.getMessage()).isEqualTo("msg");
        assertThat(entry.getComponentName()).isEqualTo("auth-service");
    }

    @Test
    void parsesAnErrorLineWithAMultiWordMessage() {
        String rawLine = "2023-01-05 14:03:11,500 [main] ERROR ir.aut.Service – Something went wrong here";

        Optional<LogEntry> result = parser.parseLine(rawLine, "billing-service");

        assertThat(result).isPresent();
        assertThat(result.get().getLevel()).isEqualTo("ERROR");
        assertThat(result.get().getMessage()).isEqualTo("Something went wrong here");
    }

    @Test
    void returnsEmptyForABlankLine() {
        assertThat(parser.parseLine("", "auth-service")).isEmpty();
        assertThat(parser.parseLine("   ", "auth-service")).isEmpty();
        assertThat(parser.parseLine(null, "auth-service")).isEmpty();
    }

    @Test
    void returnsEmptyForAMalformedLine() {
        // Missing thread name brackets and level — doesn't match the expected format at all
        String malformed = "this is not a valid log line";

        assertThat(parser.parseLine(malformed, "auth-service")).isEmpty();
    }

    @Test
    void returnsEmptyForALineWithAnInvalidTimestamp() {
        // Month 13 doesn't exist
        String badTimestamp = "2021-13-12 01:22:42,114 [ThreadName] INFO package.name.ClassName – msg";

        assertThat(parser.parseLine(badTimestamp, "auth-service")).isEmpty();
    }
}