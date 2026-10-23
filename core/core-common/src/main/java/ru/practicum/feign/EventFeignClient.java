package ru.practicum.feign;

import jakarta.validation.constraints.Positive;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.feign.fallback.EventClientFallbackFactory;

@FeignClient(name = "main-service", fallbackFactory = EventClientFallbackFactory.class)
public interface EventFeignClient {

    @GetMapping("/internal/events/{eventId}")
    EventFullDto findById(@PathVariable @Positive Long eventId);
}
