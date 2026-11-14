package ru.practicum;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import ru.practicum.config.KafkaConfig;
import ru.practicum.processor.EventSimilarityProcessor;
import ru.practicum.processor.UserActionProcessor;

@SpringBootApplication
@EnableConfigurationProperties(KafkaConfig.class)
public class AnalyzerApp {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(AnalyzerApp.class, args);
        final UserActionProcessor userActionProcessor = context.getBean(UserActionProcessor.class);

        Thread userActionThread = new Thread(userActionProcessor);
        userActionThread.setName("UserActionHandlerThread");
        userActionThread.start();

        EventSimilarityProcessor eventSimilarityProcessor = context.getBean(EventSimilarityProcessor.class);
        Thread.currentThread().setName("EventSimilarityHandlerThread");
        eventSimilarityProcessor.run();
    }
}
