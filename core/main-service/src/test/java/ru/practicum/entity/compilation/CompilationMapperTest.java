package ru.practicum.entity.compilation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.practicum.dto.category.CategoryDto;
import ru.practicum.dto.compilation.CompilationDto;
import ru.practicum.dto.compilation.LogCompilation;
import ru.practicum.dto.compilation.NewCompilationDto;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.dto.user.UserShortDto;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;


import static org.junit.jupiter.api.Assertions.*;


class CompilationMapperTest {

    private EventShortDto event;
    private Compilation compilation;
    private final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @BeforeEach
    void setUp() {
        CategoryDto category = CategoryDto.builder()
                .id(1L)
                .name("Category")
                .build();

        UserShortDto initiator = UserShortDto.builder()
                .id(1L)
                .name("Roman")
                .build();

        event = EventShortDto.builder()
                .id(1L)
                .title("Title")
                .annotation("Annotation")
                .category(category)
                .initiator(initiator)
                .eventDate(LocalDateTime.now().format(FORMATTER))
                .paid(false)
                .views(15L)
                .confirmedRequests(7L)
                .build();

        compilation = new Compilation();
        compilation.setId(10L);
        compilation.setTitle("Compilation");
        compilation.setPinned(true);
        compilation.setEventIds(Set.of(event.getId()));
    }

    @Test
    void toDto_shouldMapCompilation() {

        Compilation compilation = new Compilation();
        compilation.setId(10L);
        compilation.setTitle("Compilation");
        compilation.setPinned(true);
        compilation.setEventIds(Set.of(event.getId()));

        CompilationDto dto = CompilationMapper.toDto(compilation, Set.of(event));

        assertEquals(10L, dto.getId());
        assertEquals("Compilation", dto.getTitle());
        assertTrue(dto.getPinned());

        assertEquals(1, dto.getEvents().size());

        EventShortDto eventDto = dto.getEvents().iterator().next();
        assertEquals(event.getViews(), eventDto.getViews());
        assertEquals(event.getConfirmedRequests(), eventDto.getConfirmedRequests());
    }

    @Test
    void toLog_shouldMapCompilationDto() {
        EventShortDto event = EventShortDto.builder()
                .id(1L)
                .build();

        CompilationDto dto = CompilationDto.builder()
                .id(5L)
                .title("Test")
                .pinned(false)
                .events(Set.of(event))
                .build();

        LogCompilation log = CompilationMapper.toLog(dto);

        assertEquals(5L, log.getId());
        assertEquals("Test", log.getTitle());
        assertFalse(log.getPinned());

        assertEquals(1, log.getEvents().size());
        assertEquals(1L, log.getEvents().iterator().next().getId());
    }

    @Test
    void toEntity_shouldMapAllFields() {

        Set<Long> eventIds = Set.of(1L);

        NewCompilationDto dto = new NewCompilationDto();
        dto.setTitle("Compilation");
        dto.setPinned(true);
        dto.setEvents(eventIds);

        Compilation entity = CompilationMapper.toEntity(dto);

        assertEquals("Compilation", entity.getTitle());
        assertTrue(entity.getPinned());
        assertEquals(eventIds, entity.getEventIds());
    }

    @Test
    void toEntity_shouldHandleNullFields() {
        NewCompilationDto dto = new NewCompilationDto();
        dto.setTitle("Compilation");

        Compilation entity = CompilationMapper.toEntity(dto);

        assertEquals("Compilation", entity.getTitle());
        assertFalse(entity.getPinned());
        assertTrue(entity.getEventIds().isEmpty());
    }

}