package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Instant;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class SimilarityCalcServiceImpl implements SimilarityCalcService {

    private final Map<Long, Map<Long, Double>> userActionsMap = new TreeMap<>();
    private final Map<Long, Double> eventWeightSumMap = new HashMap<>();
    private final TreeMap<Long, Map<Long, Double>> eventsMinWeightSumMap = new TreeMap<>();

    private static final double ZERO = 0.0;

    @Override
    public List<EventSimilarityAvro> processUserAction(UserActionAvro action) {

        Double newWeight = getWeight(action.getActionType());

        Long eventId = action.getEventId();

        Map<Long, Double> userWeightsMap = userActionsMap.computeIfAbsent(eventId, k -> new HashMap<>());
        Double oldWeight = userWeightsMap.getOrDefault(action.getUserId(), ZERO);

        if (oldWeight > newWeight) {
            return List.of();
        }
        userWeightsMap.put(action.getUserId(), newWeight);
        eventWeightSumMap.putIfAbsent(eventId, newWeight);

        if (Double.compare(ZERO, oldWeight) == 0) {
            return computeSimilarity(eventId);
        } else {
            return updateSimilarity(eventId, oldWeight, newWeight);
        }
    }

    private List<EventSimilarityAvro> updateSimilarity(Long eventId, Double oldWeight, Double newWeight) {
        double delta = newWeight - oldWeight;
        eventWeightSumMap.computeIfPresent(eventId, (k, v) -> v + delta);
        NavigableMap<Long, Map<Long, Double>> lessIdsMap = eventsMinWeightSumMap.headMap(eventId, false);

        Set<Long> idsToUpdateSimilarity = new HashSet<>();

        List<EventSimilarityAvro> result = new ArrayList<>();

        for (Map.Entry<Long, Map<Long, Double>> entry : lessIdsMap.entrySet()) {

            Map<Long, Double> minSumMap = entry.getValue();
            if (minSumMap.containsKey(eventId)) {
                minSumMap.compute(eventId, (k, v) -> v + delta);
                idsToUpdateSimilarity.add(entry.getKey());
            }
        }

        Map<Long, Double> greaterIdsMap = eventsMinWeightSumMap.get(eventId);
        if (greaterIdsMap != null) {
            List<Long> idsToPutNewWeight = new ArrayList<>();
            for (Map.Entry<Long, Double> entry : greaterIdsMap.entrySet()) {
                if (entry.getValue() < newWeight) {
                    idsToPutNewWeight.add(entry.getKey());
                }
            }

            for (Long id : idsToPutNewWeight) {
                greaterIdsMap.compute(id, (k, v) -> v + delta);
            }
            idsToUpdateSimilarity.addAll(idsToPutNewWeight);
        }

        for (Long id : idsToUpdateSimilarity) {

            long eventA = Long.min(eventId, id);
            long eventB = Long.max(eventId, id);

            double minWeightSum = eventsMinWeightSumMap.get(eventA).get(eventB);

            double aSum = getOrCalculateEventWeightSum(eventA);
            double bSum = getOrCalculateEventWeightSum(eventB);

            EventSimilarityAvro avro = EventSimilarityAvro.newBuilder()
                    .setEventA(eventA)
                    .setEventB(eventB)
                    .setScore(similarity(minWeightSum, aSum, bSum))
                    .setTimestamp(Instant.now())
                    .build();

            result.add(avro);
        }

        return result;
    }

    private List<EventSimilarityAvro> computeSimilarity(Long eventId) {

        List<EventSimilarityAvro> result = new ArrayList<>();
        for (Map.Entry<Long, Map<Long, Double>> entry : userActionsMap.entrySet()) {
            if (Objects.equals(entry.getKey(), eventId)) {
                continue;
            }
            Set<Long> users = new HashSet<>(userActionsMap.get(eventId).keySet());

            users.addAll(entry.getValue().keySet());

            long eventA = Long.min(eventId, entry.getKey());
            long eventB = Long.max(eventId, entry.getKey());

            Map<Long, Double> aActions = userActionsMap.get(eventA);
            Map<Long, Double> bActions = userActionsMap.get(eventB);
            Map<Long, Double> minWeightMap = eventsMinWeightSumMap.computeIfAbsent(eventA, k -> new HashMap<>());

            double minWeightSum = users.stream()
                    .map(user ->
                            Double.min(aActions.getOrDefault(user, ZERO), bActions.getOrDefault(user, ZERO))
                    )
                    .filter(minAction -> Double.compare(ZERO, minAction) != 0)
                    .mapToDouble(Double::doubleValue)
                    .sum();

            if (minWeightSum == 0) {
                return List.of();
            }
            minWeightMap.put(eventB, minWeightSum);

            Double aSum = getOrCalculateEventWeightSum(eventA);
            Double bSum = getOrCalculateEventWeightSum(eventB);

            EventSimilarityAvro avro = EventSimilarityAvro.newBuilder()
                    .setEventA(eventA)
                    .setEventB(eventB)
                    .setScore(similarity(minWeightSum, aSum, bSum))
                    .setTimestamp(Instant.now())
                    .build();

            result.add(avro);
        }

        return result;
    }

    private Double getOrCalculateEventWeightSum(long eventId) {
        Double result = eventWeightSumMap.get(eventId);
        if (result == null) {
            result = userActionsMap.get(eventId).values().stream()
                    .mapToDouble(Double::doubleValue)
                    .sum();
        }
        return result;
    }

    private Double similarity(double minWeightSum, double aSum, double bSum) {
        double denominator = Math.sqrt(aSum) * Math.sqrt(bSum);
        return minWeightSum / denominator;
    }

    private Double getWeight(ActionTypeAvro actionType) {
        switch (actionType) {
            case VIEW:
                return 0.4;
            case REGISTER:
                return 0.8;
            case LIKE:
                return 1.0;
            case null, default:
                throw new IllegalArgumentException("Can't find weight conversion for " +
                        (actionType == null ? null : actionType.name()));
        }
    }
}
