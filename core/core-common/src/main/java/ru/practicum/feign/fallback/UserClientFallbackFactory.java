package ru.practicum.feign.fallback;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.practicum.dto.user.UserDto;
import ru.practicum.dto.user.UserFullDto;
import ru.practicum.dto.user.UserParamDto;
import ru.practicum.feign.exception.UserServiceUnavailableException;
import ru.practicum.feign.UserFeignClient;

import java.util.List;

@Slf4j
@Component
public class UserClientFallbackFactory implements FallbackFactory<UserFeignClient> {

    @Override
    public UserFeignClient create(Throwable cause) {
        return new UserFeignClient() {
            @Override
            public List<UserDto> getUsers(UserParamDto dto) {
                log.warn("user-service unavailable while requesting users {}", dto, cause);
                return List.of();
            }

            @Override
            public UserFullDto findById(Long userId) {
                log.warn("user-service unavailable while requesting user id={}", userId, cause);
                throw new UserServiceUnavailableException(
                        "user-service unavailable while requesting user id=" + userId, cause
                );
            }

            @Override
            public UserFullDto updateUser(UserFullDto dto) {
                log.warn("user-service unavailable while updating user {}", dto, cause);
                throw new UserServiceUnavailableException(
                        "user-service unavailable while updating user id=" + dto.getId(), cause
                );
            }

            @Override
            public void checkUserExists(Long userId) {
                log.warn("user-service unavailable while checking existence of user id={}", userId, cause);
                throw new UserServiceUnavailableException(
                        "user-service unavailable while checking existence of user id=" + userId, cause
                );
            }
        };
    }
}
