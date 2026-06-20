package au.com.example.webchat.server.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Configures Reactor Kafka (reactor-kafka) beans for fully non-blocking,
 * event-driven Kafka producer and consumer on Netty event-loop threads.
 */
@Configuration
public class ReactiveKafkaConfig {

    private static final String TOPIC = "webchat-messages";

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    // ── Producer (KafkaSender) ────────────────────────────────────────────────

    @Bean
    public KafkaSender<String, String> kafkaSender() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,      bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,   StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG,                   "1");
        props.put(ProducerConfig.LINGER_MS_CONFIG,              "0");
        props.put(ProducerConfig.BATCH_SIZE_CONFIG,             "1");
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG,       "none");

        SenderOptions<String, String> senderOptions =
                SenderOptions.<String, String>create(props)
                        .maxInFlight(1024);

        return KafkaSender.create(senderOptions);
    }

    // ── Consumer (KafkaReceiver) ──────────────────────────────────────────────

    @Bean
    public KafkaReceiver<String, String> kafkaReceiver() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,        bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG,                 "webchat-reactive-group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,   StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,        "latest");
        // Poll immediately — don't wait to batch records
        props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG,          "1");
        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG,        "0");
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG,         "100");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,       "true");
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG,  "100");

        ReceiverOptions<String, String> receiverOptions =
                ReceiverOptions.<String, String>create(props)
                        .subscription(Collections.singleton(TOPIC))
                        .commitBatchSize(10)
                        .commitInterval(Duration.ofMillis(100));

        return KafkaReceiver.create(receiverOptions);
    }
}
