package ru.practicum.user.service.entity;

import ru.practicum.dto.comment.UserCommentAdminDto;
import ru.practicum.dto.user.UserFullDto;
import ru.practicum.dto.user.NewUserRequest;
import ru.practicum.dto.user.UserDto;
import ru.practicum.dto.user.UserParamDto;

import java.time.LocalDateTime;
import java.util.List;

public interface UserService {

    UserDto saveUser(NewUserRequest userRequest);

    List<UserDto> getUsers(UserParamDto userParamDto);

    void deleteUserById(Long userId);

    UserFullDto findById(Long userId);

    void checkUserExist(Long userId);

    UserCommentAdminDto updateUserBan(Long userId, LocalDateTime banDate);

    UserFullDto updateUser(UserFullDto dto);
}
