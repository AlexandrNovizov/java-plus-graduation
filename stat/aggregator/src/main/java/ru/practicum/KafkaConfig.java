package ru.practicum;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Properties;

@Getter
@AllArgsConstructor
@ConfigurationProperties("kafka")
public class KafkaConfig {
    private String bootstrapServers;
    private Properties producer;
    private Properties consumer;
    private Properties topics;
}
