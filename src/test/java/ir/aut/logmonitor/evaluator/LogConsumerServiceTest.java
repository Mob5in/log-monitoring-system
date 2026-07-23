package ir.aut.logmonitor.evaluator;

import ir.aut.logmonitor.common.model.LogEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for LogConsumerService. RuleEngine is mocked since this class's
 * only job is to delegate consumed messages to it (and not let exceptions
 * escape and kill the Kafka listener thread).
 */
@ExtendWith(MockitoExtension.class)
class LogConsumerServiceTest {

    @Mock
    private RuleEngine ruleEngine;

    private LogEntry sampleEntry() {
        return new LogEntry(LocalDateTime.now(), "main", "ERROR", "some.Logger", "msg", "auth-service");
    }

    @Test
    void delegatesEachConsumedEntryToRuleEngine() {
        LogConsumerService consumerService = new LogConsumerService(ruleEngine);
        LogEntry entry = sampleEntry();

        consumerService.consume(entry);

        verify(ruleEngine).process(entry);
    }

    @Test
    void doesNotPropagateExceptionsThrownByRuleEngine() {
        LogConsumerService consumerService = new LogConsumerService(ruleEngine);
        LogEntry entry = sampleEntry();
        doThrow(new RuntimeException("boom")).when(ruleEngine).process(entry);

        // Should be swallowed and logged, not propagated — a bad entry shouldn't kill the consumer thread
        assertThatCode(() -> consumerService.consume(entry)).doesNotThrowAnyException();
    }
}