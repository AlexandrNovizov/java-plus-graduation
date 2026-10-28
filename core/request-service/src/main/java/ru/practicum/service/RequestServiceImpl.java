package ru.practicum.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventRequestsCountDto;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.dto.user.UserFullDto;
import ru.practicum.entity.Request;
import ru.practicum.enums.event.EventState;
import ru.practicum.enums.request.RequestStatus;
import ru.practicum.feign.EventFeignClient;
import ru.practicum.feign.UserFeignClient;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.repository.RequestRepository;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RequestServiceImpl implements RequestService {

    private final RequestRepository requestRepository;
    private final EventFeignClient eventClient;
    private final UserFeignClient userClient;

    @Override
    @Transactional
    public ParticipationRequestDto saveRequest(Long userId, Long eventId) {
        if (existsByUserIdAndEventId(userId, eventId)) {
            throw new ConflictException("Attempt to send a duplicate request.");
        }

        UserFullDto user = userClient.findById(userId);
        EventFullDto event = getEvent(eventId);
        checkUserRequestForEvent(event, userId);

        Request request = RequestMapper.toRequest(event, user.getId());
        request = requestRepository.save(request);

        var requestDto = RequestMapper.toRequestDto(request);
        log.info("Новая заявка на событие: {}", requestDto);

        return requestDto;
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        Request request = getRequest(requestId);

        if (!request.getRequesterId().equals(userId)) {
            throw new ConflictException("You can only cancel your own participation requests.");
        }

        request.setStatus(RequestStatus.CANCELED);

        var requestDto = RequestMapper.toRequestDto(request);
        log.info("Заявка на событие отменена: {}", requestDto);

        return requestDto;
    }

    @Override
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        checkUserExist(userId);

        return RequestMapper.toRequestDto(requestRepository.findByRequesterId(userId));
    }

    @Override
    public List<EventRequestsCountDto> countByEventIdsAndStatus(Set<Long> ids, RequestStatus status) {
        return requestRepository.countByEventIdInAndStatus(ids, status);
    }

    @Override
    public EventRequestsCountDto countByEventIdAndStatus(Long eventId, RequestStatus status) {
        Optional<EventRequestsCountDto> countDto = requestRepository.countByEventIdAndStatus(eventId, status);
        return countDto.orElseGet(() -> new EventRequestsCountDto(eventId, 0L));
    }

    @Override
    public List<ParticipationRequestDto> findByIdsAndEventId(Set<Long> ids, Long eventId) {
        return requestRepository.findByIdInAndEventId(ids, eventId).stream()
                .map(RequestMapper::toRequestDto)
                .toList();
    }

    @Override
    public List<ParticipationRequestDto> getParticipationRequestsByEventId(Long eventId) {
        return RequestMapper.toRequestDto(requestRepository.findByEventId(eventId));
    }


    @Override
    @Transactional
    public List<ParticipationRequestDto> updateRequestsStatus(Set<Long> ids, RequestStatus status) {

        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }

        List<Request> requests = requestRepository.findAllByIdIn(ids);

        Set<Long> receivedIds = requests.stream()
                .map(Request::getId)
                .collect(Collectors.toSet());

        List<Long> missedIds = ids.stream()
                .filter(id -> !receivedIds.contains(id))
                .toList();

        if (!missedIds.isEmpty()) {
            throw new NotFoundException("Не найдены запросы с id=" + missedIds);
        }

        requests = requests.stream()
                .peek(request -> request.setStatus(status))
                .toList();

        requests = requestRepository.saveAll(requests);

        log.info("Обновлены заявки: {}", requests);

        return requests.stream()
                .map(RequestMapper::toRequestDto)
                .toList();
    }

    private boolean existsByUserIdAndEventId(Long userId, Long eventId) {
        return requestRepository.existsByRequesterIdAndEventId(userId, eventId);
    }

    private EventFullDto getEvent(Long eventId) {
        try {
           return eventClient.findById(eventId);
        } catch (FeignException.NotFound e) {
            throw new NotFoundException("Event with id " + eventId + " not found.");
        }
    }

    private Request getRequest(Long requestId) {
        return requestRepository.findById(requestId).orElseThrow(
                () -> new NotFoundException("Request with id " + requestId + " not found.")
        );
    }

    private void checkUserRequestForEvent(EventFullDto event, Long userId) {
        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("The initiator can't request to join their own event.");
        }
        if (!event.getState().equals(EventState.PUBLISHED.toString())) {
            throw new ConflictException("The Participation in unpublished events is forbidden.");
        }

        if (event.getParticipantLimit() != 0) {
            EventRequestsCountDto countDto = countByEventIdAndStatus(event.getId(), RequestStatus.CONFIRMED);
            long requests = countDto.count();
            if (requests >= event.getParticipantLimit()) {
                throw new ConflictException("The participant limit has been reached.");
            }
        }
    }

    private void checkUserExist(Long userId) {
        try {
            userClient.checkUserExists(userId);
        } catch (FeignException.NotFound e) {
            throw new NotFoundException("User with id: " + userId + " does not exist");
        }
    }
}
