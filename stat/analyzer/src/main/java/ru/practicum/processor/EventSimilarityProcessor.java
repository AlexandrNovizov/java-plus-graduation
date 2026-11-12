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
import ru.practicum.service.EventSimilarityService;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static ru.practicum.config.KafkaConfig.EVENT_SIMILARITY_CONFIG_NAME;

@Slf4j
@Component
public class EventSimilarityProcessor implements Runnable {

    private final Consumer<String, SpecificRecordBase> consumer;
    private final KafkaConfig kafkaConfig;
    private final EventSimilarityService service;

    private final Map<TopicPartition, AtomicLong> retryCounts = new HashMap<>();

    private Map<TopicPartition, OffsetAndMetadata> offsetsToCommit;

    private static final long MAX_RETRIES = 3;

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
                offsetsToCommit = new HashMap<>();

                for (ConsumerRecord<String, SpecificRecordBase> record : records) {
                    TopicPartition tp = new TopicPartition(record.topic(), record.partition());

                    try {
                        EventSimilarityAvro eventSimilarity = (EventSimilarityAvro) record.value();
                        service.processMessage(eventSimilarity);
                        offsetsToCommit.put(
                                tp,
                                new OffsetAndMetadata(record.offset() + 1, "")
                        );
                        retryCounts.remove(tp);
                    } catch (Exception e) {
                        handleProcessingError(record, tp, e);
                        break;
                    }
                }

                if (!offsetsToCommit.isEmpty()) {
                    consumer.commitAsync(
                            offsetsToCommit,
                            (commitedOffsets, exception) -> {
                                if (exception != null) {
                                    log.warn("Error while commiting messages. Offsets: {}", commitedOffsets, exception);
                                }
                            });
                }
            }
        } catch (WakeupException ignored) {
            log.info("Consumer wakeup");
        } catch (Exception e) {
            log.error("Fatal error while working with consumer: {}", e.getMessage());
        } finally {
            consumer.commitSync(offsetsToCommit);
            consumer.close(CLOSE_TIMEOUT);
        }
    }

    private void handleProcessingError(
            ConsumerRecord<String, SpecificRecordBase> record,
            TopicPartition tp,
            Exception e
    ) {
        AtomicLong retryCounter = retryCounts.computeIfAbsent(tp, k -> new AtomicLong(0));
        long currentRetries = retryCounter.incrementAndGet();

        log.error(
                "Error processing record (topic={}, partition={}, offset={}, retries={})",
                record.topic(),
                record.partition(),
                record.offset(),
                currentRetries,
                e
        );

        if (currentRetries > MAX_RETRIES) {
            log.error("Retries limit exceeded for message with offset {}. Consumer wakeup.", record.offset());
            retryCounts.remove(tp);
            consumer.wakeup();
        }

        log.debug("Setting seek back to offset {} for partition {}", record.offset(), tp.partition());

        consumer.seek(tp, record.offset());
    }
}
