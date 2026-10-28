package ru.practicum.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.practicum.dto.stat.StatsRequest;
import ru.practicum.dto.stat.EndpointHit;
import ru.practicum.dto.stat.ViewStats;
import ru.practicum.feign.fallback.StatClientFallbackFactory;

import java.util.List;

@FeignClient(name = "stats-server", fallbackFactory = StatClientFallbackFactory.class)
public interface StatFeignClient {

    @PostMapping("/hit")
    void hit(@RequestBody EndpointHit endpointHit);

    @GetMapping("/stats")
    List<ViewStats> getViewStats(@SpringQueryMap StatsRequest request);
}
