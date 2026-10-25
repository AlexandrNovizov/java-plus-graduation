package ru.practicum.feign;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.practicum.EndpointHit;
import ru.practicum.StatsRequest;
import ru.practicum.ViewStats;
import ru.practicum.exception.StatsServerUnavailable;

import java.util.List;

@Slf4j
@Component
public class StatClientFallbackFactory implements FallbackFactory<FeignStatClient> {

    @Override
    public FeignStatClient create(Throwable cause) {
        return new FeignStatClient() {
            @Override
            public void hit(EndpointHit endpointHit) {
                log.warn("stats-server unavailable while sending endpoint hit {}", endpointHit, cause);
                throw new StatsServerUnavailable(
                        "stats-server unavailable while sending endpoint hit " + endpointHit, cause);
            }

            @Override
            public List<ViewStats> getViewStats(StatsRequest request) {
                log.warn("stats-server unavailable while requesting view stats {}", request, cause);
                return List.of();
            }
        };
    }
}
