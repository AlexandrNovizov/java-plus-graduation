package ru.practicum.service;

import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.model.UserAction;

import java.util.List;
import java.util.Set;

public interface UserActionService {

    void processAction(UserActionAvro avro);

    List<Long> getAllEventIdsByUserId(Long userId);

    List<UserAction> getByUserId(Long userId);

    List<UserAction> getByUserIdAndEventIdIn(Long userId, Set<Long> eventIds);

    List<UserAction> getByEventIdIn(Set<Long> eventIds);
}
