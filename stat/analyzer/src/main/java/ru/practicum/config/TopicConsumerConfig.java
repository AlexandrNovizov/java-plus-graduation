package ru.practicum.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TopicConsumerConfig {

    private String keyDeserializer;

    private String valueDeserializer;

    private String groupId;

    private boolean enableAutoCommit;

    private Duration pollTimeout;
}
