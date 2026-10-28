package ru.practicum.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.practicum.dto.user.UserDto;
import ru.practicum.dto.user.UserFullDto;
import ru.practicum.dto.user.UserParamDto;
import ru.practicum.feign.fallback.UserClientFallbackFactory;

import java.util.List;

@FeignClient(name = "user-service", fallbackFactory = UserClientFallbackFactory.class)
public interface UserFeignClient {

    @GetMapping("/admin/users")
    List<UserDto> getUsers(@SpringQueryMap UserParamDto dto);

    @GetMapping("/internal/users/{userId}")
    UserFullDto findById(@PathVariable Long userId);

    @PutMapping("/internal/users")
    UserFullDto updateUser(@RequestBody UserFullDto dto);

    @GetMapping("/internal/users/exists/{userId}")
    void checkUserExists(@PathVariable Long userId);
}
