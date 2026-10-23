package ru.practicum.entity;

import lombok.experimental.UtilityClass;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.enums.request.RequestStatus;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@UtilityClass
public class RequestMapper {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static Request toRequest(EventFullDto event, Long requesterId) {
        return Request.builder()
                .eventId(event.getId())
                .requesterId(requesterId)
                .createdAt(LocalDateTime.now())
                .status(isStatusConfirmed(event) ? RequestStatus.CONFIRMED : RequestStatus.PENDING)
                .build();
    }

    public static ParticipationRequestDto toRequestDto(Request request) {
        return ParticipationRequestDto.builder()
                .id(request.getId())
                .created(request.getCreatedAt().format(formatter))
                .eventId(request.getEventId())
                .requesterId(request.getRequesterId())
                .status(request.getStatus().name())
                .build();
    }

    public static List<ParticipationRequestDto> toRequestDto(List<Request> requests) {
        return requests.stream()
                .map(RequestMapper::toRequestDto)
                .toList();
    }

    private boolean isStatusConfirmed(EventFullDto event) {
        return !event.getRequestModeration() || event.getParticipantLimit() == 0;
    }
}
