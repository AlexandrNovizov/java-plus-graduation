package ru.practicum.entity.event;

import org.junit.jupiter.api.Test;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.dto.user.UserDto;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.entity.category.Category;
import ru.practicum.enums.event.EventState;

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EventMapperTest {

    @Test
    void toEventFullDto_shouldMapAllFields() {
        Category category = Category.builder().id(1L).name("test").build();
        UserDto initiator = UserDto.builder().id(10L).name("initiator").build();

        Event event = Event.builder()
                .id(1L)
                .annotation("annotation")
                .description("description")
                .eventDate(LocalDateTime.of(2026, 6, 25, 12, 0))
                .createdOn(LocalDateTime.of(2026, 6, 24, 12, 0))
                .publishedOn(LocalDateTime.of(2026, 6, 24, 13, 0))
                .paid(true)
                .location(new Location(1.1f,1.1f))
                .participantLimit(10)
                .requestModeration(true)
                .state(EventState.PUBLISHED)
                .title("title")
                .category(category)
                .initiatorId(initiator.getId())
                .build();

        EventFullDto dto = EventMapper.toEventFullDto(event, 5L, 100L, initiator.getName());

        assertEquals(1L, dto.getId());
        assertEquals("annotation", dto.getAnnotation());
        assertEquals("description", dto.getDescription());
        assertEquals(5L, dto.getConfirmedRequests());
        assertEquals(100L, dto.getViews());
        assertTrue(dto.getPaid());
        assertEquals("PUBLISHED", dto.getState());
    }

    @Test
    void toEventShortDto_shouldMapOnlyShortFields() {
        Category category = Category.builder().id(1L).name("test").build();
        UserShortDto initiator = UserShortDto.builder().id(10L).name("initiator").build();

        Event event = Event.builder()
                .id(1L)
                .annotation("annotation")
                .eventDate(LocalDateTime.of(2026, 6, 25, 12, 0))
                .paid(false)
                .title("title")
                .category(category)
                .initiatorId(initiator.getId())
                .build();

        EventShortDto dto = EventMapper.toEventShortDto(event, 3L, 50L, initiator.getName());

        assertEquals(1L, dto.getId());
        assertEquals("annotation", dto.getAnnotation());
        assertEquals(3L, dto.getConfirmedRequests());
        assertEquals(50L, dto.getViews());
        assertFalse(dto.getPaid());
        assertEquals("title", dto.getTitle());
    }
}