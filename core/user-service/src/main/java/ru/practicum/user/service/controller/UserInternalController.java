package ru.practicum.user.service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.user.UserFullDto;
import ru.practicum.user.service.entity.UserService;


/* feign client only routes controller */
@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/internal/users")
public class UserInternalController {

    private final UserService userService;

    @PutMapping
    @ResponseStatus(HttpStatus.OK)
    public UserFullDto updateUser(@RequestBody UserFullDto dto) {
        return userService.updateUser(dto);
    }

    @GetMapping("/{userId}")
    @ResponseStatus(HttpStatus.OK)
    public UserFullDto getById(@PathVariable Long userId) {
        return userService.findById(userId);
    }

    @GetMapping("/exists/{userId}")
    @ResponseStatus(HttpStatus.OK)
    public void checkUserExists(@PathVariable Long userId) {
        userService.checkUserExist(userId);
    }
}
