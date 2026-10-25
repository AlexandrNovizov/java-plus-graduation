package ru.practicum.feign.fallback;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.feign.EventFeignClient;
import ru.practicum.feign.exception.EventServiceUnavailableException;

import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class EventClientFallbackFactory implements FallbackFactory<EventFeignClient> {

    @Override
    public EventFeignClient create(Throwable cause) {
        return new EventFeignClient() {
            @Override
            public EventFullDto findById(Long eventId) {
                log.warn("event-service unavailable while requesting event id={}", eventId, cause);
                throw new EventServiceUnavailableException(
                        "event-service unavailable while requesting event id=" + eventId, cause
                );
            }

            @Override
            public boolean existsByCategoryId(Long categoryId) {
                log.warn("event-service unavailable while requesting existence of event with category id={}",
                        categoryId, cause);
                return false;
            }

            @Override
            public boolean isEventExists(Long eventId) {
                log.warn("event-service unavailable while requesting existence of event id={}",
                        eventId, cause);
                return false;
            }

            @Override
            public List<EventShortDto> getByIds(Set<Long> ids) {
                log.warn("event-service unavailable while requesting existence of event ids={}",
                        ids, cause);
                return List.of();
            }
        };
    }
}
