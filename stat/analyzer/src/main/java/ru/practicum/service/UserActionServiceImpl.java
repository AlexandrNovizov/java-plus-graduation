package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.mapper.UserActionTypeMapper;
import ru.practicum.model.UserAction;
import ru.practicum.model.UserActionId;
import ru.practicum.repository.UserActionRepository;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserActionServiceImpl implements UserActionService {

    private final UserActionRepository repository;

    @Override
    @Transactional
    public void processAction(UserActionAvro avro) {
        UserActionId id = new UserActionId();
        id.setUserId(avro.getUserId());
        id.setEventId(avro.getEventId());

        UserAction action = UserAction.builder()
                .id(id)
                .actionType(UserActionTypeMapper.mapToEntity(avro.getActionType()))
                .actionTime(LocalDateTime.ofInstant(avro.getTimestamp(), ZoneId.systemDefault()))
                .build();

        Optional<UserAction> optSavedAction = repository.findById(id);

        if (optSavedAction.isPresent()) {
            UserAction savedAction = optSavedAction.get();
            if (savedAction.getActionType().getWeight() < action.getActionType().getWeight()) {
                savedAction.setActionType(action.getActionType());
                savedAction.setActionTime(action.getActionTime());
                repository.save(savedAction);
            }
        } else {
            repository.save(action);
        }
    }

    @Override
    public List<Long> getAllEventIdsByUserId(Long userId) {
        return repository.findAllEventIdsByUserId(userId);
    }

    @Override
    public List<UserAction> getByUserId(Long userId, Integer maxResults) {

        Sort sort = Sort.by(Sort.Direction.DESC, "action_time");
        Pageable pageable = PageRequest.of(0, maxResults, sort);

        return repository.findByUserIdLimited(userId, pageable);
    }

    @Override
    public List<UserAction> getByUserIdAndEventIdIn(Long userId, Set<Long> eventIds) {
        return repository.findByIdUserIdAndIdEventIdIn(userId, eventIds);
    }

    @Override
    public List<UserAction> getByEventIdIn(Set<Long> eventIds) {
        return repository.findByIdEventIdIn(eventIds);
    }
}
