package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.view.EventSimilarityInfo;
import ru.practicum.grpc.InteractionsCountRequestProto;
import ru.practicum.grpc.RecommendedEventProto;
import ru.practicum.grpc.SimilarEventsRequestProto;
import ru.practicum.grpc.UserPredictionsRequestProto;
import ru.practicum.mapper.EventSimilarityMapper;
import ru.practicum.model.UserAction;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationServiceImpl implements RecommendationService {

    private final EventSimilarityService similarityService;
    private final UserActionService actionService;

    @Override
    public List<RecommendedEventProto> getSimilarEvents(SimilarEventsRequestProto request) {
        final long eventId = request.getEventId();
        List<EventSimilarityInfo> similarEvents = similarityService.findAllByEventId(eventId);
        if (similarEvents.isEmpty()) {
            return List.of();
        }

        Set<Long> viewedIds = new HashSet<>(actionService.getAllEventIdsByUserId(request.getUserId()));
        if (viewedIds.isEmpty()) {
            return List.of();
        }

        return similarEvents.stream()
                .filter(s -> viewedIds.contains(s.similarId()))
                .sorted(Comparator.comparingDouble(EventSimilarityInfo::score).reversed())
                .limit(request.getMaxResults())
                .map(EventSimilarityMapper::mapToRecommendedEvent)
                .toList();
    }

    @Override
    public List<RecommendedEventProto> getRecommendedForUser(UserPredictionsRequestProto request) {
        long userId = request.getUserId();
        int maxResults = request.getMaxResults();

        List<UserAction> userActions = actionService.getByUserId(userId, maxResults);

        if (userActions.isEmpty()) {
            return List.of();
        }

        Set<Long> knownIds = new HashSet<>(actionService.getAllEventIdsByUserId(userId));

        Set<Long> similarIds = userActions.stream()
                .map(action -> action.getId().getEventId())
                .collect(Collectors.toSet());

        List<EventSimilarityInfo> similarButNotKnown =
                similarityService.findSimilarNotKnown(similarIds, knownIds, maxResults);

        if (similarButNotKnown.isEmpty()) {
            return List.of();
        }

        Set<Long> similarButNotKnownIds = similarButNotKnown.stream()
                .map(EventSimilarityInfo::similarId)
                .collect(Collectors.toSet());

        Map<Long, List<EventSimilarityInfo>> similarKnown =
                similarityService.findSimilarKnown(similarButNotKnownIds, knownIds)
                        .stream().collect(Collectors.groupingBy(EventSimilarityInfo::requestedId));

        Set<Long> similarKnownIds = similarKnown.values().stream()
                .flatMap(List::stream)
                .map(EventSimilarityInfo::similarId)
                .collect(Collectors.toSet());

        Map<Long, Double> actionsMap = actionService.getByUserIdAndEventIdIn(userId, similarKnownIds).stream()
                .collect(Collectors.toMap(
                        action -> action.getId().getEventId(),
                        action -> action.getActionType().getWeight()
                ));

        List<RecommendedEventProto> result = new ArrayList<>();
        for (var similarEvent : similarButNotKnown) {
            List<EventSimilarityInfo> similarEvents = similarKnown.get(similarEvent.similarId()).stream()
                    .sorted(Comparator.comparingDouble(EventSimilarityInfo::score).reversed())
                    .limit(maxResults)
                    .toList();

            double sumOfWeight = similarEvents.stream()
                    .mapToDouble(event -> event.score() * actionsMap.get(event.similarId()))
                    .sum();

            double sumOfScore = similarEvents.stream()
                    .mapToDouble(EventSimilarityInfo::score)
                    .sum();

            double calculatedScore = sumOfScore == 0 ?
                    0 :
                    sumOfWeight / sumOfScore;

            RecommendedEventProto proto = RecommendedEventProto.newBuilder()
                    .setEventId(similarEvent.similarId())
                    .setScore(calculatedScore)
                    .build();

            result.add(proto);
        }
        return result;
    }

    @Override
    public List<RecommendedEventProto> getInteractionsCount(InteractionsCountRequestProto request) {
        Set<Long> eventIds = new HashSet<>(request.getEventIdList());

        if (eventIds.isEmpty()) {
            return List.of();
        }

         Map<Long, Double> sumMap = actionService.getByEventIdIn(eventIds).stream()
                .collect(Collectors.groupingBy(
                        action -> action.getId().getEventId(),
                        Collectors.summingDouble(action -> action.getActionType().getWeight())
                ));

        return sumMap.entrySet().stream()
                .map(entry ->
                        RecommendedEventProto.newBuilder()
                                .setEventId(entry.getKey())
                                .setScore(entry.getValue())
                                .build()
                )
                .toList();
    }
}
