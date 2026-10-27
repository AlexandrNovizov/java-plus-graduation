package ru.practicum.repository;

import com.querydsl.core.types.Predicate;
import lombok.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.query.Param;
import ru.practicum.dto.event.EventRequestsCountDto;
import ru.practicum.entity.Event;
import ru.practicum.enums.event.EventState;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface EventRepository extends JpaRepository<Event, Long>, QuerydslPredicateExecutor<Event> {

    List<Event> findAllByInitiatorId(Long initiatorId, Pageable pageable);

    @NonNull
    Page<Event> findAll(@NonNull Pageable pageable);

    @NonNull
    Page<Event> findAll(@NonNull Predicate predicate, @NonNull Pageable pageable);

    Optional<Event> findWithRelationsById(Long id);

    Optional<Event> findByIdAndState(Long id, EventState state);

    boolean existsByCategoryId(Long categoryId);

    @Modifying
    @Query("UPDATE Event e SET e.views = e.views + 1 WHERE e.id = :id")
    int incrementViews(@Param("id") Long id);

    @Query("SELECT new ru.practicum.dto.event.EventRequestsCountDto(e.id, e.views) " +
           "FROM Event e " +
           "WHERE e.id IN :ids")
    List<EventRequestsCountDto> getViewsCountByIds(@Param("ids") Set<Long> ids);

}