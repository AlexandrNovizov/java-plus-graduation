package ru.practicum.entity;

import com.querydsl.core.types.Predicate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import ru.practicum.dto.category.CategoryDto;
import ru.practicum.dto.compilation.CompilationDto;
import ru.practicum.dto.compilation.CompilationsParamDto;
import ru.practicum.dto.compilation.NewCompilationDto;
import ru.practicum.dto.compilation.UpdateCompilationRequest;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.dto.event.LocationDto;
import ru.practicum.dto.user.UserDto;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.enums.event.EventState;
import ru.practicum.feign.EventFeignClient;
import ru.practicum.feign.UserFeignClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompilationServiceImplTest {

    @Mock
    private CompilationRepository compilationRepository;

    @Mock
    private EventFeignClient eventClient;

    @Mock
    private UserFeignClient userClient;

    @InjectMocks
    private CompilationServiceImpl compilationService;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private EventFullDto event;
    private Compilation compilation;
    private UserShortDto user;

    @BeforeEach
    void setUp() {
        user = UserShortDto.builder()
                .id(1L)
                .name("Roman")
                .build();

        CategoryDto category = CategoryDto.builder()
                .id(1L)
                .name("Category")
                .build();

        event = EventFullDto.builder()
                .id(1L)
                .annotation("annotation")
                .title("Event")
                .category(category)
                .initiator(user)
                .eventDate(LocalDateTime.of(2026, 1, 1, 12, 0).format(formatter))
                .createdOn(LocalDateTime.of(2025, 1, 1, 12, 0).format(formatter))
                .paid(false)
                .participantLimit(0)
                .requestModeration(true)
                .state(EventState.PUBLISHED.toString())
                .location(new LocationDto(10f, 20f))
                .confirmedRequests(3L)
                .views(20L)
                .build();

        compilation = Compilation.builder()
                .id(1L)
                .title("Compilation")
                .pinned(true)
                .eventIds(Set.of(event.getId()))
                .build();
    }

    @Test
    void getAll_shouldReturnCompilations() {
        CompilationsParamDto params = CompilationsParamDto.builder()
                .from(0)
                .size(10)
                .build();

        when(compilationRepository.findAll(Mockito.<Pageable>any()))
                .thenReturn(new PageImpl<>(List.of(compilation)));

        when(eventClient.getByIds(anySet()))
                .thenReturn(List.of(makeShort(event)));

        List<CompilationDto> result = compilationService.getAll(params);

        assertEquals(1, result.size());

        CompilationDto dto = result.getFirst();

        assertEquals(compilation.getId(), dto.getId());
        assertEquals(compilation.getTitle(), dto.getTitle());
        assertTrue(dto.getPinned());

        EventShortDto shortDto = dto.getEvents().iterator().next();

        assertEquals(event.getViews(), shortDto.getViews());
        assertEquals(event.getConfirmedRequests(), shortDto.getConfirmedRequests());

        verify(compilationRepository).findAll(Mockito.<Pageable>any());
        verify(eventClient).getByIds(anySet());
    }

    @Test
    void getAll_shouldReturnPinnedCompilations() {
        CompilationsParamDto params = CompilationsParamDto.builder()
                .from(0)
                .size(10)
                .pinned(true)
                .build();

        when(compilationRepository.findAll(
                Mockito.<Predicate>any(),
                Mockito.<Pageable>any()))
                .thenReturn(new PageImpl<>(List.of(compilation)));

        when(eventClient.getByIds(anySet()))
                .thenReturn(List.of(makeShort(event)));

        List<CompilationDto> result = compilationService.getAll(params);

        assertEquals(1, result.size());

        verify(compilationRepository)
                .findAll(
                        Mockito.<Predicate>any(),
                        Mockito.<Pageable>any()
                );

        verify(compilationRepository, never())
                .findAll(Mockito.<Pageable>any());
    }

    @Test
    void getAll_shouldReturnEmptyList() {
        CompilationsParamDto params = CompilationsParamDto.builder()
                .from(0)
                .size(10)
                .build();

        when(compilationRepository.findAll(Mockito.<Pageable>any()))
                .thenReturn(Page.empty());

        List<CompilationDto> result = compilationService.getAll(params);

        assertTrue(result.isEmpty());

        verifyNoInteractions(eventClient);
    }

    @Test
    void getById_shouldReturnCompilation() {
        when(compilationRepository.findWithEventsById(1L))
                .thenReturn(Optional.of(compilation));

        when(eventClient.getByIds(anySet()))
                .thenReturn(List.of(makeShort(event)));

        CompilationDto dto = compilationService.getById(1L);

        assertEquals(compilation.getId(), dto.getId());

        EventShortDto shortDto = dto.getEvents().iterator().next();

        assertEquals(event.getViews(), shortDto.getViews());
        assertEquals(event.getConfirmedRequests(), shortDto.getConfirmedRequests());

        verify(compilationRepository).findWithEventsById(1L);
    }

    @Test
    void getById_shouldThrowNotFound() {
        when(compilationRepository.findWithEventsById(1L))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> compilationService.getById(1L));

        verifyNoInteractions(eventClient);
    }

    @Test
    void create_shouldSaveCompilation() {
        NewCompilationDto dto = new NewCompilationDto();
        dto.setTitle("Compilation");
        dto.setEvents(Set.of(1L, 2L));

        EventShortDto event1 = EventShortDto.builder()
                .id(1L)
                .initiator(new UserShortDto(1L, "initiator 1"))
                .category(CategoryDto.builder()
                        .id(1L)
                        .name("Category1")
                        .build())
                .eventDate(LocalDateTime.of(2026, 1, 1, 12, 0).format(formatter))
                .build();
        EventShortDto event2 = EventShortDto.builder()
                .id(2L)
                .initiator(new UserShortDto(2L, "initiator 2"))
                .category(CategoryDto.builder()
                        .id(2L)
                        .name("Category2")
                        .build())
                .eventDate(LocalDateTime.of(2026, 1, 1, 12, 0).format(formatter))
                .build();

        when(compilationRepository.existsByTitle("Compilation"))
                .thenReturn(false);

        when(eventClient.getByIds(dto.getEvents()))
                .thenReturn(List.of(event1, event2));

        when(compilationRepository.save(Mockito.any(Compilation.class)))
                .thenAnswer(invocation -> {
                    Compilation c = invocation.getArgument(0);
                    c.setId(100L);
                    return c;
                });

        CompilationDto result = compilationService.create(dto);

        assertEquals(100L, result.getId());
        assertEquals("Compilation", result.getTitle());

        assertEquals(2, result.getEvents().size());

        verify(compilationRepository).existsByTitle("Compilation");
        verify(eventClient).getByIds(dto.getEvents());
        verify(compilationRepository).save(Mockito.<Compilation>any());
    }

    @Test
    void create_shouldThrowConflictWhenTitleExists() {

        NewCompilationDto dto = new NewCompilationDto();
        dto.setTitle("Compilation");

        when(compilationRepository.existsByTitle("Compilation"))
                .thenReturn(true);

        assertThrows(ConflictException.class,
                () -> compilationService.create(dto));

        verify(compilationRepository, never()).save(any());
        verifyNoInteractions(eventClient);
    }

    @Test
    void delete_shouldDeleteCompilation() {

        Compilation compilation = new Compilation();
        compilation.setId(1L);

        when(compilationRepository.findById(1L))
                .thenReturn(Optional.of(compilation));

        compilationService.delete(1L);

        verify(compilationRepository).delete(compilation);
    }

    @Test
    void delete_shouldThrowNotFound() {

        when(compilationRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> compilationService.delete(1L));

        verify(compilationRepository, never()).delete(any());
    }

    @Test
    void update_shouldChangeTitleAndPinned() {
        UpdateCompilationRequest request = new UpdateCompilationRequest();
        request.setTitle("New title");
        request.setPinned(false);

        when(compilationRepository.findWithEventsById(1L))
                .thenReturn(Optional.of(compilation));

        when(eventClient.getByIds(anySet()))
                .thenReturn(List.of(makeShort(event)));

        CompilationDto result = compilationService.update(1L, request);

        assertEquals("New title", result.getTitle());
        assertFalse(result.getPinned());

        verify(compilationRepository).findWithEventsById(1L);
    }

    @Test
    void update_shouldChangeOnlyPinned() {
        UpdateCompilationRequest request = new UpdateCompilationRequest();
        request.setPinned(false);

        when(compilationRepository.findWithEventsById(1L))
                .thenReturn(Optional.of(compilation));

        when(eventClient.getByIds(anySet()))
                .thenReturn(List.of());

        CompilationDto result = compilationService.update(1L, request);

        assertFalse(result.getPinned());
        assertEquals("Compilation", result.getTitle());
    }

    @Test
    void update_shouldUpdateEvents() {
        UpdateCompilationRequest request = new UpdateCompilationRequest();
        request.setEvents(Set.of(2L, 3L));

        CategoryDto category = CategoryDto.builder().id(1L).name("Category").build();
        UserDto user = UserDto.builder().id(1L).name("User").build();

        EventShortDto event2 = EventShortDto.builder()
                .id(2L)
                .category(category)
                .initiator(makeShort(user))
                .annotation("a")
                .title("t")
                .eventDate(LocalDateTime.of(2026, 1, 1, 10, 0).format(formatter))
                .build();

        EventShortDto event3 = EventShortDto.builder()
                .id(3L)
                .category(category)
                .initiator(makeShort(user))
                .annotation("a")
                .title("t")
                .eventDate(LocalDateTime.of(2026, 1, 1, 10, 0).format(formatter))
                .build();

        when(compilationRepository.findWithEventsById(1L))
                .thenReturn(Optional.of(compilation));

        when(eventClient.getByIds(request.getEvents()))
                .thenReturn(List.of(event2, event3));

        CompilationDto result = compilationService.update(1L, request);

        assertEquals(2, result.getEvents().size());

        verify(eventClient).getByIds(request.getEvents());
    }

    @Test
    void update_shouldThrowNotFound() {

        when(compilationRepository.findWithEventsById(1L))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> compilationService.update(1L, new UpdateCompilationRequest()));

        verifyNoInteractions(eventClient);
    }

    @Test
    void update_shouldThrowConflictWhenTitleExists() {

        UpdateCompilationRequest request = new UpdateCompilationRequest();
        request.setTitle("New title");

        Compilation other = Compilation.builder()
                .id(2L)
                .title("New title")
                .build();

        when(compilationRepository.findWithEventsById(1L))
                .thenReturn(Optional.of(compilation));

        when(compilationRepository.existsByTitle("New title"))
                .thenReturn(true);

        assertThrows(ConflictException.class,
                () -> compilationService.update(1L, request));
    }

    private static UserShortDto makeShort(UserDto dto) {
        return UserShortDto.builder()
                .id(dto.getId())
                .name(dto.getName())
                .build();
    }

    private static EventShortDto makeShort(EventFullDto dto) {
        return EventShortDto.builder()
                .id(dto.getId())
                .title(dto.getTitle())
                .annotation(dto.getAnnotation())
                .category(dto.getCategory())
                .initiator(dto.getInitiator())
                .eventDate(dto.getEventDate())
                .views(dto.getViews())
                .confirmedRequests(dto.getConfirmedRequests())
                .paid(dto.getPaid())
                .build();
    }
}