package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.service.EventService;

import java.util.List;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/internal/events")
public class InternalEventController {

    private final EventService eventService;

    @GetMapping("/{eventId}")
    @ResponseStatus(HttpStatus.OK)
    EventFullDto getById(@PathVariable Long eventId) {
        return eventService.getByEventId(eventId);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    List<EventShortDto> getByIds(@RequestParam Set<Long> ids) {
        return eventService.getByEventIds(ids);
    }

    @GetMapping("/category/{categoryId}")
    @ResponseStatus(HttpStatus.OK)
    boolean existsByCategoryId(@PathVariable Long categoryId) {
        return eventService.existsByCategoryId(categoryId);
    }

    @GetMapping("/{eventId}/exists")
    @ResponseStatus(HttpStatus.OK)
    boolean isEventExists(@PathVariable Long eventId) {
        return eventService.isEventExists(eventId);
    }
}
