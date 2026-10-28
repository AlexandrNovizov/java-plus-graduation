package ru.practicum.dto.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.dto.event.*;


@UtilityClass
public class EventMapper {

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