package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.comment.UserCommentAdminDto;
import ru.practicum.dto.user.UserFullDto;
import ru.practicum.dto.user.NewUserRequest;
import ru.practicum.dto.user.UserDto;
import ru.practicum.dto.user.UserParamDto;
import ru.practicum.enums.comment.CommentsRank;
import ru.practicum.entity.User;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.repository.UserRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    @Transactional
    public UserDto saveUser(NewUserRequest userRequest) {
        if (userRepository.existsByMail(userRequest.getEmail())) {
            throw new ConflictException("User with this email already exists");
        }

        User savedUser = userRepository.save(UserMapper.toUser(userRequest));
        log.info("Новый пользователь {}", savedUser);

        return UserMapper.toUserDto(savedUser);
    }

    @Override
    public List<UserDto> getUsers(UserParamDto userParamDto) {

        if (userParamDto.getIds() != null && !userParamDto.getIds().isEmpty()) {
            List<Long> ids = userParamDto.getIds().stream().toList();
            return UserMapper.toUserDtos(userRepository.findAllByIdIn(ids));
        }

        int page = userParamDto.getFrom() / userParamDto.getSize();
        int pageSize = userParamDto.getSize();
        List<User> users = userRepository.findAll(PageRequest.of(page, pageSize)).getContent();

        return UserMapper.toUserDtos(users);
    }

    @Override
    @Transactional
    public void deleteUserById(Long userId) {
        checkUserExist(userId);
        userRepository.deleteById(userId);
    }

    @Override
    public UserFullDto findById(Long userId) {
        return UserMapper.toFullDto(getUserOrThrowException(userId));
    }

    @Override
    public void checkUserExist(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User with id: " + userId + " does not exist");
        }
    }

    @Override
    @Transactional
    public UserCommentAdminDto updateUserBan(Long userId, LocalDateTime banDate) {
        User user = getUserOrThrowException(userId);
        return banDate != null ? banUser(user, banDate) : unbanUser(user);
    }

    @Override
    @Transactional
    public UserFullDto updateUser(UserFullDto dto) {
        User user = getUserOrThrowException(dto.getId());
        updateFields(user, dto);
        user = userRepository.save(user);
        return UserMapper.toFullDto(user);
    }

    private User getUserOrThrowException(Long userId) {
        return userRepository.findById(userId).orElseThrow(
                () -> new NotFoundException("User with id: " + userId + " does not exist"));
    }

    private static UserCommentAdminDto unbanUser(User user) {
        if (!user.isBanned()) {
            throw new ConflictException("User not banned");
        }

        log.info("Пользователь разблокирован:{}", user);
        user.setBannedUntil(null);
        return UserMapper.toUserCommentAdminDto(user);
    }

    private static UserCommentAdminDto banUser(User user, LocalDateTime banDate) {
        if (user.isBanned()) {
            throw new ConflictException("User already banned");
        }

        log.info("Пользователь получил блокировку до {}: {}", banDate,  user);
        user.setBannedUntil(banDate);
        return UserMapper.toUserCommentAdminDto(user);
    }

    private void updateFields(User user, UserFullDto dto) {
        if (dto.getName() != null && !user.getName().equals(dto.getName())) {
            user.setName(dto.getName());
        }
        if (dto.getEmail() != null && !user.getMail().equals(dto.getEmail())) {
            user.setMail(dto.getEmail());
        }
        if (dto.getBannedUntil() != null) {
            LocalDateTime dtoBannedUntil = LocalDateTime.parse(dto.getBannedUntil(), formatter);
            if (!dtoBannedUntil.equals(user.getBannedUntil())) {
                user.setBannedUntil(dtoBannedUntil);
            }
        }
        if (dto.getAdminWarnings() != null && !user.getAdminWarnings().equals(dto.getAdminWarnings())) {
            user.setAdminWarnings(dto.getAdminWarnings());
        }
        if (dto.getCommentsCount() != null && !(user.getCommentsCount() == dto.getCommentsCount())) {
            user.setCommentsCount(dto.getCommentsCount());
        }
        if (dto.getRank() != null) {
            CommentsRank dtoRank = CommentsRank.valueOf(dto.getRank());
            if (!user.getRank().equals(dtoRank)) {
                user.setRank(dtoRank);
            }
        }
    }
}
