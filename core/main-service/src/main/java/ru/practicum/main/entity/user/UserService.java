package ru.practicum.main.entity.user;

import ru.practicum.main.dto.comment.UserCommentAdminDto;
import ru.practicum.main.dto.user.NewUserRequest;
import ru.practicum.main.dto.user.UserDto;
import ru.practicum.main.dto.user.UserParamDto;

import java.time.LocalDateTime;
import java.util.List;

public interface UserService {

    UserDto saveUser(NewUserRequest userRequest);

    List<UserDto> getUsers(UserParamDto userParamDto);

    void deleteUserById(Long userId);

    User findById(Long userId);

    void checkUserExist(Long userId);

    UserCommentAdminDto updateUserBan(Long userId, LocalDateTime banDate);
}
