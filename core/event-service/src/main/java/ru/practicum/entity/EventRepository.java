package ru.practicum.entity;

import com.querydsl.core.types.Predicate;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import ru.practicum.enums.event.EventState;

import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long>, QuerydslPredicateExecutor<Event> {

    List<Event> findAllByInitiatorId(Long initiatorId, Pageable pageable);

    @NonNull
    Page<Event> findAll(@NonNull Pageable pageable);

    @NonNull
    Page<Event> findAll(@NonNull Predicate predicate, @NonNull Pageable pageable);

    Optional<Event> findWithRelationsById(Long id);

    Optional<Event> findByIdAndState(Long id, EventState state);

    boolean existsByCategoryId(Long categoryId);
}