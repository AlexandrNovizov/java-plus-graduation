package ru.practicum.feign.fallback;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.practicum.dto.stat.EndpointHit;
import ru.practicum.dto.stat.StatsRequest;
import ru.practicum.dto.stat.ViewStats;
import ru.practicum.feign.StatFeignClient;
import ru.practicum.feign.exception.StatsServerUnavailable;

import java.util.List;

@Slf4j
@Component
public class StatClientFallbackFactory implements FallbackFactory<StatFeignClient> {

    @Override
    public StatFeignClient create(Throwable cause) {
        return new StatFeignClient() {
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
