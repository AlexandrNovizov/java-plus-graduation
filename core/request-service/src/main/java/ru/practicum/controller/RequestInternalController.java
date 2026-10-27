package ru.practicum.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.event.EventRequestsCountDto;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.enums.request.RequestStatus;
import ru.practicum.service.RequestService;

import java.util.List;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/internal/requests")
public class RequestInternalController {

    private final RequestService requestService;

    @GetMapping("/events/{eventId}")
    @ResponseStatus(HttpStatus.OK)
    public List<ParticipationRequestDto> getParticipationRequestsByEventId(@PathVariable Long eventId,
                                                                           @RequestParam(required = false) Set<Long> ids) {

        if (ids == null || ids.isEmpty()) {
            return requestService.getParticipationRequestsByEventId(eventId);
        }
        return requestService.findByIdsAndEventId(ids, eventId);
    }

    @GetMapping("/events/{eventId}/count")
    @ResponseStatus(HttpStatus.OK)
    public EventRequestsCountDto countByEventIdAndStatus(@PathVariable Long eventId,
                                                         @RequestParam RequestStatus status) {

        return requestService.countByEventIdAndStatus(eventId, status);
    }

    @GetMapping("/events/count")
    @ResponseStatus(HttpStatus.OK)
    public List<EventRequestsCountDto> countByEventIdsAndStatus(@RequestParam RequestStatus status,
                                                                @RequestParam Set<Long> ids) {

        return requestService.countByEventIdsAndStatus(ids, status);
    }

    @PutMapping
    @ResponseStatus(HttpStatus.OK)
    public List<ParticipationRequestDto> updateRequests(@RequestParam RequestStatus status,
                                                        @RequestParam Set<Long> ids) {

        return requestService.updateRequestsStatus(ids, status);
    }
}
