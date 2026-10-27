package ru.practicum.service;

import lombok.experimental.UtilityClass;
import ru.practicum.dto.event.*;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.entity.Event;
import ru.practicum.enums.event.EventState;
import ru.practicum.exception.ValidationException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;


@UtilityClass
public class EventMapper {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static Event mapToEntity(NewEventDto eventDto, Long userId) {
        Event entity = new Event();

        entity.setTitle(eventDto.getTitle());
        entity.setAnnotation(eventDto.getAnnotation());
        entity.setCategoryId(eventDto.getCategory());

        LocalDateTime time;
        try {
            time = LocalDateTime.parse(eventDto.getEventDate(), FORMATTER);
        } catch (DateTimeParseException e) {
            throw new ValidationException("Invalid time format: " + eventDto.getEventDate());
        }

        if (time.isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ValidationException("eventDate must contain future date, but was: " + time.format(FORMATTER));
        }

        entity.setEventDate(time);
        entity.setDescription(eventDto.getDescription());
        entity.setParticipantLimit(eventDto.getParticipantLimit());
        entity.setCreatedOn(LocalDateTime.now());
        entity.setInitiatorId(userId);
        entity.setLocation(LocationMapper.mapToEntity(eventDto.getLocation()));
        entity.setPaid(eventDto.getPaid());
        entity.setState(EventState.PENDING);
        entity.setRequestModeration(eventDto.getRequestModeration());

        return entity;
    }

    public static EventFullDto toEventFullDto(Event event,
                                              Long confirmedRequests,
                                              Long views,
                                              String initiatorName,
                                              String categoryName) {
        return EventFullDto.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .category(new CategoryDto(event.getCategoryId(), categoryName))
                .confirmedRequests(confirmedRequests)
                .createdOn(event.getCreatedOn().format(FORMATTER))
                .description(event.getDescription())
                .eventDate(event.getEventDate().format(FORMATTER))
                .initiator(new UserShortDto(event.getInitiatorId(), initiatorName))
                .location(LocationMapper.mapToDto(event.getLocation()))
                .requestModeration(event.getRequestModeration())
                .paid(event.getPaid())
                .participantLimit(event.getParticipantLimit())
                .publishedOn(event.getPublishedOn() == null
                        ? null
                        : event.getPublishedOn().format(FORMATTER))
                .state(event.getState().name())
                .title(event.getTitle())
                .views(views)
                .build();
    }

    public static EventShortDto toEventShortDto(Event event,
                                                Long confirmedRequests,
                                                Long views,
                                                String initiatorName,
                                                String categoryName) {
        return EventShortDto.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .category(new CategoryDto(event.getCategoryId(), categoryName))
                .confirmedRequests(confirmedRequests)
                .eventDate(event.getEventDate().format(FORMATTER))
                .initiator(new UserShortDto(event.getInitiatorId(), initiatorName))
                .paid(event.getPaid())
                .title(event.getTitle())
                .views(views)
                .build();
    }

    public static List<EventShortDto> toEventShortDto(List<Event> events,
                                                      Map<Long, Long> confirmedRequests,
                                                      Map<Long, Long> views,
                                                      Map<Long, String> initiatorNames,
                                                      Map<Long, String> categoryNames) {
        return events.stream()
                .map(event ->
                        EventShortDto.builder()
                                .id(event.getId())
                                .annotation(event.getAnnotation())
                                .category(new CategoryDto(event.getCategoryId(), categoryNames.get(event.getCategoryId())))
                                .confirmedRequests(confirmedRequests.getOrDefault(event.getId(), 0L))
                                .eventDate(event.getEventDate().format(FORMATTER))
                                .initiator(new UserShortDto(event.getInitiatorId(), initiatorNames.get(event.getInitiatorId())))
                                .paid(event.getPaid())
                                .title(event.getTitle())
                                .views(views.getOrDefault(event.getId(), 0L))
                                .build()
                )
                .toList();
    }
}