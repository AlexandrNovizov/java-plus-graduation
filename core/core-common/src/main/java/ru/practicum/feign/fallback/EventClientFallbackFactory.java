package ru.practicum.feign.fallback;

import org.springframework.cloud.openfeign.FallbackFactory;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.feign.EventFeignClient;
import ru.practicum.feign.exception.EventServiceUnavailableException;

public class EventClientFallbackFactory implements FallbackFactory<EventFeignClient> {

    @Override
    public EventFeignClient create(Throwable cause) {
        return new EventFeignClient() {
            @Override
            public EventFullDto findById(Long eventId) {
                throw new EventServiceUnavailableException(
                        "event-service unavailable while requesting user id=" + eventId, cause
                );
            }
        };
    }
}
