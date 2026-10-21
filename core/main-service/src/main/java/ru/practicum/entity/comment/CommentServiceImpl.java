package ru.practicum.entity.comment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.comment.CommentDto;
import ru.practicum.dto.comment.NewCommentDto;
import ru.practicum.dto.comment.UserCommentAdminDto;
import ru.practicum.dto.comment.UpdateCommentDto;
import ru.practicum.dto.user.UserDto;
import ru.practicum.dto.user.UserFullDto;
import ru.practicum.dto.user.UserParamDto;
import ru.practicum.dto.util.UserUtil;
import ru.practicum.entity.event.Event;
import ru.practicum.entity.event.EventService;
import ru.practicum.enums.event.EventState;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.feign.UserFeignClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@Slf4j
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final CommentRepository commentRepository;
    private final UserFeignClient userClient;
    private final EventService eventService;


    @Override
    @Transactional
    public CommentDto createComment(NewCommentDto newCommentDto, Long userId) {
        UserFullDto user = userClient.findById(userId);
        banCheck(user);
        Event event = eventService.findEventByIdAndState(newCommentDto.getEventId(), EventState.PUBLISHED);

        user.setCommentsCount(user.getCommentsCount() + 1);
        setUserRank(user);

        user = userClient.updateUser(user);
        Comment comment = commentRepository.save(CommentMapper.toComment(newCommentDto, userId, event));
        CommentDto commentDto = CommentMapper.toCommentDto(comment, user.getName());

        log.info("Новый комментарий на событие: {}", CommentMapper.toLogComment(commentDto));

        return commentDto;
    }

    @Override
    @Transactional
    public CommentDto updateComment(UpdateCommentDto updateCommentDto) {
        Comment comment = getById(updateCommentDto.getCommentId());
        UserFullDto user = userClient.findById(updateCommentDto.getUserId());
        UserFullDto author = userClient.findById(comment.getAuthorId());

        checkAuthor(author, updateCommentDto.getUserId());
        banCheck(user);

        comment.setText(updateCommentDto.getText());
        CommentDto commentDto = CommentMapper.toCommentDto(comment, user.getName());
        log.info("Обновлен комментарий на событие: {}", CommentMapper.toLogComment(commentDto));

        return commentDto;
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        Comment comment = getById(commentId);
        UserFullDto author = userClient.findById(comment.getAuthorId());
        checkAuthor(author, userId);

        commentRepository.delete(comment);

        author.setCommentsCount(author.getCommentsCount() - 1);
        setUserRank(author);

        log.info("Комментарий id: {} удален пользователем id: {}", commentId, userId);
    }

    @Override
    public List<CommentDto> getComments(Long userId) {

        UserFullDto author = userClient.findById(userId);
        return commentRepository.findAllByAuthorId(userId).stream()
                .map(comment -> CommentMapper.toCommentDto(comment, author.getName()))
                .toList();
    }

    @Override
    @Transactional
    public UserCommentAdminDto giveWarning(Long commentId) {
        Comment comment = getById(commentId);
        UserFullDto author = userClient.findById(comment.getAuthorId());

        giveWarningForComment(comment, author);

        if (UserUtil.isWarningsLimitExceeded(author)) {
            banToWarningLimitExceeded(author);
        } else {
           incrementWarningCount(author);
        }
        author = userClient.updateUser(author);
        return CommentMapper.toUserCommentAdminDto(author);
    }

    @Override
    @Transactional
    public void adminDelete(Long commentId) {
        Comment comment = getById(commentId);
        UserFullDto author = userClient.findById(comment.getAuthorId());

        CommentDto dto = CommentMapper.toCommentDto(comment, author.getName());
        author.setCommentsCount(author.getCommentsCount() - 1);
        setUserRank(author);

        commentRepository.delete(comment);

        log.info("Комментарий удален администратором: {}", CommentMapper.toLogComment(dto));
    }

    @Override
    public List<CommentDto> getEventComments(Long eventId) {
        if (!eventService.isEventExists(eventId)) {
            throw new NotFoundException(String.format("Event id: %d not found.", eventId));
        }

        List<Comment> comments = commentRepository.findAllByEventId(eventId);

        if (comments.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> authorIds = comments.stream()
                .map(Comment::getAuthorId)
                .collect(Collectors.toSet());

        UserParamDto params = new UserParamDto();
        params.setIds(authorIds);

        final Map<Long, String> authorsNameMap = userClient.getUsers(params).stream()
                .collect(Collectors.toMap(UserDto::getId, UserDto::getName));

        return comments.stream()
                .map(c -> CommentMapper.toCommentDto(c, authorsNameMap.get(c.getAuthorId())))
                .toList();
    }

    @Override
    public CommentDto getComment(Long commentId) {
        Comment comment = getById(commentId);
        UserFullDto author = userClient.findById(comment.getAuthorId());
        return CommentMapper.toCommentDto(comment, author.getName());
    }

    @Override
    public List<CommentDto> searchComments(String text, Pageable pageable) {
        List<Comment> comments = commentRepository.findAllByTextContainsIgnoreCase(text, pageable);

        if (comments.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> authorIds = comments.stream()
                .map(Comment::getAuthorId)
                .collect(Collectors.toSet());

        UserParamDto params = new UserParamDto();
        params.setIds(authorIds);

        final Map<Long, String> authorsNameMap = userClient.getUsers(params).stream()
                .collect(Collectors.toMap(UserDto::getId, UserDto::getName));

        return comments.stream()
                .map(c -> CommentMapper.toCommentDto(c, authorsNameMap.get(c.getAuthorId())))
                .toList();
    }

    @Override
    public Comment getById(Long id) {
        return commentRepository.findWithRelationsById(id).orElseThrow(
                () -> new NotFoundException("Comment not found with id: " + id)
        );
    }

    private void banCheck(UserFullDto user) {
        LocalDateTime bannedUntil = null;
        if (user.getBannedUntil() != null) {
            bannedUntil = LocalDateTime.parse(user.getBannedUntil(), FORMATTER);
        }

        if (bannedUntil != null && bannedUntil.isBefore(LocalDateTime.now())) {
            user.setBannedUntil(null);
        }

        if (UserUtil.isBanned(user)) {
            throw new ConflictException("You have been banned!");
        }
    }

    private void checkAuthor(UserFullDto commentAuthor, Long userId) {
        if (!commentAuthor.getId().equals(userId)) {
            throw new ConflictException("It's not your own comment!");
        }
    }

    private void setUserRank(UserFullDto user) {
        int commentsCount = user.getCommentsCount();
        CommentsRank userRank = CommentsRank.valueOf(user.getRank());

        if (commentsCount == 1 && userRank == CommentsRank.REGULAR) {
            user.setRank(CommentsRank.NOVICE.toString());
        }

        if (commentsCount == 2) {
            user.setRank(CommentsRank.REGULAR.toString());
        }

        if (commentsCount > 2 && userRank == CommentsRank.REGULAR) {
            user.setRank(CommentsRank.VETERAN.toString());
        }
    }

    private void giveWarningForComment(Comment comment, UserFullDto author) {
        if (comment.isAdminWarning()) {
            throw new ConflictException(
                    String.format("User with id %s already received warning for comment with id %s", author.getId(), comment.getId())
            );
        }
        comment.setAdminWarning(true);
    }

    private void incrementWarningCount(UserFullDto author) {
        log.info("Пользователь получил предупреждение: {}", author);
        author.setAdminWarnings(author.getAdminWarnings() + 1);
    }

    private void banToWarningLimitExceeded(UserFullDto author) {
        banCheck(author);
        log.info("Пользователь заблокирован из-за превышения лимита предупреждений: {}", author);
        author.setAdminWarnings(0);
        author.setBannedUntil(LocalDateTime.now().plusMonths(6).format(FORMATTER));
    }
}
