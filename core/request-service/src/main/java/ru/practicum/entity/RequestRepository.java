package ru.practicum.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.dto.event.EventRequestsCountDto;
import ru.practicum.enums.request.RequestStatus;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface RequestRepository extends JpaRepository<Request, Long> {

    boolean existsByRequesterIdAndEventId(Long requesterId, Long eventId);

    @Query("SELECT new ru.practicum.dto.event.EventRequestsCountDto(r.eventId, COUNT(r.id)) " +
            "FROM Request r " +
            "WHERE r.eventId = :eventId AND r.status = :status " +
            "GROUP BY r.eventId")
    Optional<EventRequestsCountDto> countByEventIdAndStatus(@Param("eventId") Long eventId,
                                                            @Param("status") RequestStatus status);

    List<Request> findByRequesterId(Long requesterId);

    List<Request> findByIdInAndEventId(Set<Long> ids, Long eventId);

    List<Request> findByEventId(Long eventId);


    @Query("SELECT new ru.practicum.dto.event.EventRequestsCountDto(r.eventId, COUNT(r.id)) " +
            "FROM Request r " +
            "WHERE r.eventId IN :ids AND r.status = :status " +
            "GROUP BY r.eventId")
    List<EventRequestsCountDto> countByEventIdInAndStatus(@Param("ids") Set<Long> ids,
                                                          @Param("status") RequestStatus status);
}
