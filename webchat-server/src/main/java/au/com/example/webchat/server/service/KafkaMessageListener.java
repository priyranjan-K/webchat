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
 * Consumes messages from Kafka using the fully non-blocking Reactor Kafka KafkaReceiver.
 *
 * <p>A persistent Flux subscription is established on startup. Each record is
 * processed asynchronously on a bounded-elastic thread (to avoid blocking Netty
 * event-loop threads during JSON parsing) and immediately routes the message to
 * the appropriate WebSocket sink(s) — no polling delay, no thread blocking.</p>
 */
@Service
@Slf4j
public class KafkaMessageListener {

    private final KafkaReceiver<String, String> kafkaReceiver;
    private final ReactiveChatWebSocketHandler  chatWebSocketHandler;
    private final ObjectMapper                  objectMapper = new ObjectMapper();

    /** Holds the disposable so we can cancel on shutdown. */
    private reactor.core.Disposable subscription;

    public KafkaMessageListener(
            KafkaReceiver<String, String> kafkaReceiver,
            @Lazy ReactiveChatWebSocketHandler chatWebSocketHandler) {
        this.kafkaReceiver        = kafkaReceiver;
        this.chatWebSocketHandler = chatWebSocketHandler;
    }

    @PostConstruct
    public void startConsuming() {
        log.info("Starting Reactive Kafka consumer subscription …");

        Flux<ReceiverRecord<String, String>> records = kafkaReceiver.receive();

        subscription = records
                // Move JSON parsing off the Kafka poll thread (bounded-elastic = virtual thread backed)
                .publishOn(Schedulers.boundedElastic())
                .doOnNext(record -> {
                    try {
                        String json = record.value();
                        ChatMessage message = objectMapper.readValue(json, ChatMessage.class);
                        log.debug("Reactive Kafka consumer received: type={} sender={}",
                                message.getType(), message.getSender());
                        chatWebSocketHandler.routeMessageFromKafka(message);
                        // Acknowledge offset so Kafka knows this record is processed
                        record.receiverOffset().acknowledge();
                    } catch (Exception e) {
                        log.error("Error processing Kafka record: {}", record.value(), e);
                        record.receiverOffset().acknowledge(); // ack even on error to avoid reprocessing
                    }
                })
                .doOnError(e -> log.error("Reactive Kafka consumer stream error", e))
                // Restart the stream automatically on errors (e.g. transient broker issues)
                .onErrorResume(e -> {
                    log.warn("Restarting reactive Kafka consumer after error: {}", e.getMessage());
                    return kafkaReceiver.receive()
                            .publishOn(Schedulers.boundedElastic());
                })
                .subscribe(
                        record -> { /* handled in doOnNext */ },
                        e -> log.error("Reactive Kafka consumer terminated with error", e),
                        () -> log.info("Reactive Kafka consumer stream completed")
                );

        log.info("Reactive Kafka consumer subscription active.");
    }

    @PreDestroy
    public void stopConsuming() {
        if (subscription != null && !subscription.isDisposed()) {
            subscription.dispose();
            log.info("Reactive Kafka consumer subscription disposed.");
        }
    }
}
