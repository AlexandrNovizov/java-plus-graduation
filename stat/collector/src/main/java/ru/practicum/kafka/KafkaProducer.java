package ru.practicum.kafka;

import ru.practicum.ewm.stats.avro.UserActionAvro;

public interface KafkaProducer {

    void send(UserActionAvro userActionAvro);

    void close();
}
