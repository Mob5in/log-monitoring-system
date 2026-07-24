package ir.aut.logmonitor.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Declares the raw-logs Kafka topic with multiple partitions.
 *
 * Why this matters for horizontal scaling: Kafka only spreads messages
 * across multiple consumers *in the same consumer group* if the topic has
 * more partitions than 1. Since all Rule Evaluator instances share the same
 * group id (see spring.kafka.consumer.group-id in application.properties),
 * without enough partitions only one evaluator instance would ever receive
 * messages, no matter how many instances you run.
 *
 * Spring's KafkaAdmin will create this topic automatically on startup if it
 * doesn't already exist. If the topic was already auto-created earlier with
 * fewer partitions (e.g. during initial testing), you may need to delete it
 * via Kafka UI (http://localhost:8081) and let it get recreated, since
 * partition counts aren't always safely increased on an existing topic.
 */
@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic logsTopic(@Value("${app.kafka.topic.logs}") String topicName,
                              @Value("${app.kafka.topic.logs.partitions:3}") int partitions) {
        return TopicBuilder.name(topicName)
                .partitions(partitions)
                .replicas(1)
                .build();
    }
}