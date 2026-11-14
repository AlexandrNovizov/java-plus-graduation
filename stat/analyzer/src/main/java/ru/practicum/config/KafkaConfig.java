package ru.practicum.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;
import java.util.Properties;

@Getter
@ConfigurationProperties(prefix = "kafka")
@AllArgsConstructor
public class KafkaConfig {

    public static final String USER_ACTION_CONFIG_NAME = "user.action";
    public static final String EVENT_SIMILARITY_CONFIG_NAME = "event.similarity";

    private Map<String, TopicConsumerConfig> consumers;
    private Properties topics;
    private String bootstrapServers;
}
