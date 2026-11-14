package ru.practicum.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventPublicParamDto;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.service.EventService;

import java.util.List;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
@Validated
public class EventPublicController {

    private final EventService eventService;

    @GetMapping
    public List<EventShortDto> getEvents(@ModelAttribute @Valid EventPublicParamDto params,
                                         HttpServletRequest request) {
        return eventService.getPublishedEvents(params, request);
    }

    @GetMapping("/{id}")
    public EventFullDto getEventById(@PathVariable @Positive Long id,
                                     @RequestHeader("X-EWM-USER-ID") @Positive Long userId,
                                     HttpServletRequest request) {
        return eventService.getPublishedEventById(id, userId, request);
    }

    @GetMapping("/recommendations")
    public List<EventShortDto> getRecommendations(@RequestHeader("X-EWM-USER-ID") @Positive Long userId,
                                                  @RequestParam(value = "limit", required = false, defaultValue = "10") @Positive int limit) {
        return eventService.getRecommendations(userId, limit);
    }

    @PutMapping("/{eventId}/like")
    public void likeEvent(@PathVariable @Positive Long eventId,
                          @RequestHeader("X-EWM-USER-ID") @Positive Long userId) {

        eventService.likeEvent(eventId, userId);
    }
}