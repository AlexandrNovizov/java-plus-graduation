package ru.practicum;

import lombok.RequiredArgsConstructor;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

@Configuration
@RequiredArgsConstructor
public class KafkaBeanConfig {

    private final KafkaConfig kafkaConfig;

    @Bean
    public Producer<Long, SpecificRecordBase> producer() {
        Properties properties = kafkaConfig.getProducer();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConfig.getBootstrapServers());
        return new KafkaProducer<>(properties);
    }

    @Bean
    public Consumer<Long, SpecificRecordBase> consumer() {
        Properties properties = kafkaConfig.getConsumer();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConfig.getBootstrapServers());
        return new KafkaConsumer<>(properties);
    }
}
