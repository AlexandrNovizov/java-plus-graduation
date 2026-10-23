package ru.practicum.feign;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.event.EventRequestsCountDto;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.enums.request.RequestStatus;

import java.util.List;
import java.util.Set;

@FeignClient(name = "request-service")
public interface RequestFeignClient {

    @GetMapping("/internal/requests/events/{eventId}")
    List<ParticipationRequestDto> getParticipationRequestsByEventId(@PathVariable Long eventId);

    @GetMapping("/internal/requests/events/{eventId}")
    List<ParticipationRequestDto> findByIdsAndEventId(
            @RequestParam(name = "ids") @NotEmpty Set<Long> requestIds,
            @PathVariable Long eventId
    );

    @GetMapping("/internal/requests/events/{eventId}/count")
    EventRequestsCountDto countByEventIdAndStatus(
            @PathVariable Long eventId,
            @RequestParam RequestStatus status
    );

    @GetMapping("/internal/requests/events/count")
    List<EventRequestsCountDto> countByEventIdsAndStatus(
            @RequestParam(name = "ids") @NotEmpty Set<Long> requestIds,
            @RequestParam RequestStatus status
    );

    @PutMapping("/internal/requests")
    List<ParticipationRequestDto> updateRequestsStatus(
            @RequestParam(name = "ids") Set<Long> requestIds,
            @RequestParam RequestStatus status
    );

}
