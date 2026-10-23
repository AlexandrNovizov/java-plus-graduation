package ru.practicum.feign.fallback;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import ru.practicum.dto.event.EventRequestsCountDto;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.enums.request.RequestStatus;
import ru.practicum.feign.RequestFeignClient;
import ru.practicum.feign.exception.RequestServiceUnavailableException;

import java.util.List;
import java.util.Set;

@Slf4j
public class RequestClientFallbackFactory implements FallbackFactory<RequestFeignClient> {

    @Override
    public RequestFeignClient create(Throwable cause) {
        return new RequestFeignClient() {
            @Override
            public List<ParticipationRequestDto> getParticipationRequestsByEventId(Long eventId) {
                return List.of();
            }

            @Override
            public List<ParticipationRequestDto> findByIdsAndEventId(Set<Long> requestIds, Long eventId) {
                return List.of();
            }

            @Override
            public EventRequestsCountDto countByEventIdAndStatus(Long eventId, RequestStatus status) {
                log.warn("request-service unavailable while requesting requests count with event id={} and status = {}"
                        , eventId, status.toString(), cause);
                return new EventRequestsCountDto(eventId, 0L);
            }

            @Override
            public List<EventRequestsCountDto> countByEventIdsAndStatus(Set<Long> requestIds, RequestStatus status) {
                return List.of();
            }

            @Override
            public List<ParticipationRequestDto> updateRequestsStatus(Set<Long> requestIds, RequestStatus status) {
                return List.of();
            }
        };
    }
}
