package ru.practicum;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.service.SimilarityCalcService;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@RequiredArgsConstructor
public class AggregationStarter {

    private final Producer<Long, SpecificRecordBase> producer;
    private final Consumer<Long, SpecificRecordBase> consumer;
    private final KafkaConfig kafkaConfig;
    private final SimilarityCalcService similarityCalcService;

    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final Duration POLL_TIMEOUT = Duration.ofSeconds(5);
    private static final String USER_ACTION_TOPIC_KEY = "user.action";
    private static final String EVENT_SIMILARITY_TOPIC_KEY = "event.similarity";
    private Map<TopicPartition, OffsetAndMetadata> offsets;


    public void start() {
        try {
            consumer.subscribe(List.of(kafkaConfig.getTopics().getProperty(USER_ACTION_TOPIC_KEY)));
            Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));
            List<Future<RecordMetadata>> sentRecordsFutures = new ArrayList<>();

            while (true) {
                ConsumerRecords<Long, SpecificRecordBase> records = consumer.poll(POLL_TIMEOUT);
                offsets = new HashMap<>();
                for (ConsumerRecord<Long, SpecificRecordBase> record : records) {
                    UserActionAvro userAction = (UserActionAvro) record.value();
                    log.info("received message: {}", userAction);
                    List<EventSimilarityAvro> calcSimilarityList = similarityCalcService.processUserAction(userAction);
                    offsets.put(
                            new TopicPartition(record.topic(), record.partition()),
                            new OffsetAndMetadata(record.offset() + 1, "")
                    );

                    if (!calcSimilarityList.isEmpty()) {
                        for (EventSimilarityAvro avro : calcSimilarityList) {
                            ProducerRecord<Long, SpecificRecordBase> simRecord = new ProducerRecord<>(
                                    kafkaConfig.getTopics().getProperty(EVENT_SIMILARITY_TOPIC_KEY),
                                    null,
                                    Instant.now().toEpochMilli(),
                                    null,
                                    avro
                            );
                            log.info("Отправлено сообщение A:{} B:{} score:{}", avro.getEventA(), avro.getEventB(), avro.getScore());
                            sentRecordsFutures.add(producer.send(simRecord));
                        }
                    }
                }

                if (!records.isEmpty()) {
                    try {
                        log.info("Ожидание {} сообщений", sentRecordsFutures.size());
                        for (Future<RecordMetadata> sentRecords : sentRecordsFutures) {
                            sentRecords.get();
                        }
                        log.info("Сообщения доставлены");
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    } catch (ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                    consumer.commitAsync(
                            offsets,
                            (commitedOffsets, exception) -> {
                                if (exception != null) {
                                    log.warn("Error while commiting messages. Offsets: {}", commitedOffsets, exception);
                                }
                            }
                    );
                }
                sentRecordsFutures.clear();
                offsets.clear();
            }
        } catch (WakeupException ignored) {
            log.info("Consumer wakeup");
        } catch (Exception e) {
            log.error("Error while handling events from users: ", e);
        } finally {
            log.info("Closing consumer");
            consumer.close(TIMEOUT);
            try {
                log.info("Flushing data");
                producer.flush();
            } finally {
                log.info("Closing producer");
                producer.close(TIMEOUT);
            }
        }
    }
}
