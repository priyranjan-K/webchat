package au.com.example.webchat.server.service;

import au.com.example.webchat.server.dto.ChatMessage;
import au.com.example.webchat.server.websocket.ReactiveChatWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverRecord;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * Subscribes to the {@code webchat-messages} Kafka topic using a reactive
 * {@link KafkaReceiver} and routes each consumed record to the correct
 * WebSocket session(s) via {@link ReactiveChatWebSocketHandler}.
 */
@Service
@Slf4j
public class KafkaMessageListener {

    private final KafkaReceiver<String, String> kafkaReceiver;
    private final ReactiveChatWebSocketHandler  chatWebSocketHandler;
    private final ObjectMapper                  objectMapper = new ObjectMapper();

    private reactor.core.Disposable subscription;

    public KafkaMessageListener(
            KafkaReceiver<String, String> kafkaReceiver,
            @Lazy ReactiveChatWebSocketHandler chatWebSocketHandler) {
        this.kafkaReceiver        = kafkaReceiver;
        this.chatWebSocketHandler = chatWebSocketHandler;
    }

    @PostConstruct
    public void startConsuming() {
        log.info("Starting Kafka consumer …");

        Flux<ReceiverRecord<String, String>> records = kafkaReceiver.receive();

        subscription = records
                .publishOn(Schedulers.boundedElastic())
                .doOnNext(record -> {
                    try {
                        ChatMessage message = objectMapper.readValue(record.value(), ChatMessage.class);
                        log.debug("Kafka consumed: type={} sender={}", message.getType(), message.getSender());
                        chatWebSocketHandler.routeMessageFromKafka(message);
                        record.receiverOffset().acknowledge();
                    } catch (Exception e) {
                        log.error("Error processing Kafka record: {}", record.value(), e);
                        record.receiverOffset().acknowledge();
                    }
                })
                .doOnError(e -> log.error("Kafka consumer stream error", e))
                .onErrorResume(e -> {
                    log.warn("Restarting Kafka consumer after error: {}", e.getMessage());
                    return kafkaReceiver.receive().publishOn(Schedulers.boundedElastic());
                })
                .subscribe(
                        record -> {},
                        e -> log.error("Kafka consumer terminated", e),
                        () -> log.info("Kafka consumer stream completed")
                );

        log.info("Kafka consumer subscription active.");
    }

    @PreDestroy
    public void stopConsuming() {
        if (subscription != null && !subscription.isDisposed()) {
            subscription.dispose();
            log.info("Kafka consumer subscription disposed.");
        }
    }
}
