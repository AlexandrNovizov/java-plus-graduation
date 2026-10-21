package ru.practicum.entity.request;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.dto.user.UserFullDto;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.entity.event.Event;
import ru.practicum.entity.event.EventRepository;
import ru.practicum.enums.event.EventState;
import ru.practicum.enums.request.RequestStatus;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.feign.UserFeignClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequestServiceImplTest {

    @Mock
    private RequestRepository requestRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserFeignClient userClient;

    @InjectMocks
    private RequestServiceImpl requestService;

    @Test
    void saveRequest_shouldSaveRequest() {
        Long userId = 1L;
        Long eventId = 2L;

        UserFullDto user = UserFullDto.builder().id(userId).build();

        UserFullDto initiator = UserFullDto.builder().id(99L).build();

        Event event = Event.builder()
                .id(eventId)
                .initiatorId(initiator.getId())
                .state(EventState.PUBLISHED)
                .participantLimit(0)
                .requestModeration(true)
                .build();

        Request savedRequest = Request.builder()
                .id(10L)
                .requesterId(user.getId())
                .event(event)
                .createdAt(LocalDateTime.now())
                .status(RequestStatus.PENDING)
                .build();

        when(requestRepository.existsByRequesterIdAndEventId(userId, eventId))
                .thenReturn(false);
        when(userClient.findById(userId)).thenReturn(user);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(requestRepository.save(any(Request.class))).thenReturn(savedRequest);

        ParticipationRequestDto result =
                requestService.saveRequest(userId, eventId);

        assertEquals(10L, result.getId());

        verify(requestRepository).save(any(Request.class));
    }

    @Test
    void saveRequest_shouldThrowConflict_whenDuplicateRequest() {
        when(requestRepository.existsByRequesterIdAndEventId(1L, 2L))
                .thenReturn(true);

        assertThrows(
                ConflictException.class,
                () -> requestService.saveRequest(1L, 2L)
        );

        verifyNoMoreInteractions(eventRepository);
    }

    @Test
    void saveRequest_shouldThrowNotFound_whenEventNotExists() {
        when(requestRepository.existsByRequesterIdAndEventId(1L, 2L))
                .thenReturn(false);

        when(userClient.findById(1L))
                .thenReturn(UserFullDto.builder().id(1L).build());

        when(eventRepository.findById(2L))
                .thenReturn(Optional.empty());

        assertThrows(
                NotFoundException.class,
                () -> requestService.saveRequest(1L, 2L)
        );
    }

    @Test
    void saveRequest_shouldThrowConflict_whenUserIsInitiator() {
        UserFullDto user = UserFullDto.builder().id(1L).build();

        Event event = Event.builder()
                .id(2L)
                .initiatorId(user.getId())
                .state(EventState.PUBLISHED)
                .build();

        when(requestRepository.existsByRequesterIdAndEventId(1L, 2L))
                .thenReturn(false);
        when(userClient.findById(1L)).thenReturn(user);
        when(eventRepository.findById(2L))
                .thenReturn(Optional.of(event));

        assertThrows(
                ConflictException.class,
                () -> requestService.saveRequest(1L, 2L)
        );
    }

    @Test
    void saveRequest_shouldThrowConflict_whenEventNotPublished() {
        UserFullDto user = UserFullDto.builder().id(1L).build();

        Event event = Event.builder()
                .id(2L)
                .initiatorId(99L)
                .state(EventState.CANCELED)
                .build();

        when(requestRepository.existsByRequesterIdAndEventId(1L, 2L))
                .thenReturn(false);
        when(userClient.findById(1L)).thenReturn(user);
        when(eventRepository.findById(2L))
                .thenReturn(Optional.of(event));

        assertThrows(
                ConflictException.class,
                () -> requestService.saveRequest(1L, 2L)
        );
    }

    @Test
    void saveRequest_shouldThrowConflict_whenParticipantLimitReached() {
        UserFullDto user = UserFullDto.builder().id(1L).build();

        Event event = Event.builder()
                .id(2L)
                .initiatorId(99L)
                .state(EventState.PUBLISHED)
                .participantLimit(5)
                .build();

        when(requestRepository.existsByRequesterIdAndEventId(1L, 2L))
                .thenReturn(false);

        when(userClient.findById(1L))
                .thenReturn(user);

        when(eventRepository.findById(2L))
                .thenReturn(Optional.of(event));

        when(requestRepository.countByEventIdAndStatus(
                2L,
                RequestStatus.CONFIRMED))
                .thenReturn(5L);

        assertThrows(
                ConflictException.class,
                () -> requestService.saveRequest(1L, 2L)
        );
    }

    @Test
    void cancelRequest_shouldCancelRequest() {
        UserShortDto user = UserShortDto.builder().id(1L).build();

        Request request = Request.builder()
                .id(10L)
                .requesterId(user.getId())
                .status(RequestStatus.CONFIRMED)
                .event(Event.builder().id(2L).build())
                .createdAt(LocalDateTime.now())
                .build();

        when(requestRepository.findById(10L))
                .thenReturn(Optional.of(request));

        ParticipationRequestDto result =
                requestService.cancelRequest(1L, 10L);

        assertEquals("CANCELED", result.getStatus());
        assertEquals(RequestStatus.CANCELED, request.getStatus());
    }

    @Test
    void cancelRequest_shouldThrowConflict_whenRequestBelongsToAnotherUser() {
        UserShortDto owner = UserShortDto.builder().id(2L).build();

        Request request = Request.builder()
                .id(10L)
                .requesterId(owner.getId())
                .build();

        when(requestRepository.findById(10L))
                .thenReturn(Optional.of(request));

        assertThrows(
                ConflictException.class,
                () -> requestService.cancelRequest(1L, 10L)
        );
    }

    @Test
    void getUserRequests_shouldReturnRequests() {
        Request request = Request.builder()
                .id(1L)
                .requesterId(1L)
                .event(Event.builder().id(2L).build())
                .createdAt(LocalDateTime.now())
                .status(RequestStatus.CONFIRMED)
                .build();

        when(requestRepository.findByRequesterId(1L))
                .thenReturn(List.of(request));

        List<ParticipationRequestDto> result =
                requestService.getUserRequests(1L);

        assertEquals(1, result.size());

        verify(userClient).checkUserExists(1L);
    }
}