package ru.practicum.client;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.EndpointHit;
import ru.practicum.ViewStats;
import ru.practicum.client.feign.FeignStatClient;

import java.util.Collections;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class StatClient {

    private final FeignStatClient feignStatClient;

    public void hit(EndpointHit endpointHit) {
        try {
            feignStatClient.hit(endpointHit);
        } catch (FeignException e) {
            log.error("Stats service unavailable: {}", e.getMessage(), e);
        }
    }

    public List<ViewStats> getViewStats(StatsRequest statsRequest) {
        try {
            return feignStatClient.getViewStats(statsRequest);
        } catch (FeignException e) {
            log.error("Stats service unavailable: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
}