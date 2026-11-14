package ru.practicum.service;

import ru.practicum.view.EventSimilarityInfo;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;

import java.util.List;
import java.util.Set;

public interface EventSimilarityService {

    void processMessage(EventSimilarityAvro avro);

    List<EventSimilarityInfo> findAllByEventId(Long eventId);

    List<EventSimilarityInfo> findSimilarNotKnown(Set<Long> similarIds, Set<Long> knownIds);

    List<EventSimilarityInfo> findSimilarKnown(Set<Long> similarIds, Set<Long> knownIds);
}
