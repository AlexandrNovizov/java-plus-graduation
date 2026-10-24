package ru.practicum.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.feign.fallback.EventClientFallbackFactory;

import java.util.List;
import java.util.Set;

@FeignClient(name = "event-service", fallbackFactory = EventClientFallbackFactory.class)
public interface EventFeignClient {

    @GetMapping("/internal/events/{eventId}")
    EventFullDto findById(@PathVariable Long eventId);

    @GetMapping("/internal/events/category/{categoryId}")
    @ResponseStatus(HttpStatus.OK)
    boolean existsByCategoryId(@PathVariable Long categoryId);

    @GetMapping("/internal/events/{eventId}/exists")
    @ResponseStatus(HttpStatus.OK)
    boolean isEventExists(@PathVariable Long eventId);

    @GetMapping("/internal/events")
    @ResponseStatus(HttpStatus.OK)
    List<EventShortDto> getByIds(@RequestParam(name = "ids") Set<Long> eventIds);

}
