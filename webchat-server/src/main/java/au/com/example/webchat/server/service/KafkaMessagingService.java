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
 * Publishes chat messages to the {@code webchat-messages} Kafka topic
 * using a non-blocking {@link KafkaSender}.
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
                    .doOnError(e -> log.error("Kafka send failed", e))
                    .doOnNext(r -> log.debug("Published to topic={} offset={}", TOPIC, r.recordMetadata().offset()))
                    .subscribe();

        } catch (Exception e) {
            log.error("Error serialising message for Kafka", e);
        }
    }
}
