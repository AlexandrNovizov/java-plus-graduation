package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.entity.event.EventService;

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
}
