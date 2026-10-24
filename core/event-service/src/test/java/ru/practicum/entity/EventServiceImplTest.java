package ru.practicum.entity;

import com.querydsl.core.types.Predicate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import ru.practicum.entity.Event;
import ru.practicum.entity.EventRepository;
import ru.practicum.entity.EventServiceImpl;
import ru.practicum.entity.Location;
import ru.practicum.feign.CategoryFeignClient;
import ru.practicum.feign.RequestFeignClient;
import ru.practicum.StatClient;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.dto.event.*;
import ru.practicum.dto.user.UserFullDto;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.enums.event.AdminStateAction;
import ru.practicum.enums.event.EventState;
import ru.practicum.enums.request.RequestStatus;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.feign.UserFeignClient;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    @Mock
    private EventRepository eventRepository;
    @Mock
    private CategoryFeignClient categoryClient;
    @Mock
    private UserFeignClient userClient;
    @Mock
    private StatClient statClient;

    @Mock
    private RequestFeignClient requestClient;

    @InjectMocks
    private EventServiceImpl eventService;

    @Test
    void getEventsByAdmin_shouldReturnFilteredEvents() {
        EventAdminParamDto params = EventAdminParamDto.builder()
                .users(List.of(1L))
                .states(List.of("PUBLISHED"))
                .from(0)
                .size(10)
                .build();

        long categoryId = 1L;
        long initiatorId = 1L;

        Event event = Event.builder()
                .id(1L)
                .state(EventState.PUBLISHED)
                .createdOn(LocalDateTime.now())
                .eventDate(LocalDateTime.now().plusDays(1))
                .location(new Location(1.1f,1.1f))
                .categoryId(categoryId)
                .initiatorId(initiatorId)
                .build();

        when(eventRepository.findAll(any(Predicate.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(event)));
        when(statClient.getViewStats(any())).thenReturn(Collections.emptyList());

        List<EventFullDto> result = eventService.getEventsByAdmin(params);

        assertEquals(1, result.size());
        verify(statClient).getViewStats(any());
    }

    @Test
    void updateEventByAdmin_publishEvent_shouldSetPublished() {
        CategoryDto category = CategoryDto.builder().id(1L).name("cat").build();
        UserFullDto initiator = UserFullDto.builder().id(1L).name("user").build();

        Event event = Event.builder()
                .id(1L)
                .state(EventState.PENDING)
                .eventDate(LocalDateTime.now().plusDays(1))
                .createdOn(LocalDateTime.now())
                .location(new Location(1.1f,1.1f))
                .categoryId(category.getId())
                .initiatorId(initiator.getId())
                .build();

        UpdateEventAdminRequest request = UpdateEventAdminRequest.builder()
                .stateAction(AdminStateAction.PUBLISH_EVENT)
                .build();



        when(eventRepository.findWithRelationsById(1L)).thenReturn(Optional.of(event));
        when(statClient.getViewStats(any())).thenReturn(Collections.emptyList());
        when(userClient.findById(anyLong())).thenReturn(initiator);
        when(requestClient.countByEventIdAndStatus(event.getId(), RequestStatus.CONFIRMED))
                .thenReturn(new EventRequestsCountDto(event.getId(), 1L));
        when(categoryClient.findById(anyLong()))
                .thenReturn(category);

        EventFullDto result = eventService.updateEventByAdmin(1L, request);

        assertEquals("PUBLISHED", result.getState());
        assertNotNull(result.getPublishedOn());
    }

    @Test
    void updateEventByAdmin_publishEvent_wrongState_shouldThrowConflict() {
        long categoryId = 1L;
        long initiatorId = 1L;

        Event event = Event.builder()
                .id(1L)
                .state(EventState.PUBLISHED)
                .eventDate(LocalDateTime.now().plusDays(1))
                .categoryId(categoryId)
                .initiatorId(initiatorId)
                .build();

        UpdateEventAdminRequest request = UpdateEventAdminRequest.builder()
                .stateAction(AdminStateAction.PUBLISH_EVENT)
                .build();

        when(eventRepository.findWithRelationsById(1L)).thenReturn(Optional.of(event));

        assertThrows(ConflictException.class,
                () -> eventService.updateEventByAdmin(1L, request));
    }

    @Test
    void getPublishedEventById_notFound_shouldThrowNotFoundException() {
        when(eventRepository.findByIdAndState(999L, EventState.PUBLISHED))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> eventService.getPublishedEventById(999L, null));
    }

    @Test
    void create_shouldReturnCreatedEvent() {
        UserFullDto user = UserFullDto.builder().id(1L).name("user").build();
        CategoryDto categoryDto = CategoryDto.builder().id(1L).name("cat").build();

        String futureDate = LocalDateTime.now().plusDays(1)
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        NewEventDto newEvent = NewEventDto.builder()
                .annotation("annotation")
                .category(1L)
                .description("description")
                .eventDate(futureDate)
                .title("title")
                .location(new LocationDto(1.0f, 1.0f))
                .build();

        when(userClient.findById(1L)).thenReturn(user);
        when(categoryClient.findById(1L)).thenReturn(categoryDto);
        when(eventRepository.save(any())).thenAnswer(inv -> {
            Event e = inv.getArgument(0);
            e.setId(1L);
            return e;
        });

        EventFullDto result = eventService.create(1L, newEvent);

        assertEquals("title", result.getTitle());
        assertEquals("PENDING", result.getState());
        verify(eventRepository).save(any());
    }
}