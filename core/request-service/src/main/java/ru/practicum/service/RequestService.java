package ru.practicum.service;

import ru.practicum.dto.event.EventRequestsCountDto;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.enums.request.RequestStatus;

import java.util.List;
import java.util.Set;

public interface RequestService {

    ParticipationRequestDto saveRequest(Long userId, Long eventId);


    List<ParticipationRequestDto> updateRequestsStatus(Set<Long> ids, RequestStatus status);

    ParticipationRequestDto cancelRequest(Long userId, Long requestId);

    List<ParticipationRequestDto> getUserRequests(Long userId);

    List<EventRequestsCountDto> countByEventIdsAndStatus(Set<Long> ids, RequestStatus status);

    EventRequestsCountDto countByEventIdAndStatus(Long eventId, RequestStatus status);

    List<ParticipationRequestDto> findByIdsAndEventId(Set<Long> ids, Long eventId);

    List<ParticipationRequestDto> getParticipationRequestsByEventId(Long eventId);
}
