package ru.practicum.entity.event;

import lombok.experimental.UtilityClass;
import ru.practicum.dto.event.*;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.entity.category.Category;
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

    public static Event mapToEntity(NewEventDto eventDto, Category category, Long userId) {
        Event entity = new Event();

        entity.setTitle(eventDto.getTitle());
        entity.setAnnotation(eventDto.getAnnotation());
        entity.setCategory(category);

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

    public static EventFullDto toEventFullDto(Event event, Long confirmedRequests, Long views, String authorName) {
        return EventFullDto.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .category(new CategoryDto(event.getCategory().getId(), event.getCategory().getName()))
                .confirmedRequests(confirmedRequests)
                .createdOn(event.getCreatedOn().format(FORMATTER))
                .description(event.getDescription())
                .eventDate(event.getEventDate().format(FORMATTER))
                .initiator(new UserShortDto(event.getInitiatorId(), authorName))
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

    public static EventShortDto toEventShortDto(Event event, Long confirmedRequests, Long views, String initiatorName) {
        return EventShortDto.builder()
                .id(event.getId())
                .annotation(event.getAnnotation())
                .category(new CategoryDto(event.getCategory().getId(), event.getCategory().getName()))
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
                                                      Map<Long, String> initiatorNames) {
        return events.stream()
                .map(event ->
                        EventShortDto.builder()
                                .id(event.getId())
                                .annotation(event.getAnnotation())
                                .category(new CategoryDto(event.getCategory().getId(), event.getCategory().getName()))
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

    public static LogEvent toLogEvent(EventFullDto eventFullDto) {
        return LogEvent.builder()
                .id(eventFullDto.getId())
                .annotation(stringBuilder(eventFullDto.getAnnotation()))
                .category(eventFullDto.getCategory())
                .confirmedRequests(eventFullDto.getConfirmedRequests())
                .createdOn(eventFullDto.getCreatedOn())
                .description(stringBuilder(eventFullDto.getDescription()))
                .eventDate(eventFullDto.getEventDate())
                .initiator(eventFullDto.getInitiator())
                .locationDto(eventFullDto.getLocation())
                .requestModeration(eventFullDto.getRequestModeration())
                .participantLimit(eventFullDto.getParticipantLimit())
                .publishedOn(eventFullDto.getPublishedOn())
                .state(eventFullDto.getState())
                .paid(eventFullDto.getPaid())
                .title(stringBuilder(eventFullDto.getTitle()))
                .views(eventFullDto.getViews())
                .build();
    }

    public static LogEventShort toLogEventShort(EventShortDto eventShortDto) {
        return LogEventShort.builder()
                .id(eventShortDto.getId())
                .annotation(stringBuilder(eventShortDto.getAnnotation()))
                .category(eventShortDto.getCategory())
                .confirmedRequests(eventShortDto.getConfirmedRequests())
                .eventDate(eventShortDto.getEventDate())
                .initiator(eventShortDto.getInitiator())
                .paid(eventShortDto.getPaid())
                .title(stringBuilder(eventShortDto.getTitle()))
                .views(eventShortDto.getViews())
                .build();
    }

    private static String stringBuilder(String string) {
        if (string == null) {
            return null;
        }
        return string.length() > 10 ? string.substring(0, 10) + "..." : string;
    }
}