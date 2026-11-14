package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.model.UserAction;
import ru.practicum.model.UserActionId;

import java.util.List;
import java.util.Set;

@Repository
public interface UserActionRepository extends JpaRepository<UserAction, UserActionId> {

    @Query("SELECT DISTINCT ua.id.eventId " +
            "FROM UserAction ua " +
            "WHERE ua.id.userId = :userId")
    List<Long> findAllEventIdsByUserId(@Param("userId") Long userId);

    @Query("SELECT ua FROM UserAction ua WHERE ua.id.userId = :userId ORDER BY ua.actionTime DESC")
    List<UserAction> findByUserIdLimited(Long userId);


    List<UserAction> findByIdUserIdAndIdEventIdIn(Long userId, Set<Long> eventIds);

    List<UserAction> findByIdEventIdIn(Set<Long> eventIds);
}
