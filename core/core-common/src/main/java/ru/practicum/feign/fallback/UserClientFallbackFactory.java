package ru.practicum.feign.fallback;

import org.springframework.cloud.openfeign.FallbackFactory;
import ru.practicum.dto.user.UserDto;
import ru.practicum.dto.user.UserFullDto;
import ru.practicum.dto.user.UserParamDto;
import ru.practicum.feign.exception.UserServiceUnavailableException;
import ru.practicum.feign.UserFeignClient;

import java.util.List;

public class UserClientFallbackFactory implements FallbackFactory<UserFeignClient> {

    @Override
    public UserFeignClient create(Throwable cause) {
        return new UserFeignClient() {
            @Override
            public List<UserDto> getUsers(UserParamDto dto) {
                return List.of();
            }

            @Override
            public UserFullDto findById(Long userId) {
                throw new UserServiceUnavailableException(
                        "user-service unavailable while requesting user id=" + userId, cause
                );
            }

            @Override
            public UserFullDto updateUser(UserFullDto dto) {
                throw new UserServiceUnavailableException(
                        "user-service unavailable while updating user id=" + dto.getId(), cause
                );
            }

            @Override
            public void checkUserExists(Long userId) {
                throw new UserServiceUnavailableException(
                        "user-service unavailable while checking existence of user id=" + userId, cause
                );
            }
        };
    }
}
