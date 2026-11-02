package ru.practicum.kafka;

import lombok.RequiredArgsConstructor;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Service;
import ru.practicum.KafkaConfig;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class KafkaProducerImpl implements KafkaProducer {

    private final Producer<String, SpecificRecordBase> producer;
    private final KafkaConfig kafkaConfig;

    private static final String USER_TOPIC = "user.action";

    @Override
    public void send(UserActionAvro userActionAvro) {
        ProducerRecord<String, SpecificRecordBase> record = new ProducerRecord<>(
                kafkaConfig.getTopics().getProperty(USER_TOPIC),
                null,
                userActionAvro.getTimestamp().toEpochMilli(),
                String.valueOf(userActionAvro.getUserId()),
                userActionAvro
        );
        producer.send(record);
    }

    @Override
    public void close() {
        producer.flush();
        producer.close(Duration.ofSeconds(10));
    }
}
