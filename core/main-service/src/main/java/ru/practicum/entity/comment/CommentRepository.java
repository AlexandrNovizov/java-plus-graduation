package ru.practicum.entity.comment;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {


    Optional<Comment> findWithRelationsById(Long id);

    List<Comment> findAllByAuthorId(Long id);

    List<Comment> findAllByEventId(Long eventId);

    List<Comment> findAllByTextContainsIgnoreCase(String text, Pageable pageable);
}
