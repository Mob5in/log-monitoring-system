package ir.aut.logmonitor.evaluator;

import ir.aut.logmonitor.common.model.LogEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * Consumes log entries from the Kafka topic (as published by the File
 * Ingester subsystem) and feeds each one into the {@link RuleEngine} for
 * evaluation.
 */
@Service
public class LogConsumerService {

    private static final Logger log = LoggerFactory.getLogger(LogConsumerService.class);

    private final RuleEngine ruleEngine;

    public LogConsumerService(RuleEngine ruleEngine) {
        this.ruleEngine = ruleEngine;
    }

    @KafkaListener(topics = "${app.kafka.topic.logs}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(LogEntry entry) {
        log.debug("Received log entry from Kafka: {}", entry);
        try {
            ruleEngine.process(entry);
        } catch (Exception e) {
            // We don't want a single bad/unexpected entry to kill the consumer thread
            // and stop the whole pipeline — log it and keep going.
            log.error("Failed to evaluate rules for log entry: {}", entry, e);
        }
    }
}