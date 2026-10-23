package ru.practicum.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventRequestsCountDto;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.dto.user.UserFullDto;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.enums.event.EventState;
import ru.practicum.enums.request.RequestStatus;
import ru.practicum.feign.EventFeignClient;
import ru.practicum.feign.UserFeignClient;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;

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
    private EventFeignClient eventClient;

    @Mock
    private UserFeignClient userClient;

    @InjectMocks
    private RequestServiceImpl requestService;

    @Test
    void saveRequest_shouldSaveRequest() {
        Long userId = 1L;
        Long eventId = 2L;

        UserFullDto user = UserFullDto.builder().id(userId).build();

        UserShortDto initiator = UserShortDto.builder().id(99L).build();

        EventFullDto event = EventFullDto.builder()
                .id(eventId)
                .initiator(initiator)
                .state(EventState.PUBLISHED.toString())
                .participantLimit(0)
                .requestModeration(true)
                .build();

        Request savedRequest = Request.builder()
                .id(10L)
                .requesterId(user.getId())
                .eventId(event.getId())
                .createdAt(LocalDateTime.now())
                .status(RequestStatus.PENDING)
                .build();

        when(requestRepository.existsByRequesterIdAndEventId(userId, eventId))
                .thenReturn(false);
        when(userClient.findById(userId)).thenReturn(user);
        when(eventClient.findById(eventId)).thenReturn(event);
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

        verifyNoMoreInteractions(eventClient);
    }

    @Test
    void saveRequest_shouldThrowNotFound_whenEventNotExists() {
        when(requestRepository.existsByRequesterIdAndEventId(1L, 2L))
                .thenReturn(false);

        when(userClient.findById(1L))
                .thenReturn(UserFullDto.builder().id(1L).build());

        when(eventClient.findById(2L))
                .thenThrow(new NotFoundException("Event with id " + 2L + " not found"));

        assertThrows(
                NotFoundException.class,
                () -> requestService.saveRequest(1L, 2L)
        );
    }

    @Test
    void saveRequest_shouldThrowConflict_whenUserIsInitiator() {
        UserFullDto user = UserFullDto.builder().id(1L).build();

        EventFullDto event = EventFullDto.builder()
                .id(2L)
                .initiator(makeShort(user))
                .state(EventState.PUBLISHED.toString())
                .build();

        when(requestRepository.existsByRequesterIdAndEventId(1L, 2L))
                .thenReturn(false);
        when(userClient.findById(1L)).thenReturn(user);
        when(eventClient.findById(2L))
                .thenReturn(event);

        assertThrows(
                ConflictException.class,
                () -> requestService.saveRequest(1L, 2L)
        );
    }

    @Test
    void saveRequest_shouldThrowConflict_whenEventNotPublished() {
        UserFullDto user = UserFullDto.builder().id(1L).build();

        EventFullDto event = EventFullDto.builder()
                .id(2L)
                .initiator(new UserShortDto(99L, "initiator"))
                .state(EventState.CANCELED.toString())
                .build();

        when(requestRepository.existsByRequesterIdAndEventId(1L, 2L))
                .thenReturn(false);
        when(userClient.findById(1L)).thenReturn(user);
        when(eventClient.findById(2L))
                .thenReturn(event);

        assertThrows(
                ConflictException.class,
                () -> requestService.saveRequest(1L, 2L)
        );
    }

    @Test
    void saveRequest_shouldThrowConflict_whenParticipantLimitReached() {
        UserFullDto user = UserFullDto.builder().id(1L).build();

        EventFullDto event = EventFullDto.builder()
                .id(2L)
                .initiator(new UserShortDto(99L, "initiator"))
                .state(EventState.PUBLISHED.toString())
                .participantLimit(5)
                .build();

        when(requestRepository.existsByRequesterIdAndEventId(1L, 2L))
                .thenReturn(false);

        when(userClient.findById(1L))
                .thenReturn(user);

        when(eventClient.findById(2L))
                .thenReturn(event);

        when(requestRepository.countByEventIdAndStatus(
                2L,
                RequestStatus.CONFIRMED))
                .thenReturn(Optional.of(new EventRequestsCountDto(2L, 5L)));

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
                .eventId(2L)
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
                .eventId(2L)
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

    private UserShortDto makeShort(UserFullDto dto) {
        return UserShortDto.builder()
                .id(dto.getId())
                .name(dto.getName())
                .build();
    }
}