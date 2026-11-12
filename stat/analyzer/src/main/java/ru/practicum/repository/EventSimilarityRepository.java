package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.view.EventSimilarityInfo;
import ru.practicum.model.EventSimilarity;
import ru.practicum.model.EventSimilarityId;

import java.util.List;
import java.util.Set;

public interface EventSimilarityRepository extends JpaRepository<EventSimilarity, EventSimilarityId> {

    @Query("""
        SELECT NEW ru.practicum.view.EventSimilarityInfo(
            CASE 
                WHEN es.id.eventA = :eventId THEN es.id.eventA 
                ELSE es.id.eventB 
            END,
            CASE 
                WHEN es.id.eventA = :eventId THEN es.id.eventB 
                ELSE es.id.eventA 
            END,
            es.score
        )
        FROM EventSimilarity es
        WHERE es.id.eventA = :eventId OR es.id.eventB = :eventId
    """)
    List<EventSimilarityInfo> findAllByEventId(@Param("eventId") Long eventId);

    @Query("""
            SELECT NEW ru.practicum.view.EventSimilarityInfo(
            CASE
                WHEN es.id.eventA IN (:similarIds) THEN es.id.eventA
                ELSE es.id.eventB
            END,
            CASE
                WHEN es.id.eventA IN (:similarIds) THEN es.id.eventB
                ELSE es.id.eventA
            END,
            es.score
            ) FROM EventSimilarity es
            WHERE es.id.eventA IN (:similarIds) AND es.id.eventB NOT IN (:knownIds) OR
            es.id.eventA NOT IN (:knownIds) AND es.id.eventB IN (:similarIds) 
            ORDER BY es.score DESC""")
    List<EventSimilarityInfo> findSimilarNotKnown(@Param("similarIds") Set<Long> similarIds,
                                                  @Param("knownIds") Set<Long> knownIds);

    @Query("""
            SELECT NEW ru.practicum.view.EventSimilarityInfo(
            CASE
                WHEN es.id.eventA IN (:similarIds) THEN es.id.eventA
                ELSE es.id.eventB
            END,
            CASE
                WHEN es.id.eventA IN (:similarIds) THEN es.id.eventB
                ELSE es.id.eventA
            END,
            es.score
            ) FROM EventSimilarity es
            WHERE es.id.eventA IN (:similarIds) AND es.id.eventB IN (:knownIds) OR
            es.id.eventA IN (:knownIds) AND es.id.eventB IN (:similarIds)""")
    List<EventSimilarityInfo> findSimilarKnown(@Param("similarIds") Set<Long> similarIds,
                                           @Param("knownIds") Set<Long> knownIds);
}
