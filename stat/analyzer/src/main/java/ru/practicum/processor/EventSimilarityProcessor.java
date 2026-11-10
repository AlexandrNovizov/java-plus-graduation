package ru.practicum.processor;

import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.practicum.config.KafkaConfig;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.model.EventSimilarity;
import ru.practicum.service.EventSimilarityService;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static ru.practicum.config.KafkaConfig.EVENT_SIMILARITY_CONFIG_NAME;
import static ru.practicum.config.KafkaConfig.USER_ACTION_CONFIG_NAME;

@Slf4j
@Component
public class EventSimilarityProcessor implements Runnable {

    private Consumer<String, SpecificRecordBase> consumer;
    private KafkaConfig kafkaConfig;
    private final Map<TopicPartition, OffsetAndMetadata> offsets = new HashMap<>();
    private final EventSimilarityService service;
    private final int maxRetries = 3;
    private final AtomicInteger currentRetry = new AtomicInteger(0);
    private long lastOffset = -1;

    private static final Duration CLOSE_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration POLL_TIMEOUT = Duration.ofSeconds(5);


    public EventSimilarityProcessor(
            @Qualifier(EVENT_SIMILARITY_CONFIG_NAME) Consumer<String, SpecificRecordBase> eventSimilarityConsumer,
            KafkaConfig kafkaConfig,
            EventSimilarityService service
    ) {

        this.consumer = eventSimilarityConsumer;
        this.kafkaConfig = kafkaConfig;
        this.service = service;
    }


    @Override
    public void run() {
        try {
            log.trace("Subscribing topics");
            Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));
            consumer.subscribe(List.of(kafkaConfig.getTopics().getProperty(EVENT_SIMILARITY_CONFIG_NAME)));

            while (true) {
                log.trace("Polling records");
                ConsumerRecords<String, SpecificRecordBase> records = consumer.poll(POLL_TIMEOUT);
                for (ConsumerRecord<String, SpecificRecordBase> record : records) {
                    try {
                        EventSimilarityAvro eventSimilarity = (EventSimilarityAvro) record.value();
                        service.processMessage(eventSimilarity);
                        offsets.put(
                                new TopicPartition(record.topic(), record.partition()),
                                new OffsetAndMetadata(record.offset() + 1, "")
                        );
                    } catch (Exception e) {
                        retryProcessingRecord(record, e);
                        break;
                    }
                }

                if (!records.isEmpty()) {
                    consumer.commitAsync(
                            offsets,
                            (commitedOffsets, exception) -> {
                                if (exception != null) {
                                    log.warn("Error while commiting messages. Offsets: {}", commitedOffsets, exception);
                                }
                            });
                }
            }
        } catch (WakeupException ignored) {
        } catch (Exception e) {
            log.error("Fatal error while working with consumer: {}", e.getMessage());
        } finally {
            consumer.commitSync(offsets);
            consumer.close(CLOSE_TIMEOUT);
        }
    }

    private void retryProcessingRecord(ConsumerRecord<String, SpecificRecordBase> record, Exception e) {
        if (record.offset() != lastOffset) {
            lastOffset = record.offset();
            currentRetry.set(0);
        }
        log.error("Error while working with consumer: {}", e.getMessage());
        if (currentRetry.incrementAndGet() > maxRetries) {
            log.error("Retries limit exceeded to message with offset {}", record.offset());
            consumer.wakeup();
        }
        log.error("Setting offset to unprocessed message");
        consumer.seek(
                new TopicPartition(record.topic(), record.partition()),
                new OffsetAndMetadata(record.offset(), "")
        );
    }
}
