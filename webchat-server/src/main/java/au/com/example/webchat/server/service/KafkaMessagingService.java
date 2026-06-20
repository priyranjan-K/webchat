package au.com.example.webchat.server.service;

import au.com.example.webchat.server.dto.ChatMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;

/**
 * Publishes chat messages to Kafka using the fully non-blocking Reactor Kafka KafkaSender.
 * Messages are sent without blocking any Netty event-loop thread.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaMessagingService {

    private final KafkaSender<String, String> kafkaSender;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String TOPIC = "webchat-messages";

    public void publishMessage(ChatMessage message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            String key  = message.getGroupId() != null ? message.getGroupId() : message.getSender();

            ProducerRecord<String, String> record = new ProducerRecord<>(TOPIC, key, json);
            SenderRecord<String, String, String> senderRecord =
                    SenderRecord.create(record, message.getMessageId());

            kafkaSender.send(Mono.just(senderRecord))
                    .doOnError(e -> log.error("Reactive Kafka send failed", e))
                    .doOnNext(result -> log.debug(
                            "Published to Kafka topic='{}' offset={} key={}",
                            TOPIC,
                            result.recordMetadata().offset(),
                            key))
                    .subscribe();

        } catch (Exception e) {
            log.error("Error serialising message for Kafka", e);
        }
    }
}
