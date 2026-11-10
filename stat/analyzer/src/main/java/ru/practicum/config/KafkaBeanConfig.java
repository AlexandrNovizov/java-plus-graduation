package ru.practicum.config;

import lombok.RequiredArgsConstructor;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

import static ru.practicum.config.KafkaConfig.*;

@Configuration
@RequiredArgsConstructor
public class KafkaBeanConfig {

    private final KafkaConfig kafkaConfig;

    @Bean(name = USER_ACTION_CONFIG_NAME)
    public Consumer<String, SpecificRecordBase> userActionConsumer() {
        Properties properties = new Properties();
        TopicConsumerConfig topicConsumerConfig = kafkaConfig.getConsumers().get(USER_ACTION_CONFIG_NAME);
        initKafkaConsumer(topicConsumerConfig, properties);
        return new KafkaConsumer<>(properties);
    }

    @Bean(name = EVENT_SIMILARITY_CONFIG_NAME)
    public Consumer<String, SpecificRecordBase> eventSimilarityConsumer() {
        Properties properties = new Properties();
        TopicConsumerConfig topicConsumerConfig = kafkaConfig.getConsumers().get(EVENT_SIMILARITY_CONFIG_NAME);
        initKafkaConsumer(topicConsumerConfig, properties);
        return new KafkaConsumer<>(properties);
    }

    public void initKafkaConsumer(TopicConsumerConfig topicConsumerConfig, Properties properties) {
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, topicConsumerConfig.getKeyDeserializer());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, topicConsumerConfig.getValueDeserializer());
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, topicConsumerConfig.getGroupId());
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, topicConsumerConfig.isEnableAutoCommit());
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConfig.getBootstrapServers());
    }
}
