package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.view.EventSimilarityInfo;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.model.EventSimilarity;
import ru.practicum.model.EventSimilarityId;
import ru.practicum.repository.EventSimilarityRepository;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class EventSimilarityServiceImpl implements EventSimilarityService {

    private final EventSimilarityRepository repository;

    @Override
    @Transactional
    public void processMessage(EventSimilarityAvro avro) {

        EventSimilarityId id = new EventSimilarityId(avro.getEventA(), avro.getEventB());
        EventSimilarity similarity = EventSimilarity.builder()
                .id(id)
                .score(avro.getScore())
                .calcTime(LocalDateTime.ofInstant(avro.getTimestamp(), ZoneId.systemDefault()))
                .build();

        Optional<EventSimilarity> optSavedSimilarity = repository.findById(id);

        if (optSavedSimilarity.isPresent()) {
            EventSimilarity savedSimilarity = optSavedSimilarity.get();
            savedSimilarity.setScore(similarity.getScore());
            savedSimilarity.setCalcTime(similarity.getCalcTime());
            repository.save(savedSimilarity);
        } else {
            repository.save(similarity);
        }
    }

    @Override
    public List<EventSimilarityInfo> findAllByEventId(Long eventId) {
        return repository.findAllByEventId(eventId);
    }

    @Override
    public List<EventSimilarityInfo> findSimilarNotKnown(Set<Long> similarIds, Set<Long> knownIds, Integer maxResults) {
        Sort sort = Sort.by(Sort.Direction.DESC, "score");
        Pageable pageable = PageRequest.of(0, maxResults, sort);

        return repository.findSimilarNotKnown(similarIds, knownIds, pageable);
    }

    @Override
    public List<EventSimilarityInfo> findSimilarKnown(Set<Long> similarIds, Set<Long> knownIds) {
        return repository.findSimilarKnown(similarIds, knownIds);
    }
}
