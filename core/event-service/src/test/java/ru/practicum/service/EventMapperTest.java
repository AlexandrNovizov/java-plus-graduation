package ru.practicum.service;

import org.junit.jupiter.api.Test;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.dto.user.UserDto;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.entity.Event;
import ru.practicum.entity.Location;
import ru.practicum.enums.event.EventState;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class EventMapperTest {

    @Test
    void toEventFullDto_shouldMapAllFields() {
        CategoryDto category = CategoryDto.builder().id(1L).name("test").build();
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
                .categoryId(category.getId())
                .initiatorId(initiator.getId())
                .build();

        EventFullDto dto = EventMapper.toEventFullDto(
                event, 5L, 0.86, initiator.getName(), category.getName()
        );

        assertEquals(1L, dto.getId());
        assertEquals("annotation", dto.getAnnotation());
        assertEquals("description", dto.getDescription());
        assertEquals(5L, dto.getConfirmedRequests());
        assertEquals(0.86, dto.getRating());
        assertTrue(dto.getPaid());
        assertEquals("PUBLISHED", dto.getState());
    }

    @Test
    void toEventShortDto_shouldMapOnlyShortFields() {
        CategoryDto category = CategoryDto.builder().id(1L).name("test").build();
        UserShortDto initiator = UserShortDto.builder().id(10L).name("initiator").build();

        Event event = Event.builder()
                .id(1L)
                .annotation("annotation")
                .eventDate(LocalDateTime.of(2026, 6, 25, 12, 0))
                .paid(false)
                .title("title")
                .categoryId(category.getId())
                .initiatorId(initiator.getId())
                .build();

        EventShortDto dto = EventMapper.toEventShortDto(
                event, 3L, 0.86, initiator.getName(), category.getName()
        );

        assertEquals(1L, dto.getId());
        assertEquals("annotation", dto.getAnnotation());
        assertEquals(3L, dto.getConfirmedRequests());
        assertEquals(0.86, dto.getRating());
        assertFalse(dto.getPaid());
        assertEquals("title", dto.getTitle());
    }
}