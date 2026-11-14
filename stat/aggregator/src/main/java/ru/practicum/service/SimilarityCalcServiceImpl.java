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

    private static final double ZERO = 0.0;

    // eventId -> (userId, weight)
    private final Map<Long, Map<Long, Double>> userActionsMap = new HashMap<>();
    // S_a: общая сумма весов по мероприятию
    private final Map<Long, Double> eventWeightSumMap = new HashMap<>();
    // меньший id -> (больший id -> S_min(A, B): сумма минимальных весов по общим пользователям)
    private final Map<Long, Map<Long, Double>> eventsMinWeightSumMap = new HashMap<>();

    @Override
    public List<EventSimilarityAvro> processUserAction(UserActionAvro action) {
        double newWeight = getWeight(action.getActionType());
        long eventId = action.getEventId();

        Map<Long, Double> userWeights = userActionsMap.computeIfAbsent(eventId, k -> new HashMap<>());
        long userId = action.getUserId();
        double oldWeight = userWeights.getOrDefault(userId, ZERO);

        log.info("Проверка весов");

        if (oldWeight >= newWeight) {
            log.info("{} >= {}, пересчета не требуется", oldWeight, newWeight);
            return List.of();
        }

        log.info("{} < {}, пересчет запущен", oldWeight, newWeight);

        userWeights.put(userId, newWeight);
        // обновляем S_a
        log.debug("Обновляем сумму весов для события id={}", eventId);
        var delta = newWeight - oldWeight;
        log.debug("Старое: {}, Дельта: {}, Новое: {}", eventWeightSumMap.get(eventId), delta, eventWeightSumMap.getOrDefault(eventId, ZERO) + delta);
        eventWeightSumMap.merge(eventId, delta, Double::sum);

        List<EventSimilarityAvro> result = new ArrayList<>();

        for (Map.Entry<Long, Map<Long, Double>> entry : userActionsMap.entrySet()) {
            long otherId = entry.getKey();
            if (otherId == eventId) {
                continue;
            }

            log.debug("Пересчет S(min(A, B)), eventId: {}, otherId: {}", eventId, otherId);

            // обновляем S_min(eventId, otherId) точно: вклад меняет только текущий пользователь
            double otherWeight = entry.getValue().getOrDefault(userId, ZERO);

            if (Double.compare(ZERO, otherWeight) == 0) {
                log.info("Пользователь с id={} не взаимодействовал с event={}, расчет не требуется", userId, entry.getKey());
                continue;
            }

            log.debug("Вес действия пользователя с другим событием: {}", otherWeight);
            log.debug("deltaMin = Math.min({}, {}) - Math.min({}, {})", newWeight, otherWeight, oldWeight, otherWeight);
            double deltaMin = Math.min(newWeight, otherWeight) - Math.min(oldWeight, otherWeight);
            log.debug("Разница суммы минимальных весов = {}", deltaMin);

            long eventA = Math.min(eventId, otherId);
            long eventB = Math.max(eventId, otherId);

            double minWeightSum = eventsMinWeightSumMap
                    .computeIfAbsent(eventA, k -> new HashMap<>())
                    .merge(eventB, deltaMin, Double::sum);

            log.debug("Сумма минимальных весов {}, {}: {}", eventA, eventB, minWeightSum);


            EventSimilarityAvro message = EventSimilarityAvro.newBuilder()
                    .setEventA(eventA)
                    .setEventB(eventB)
                    .setScore(similarity(minWeightSum, eventWeightSumMap.get(eventA), eventWeightSumMap.get(eventB)))
                    .setTimestamp(action.getTimestamp())
                    .build();

            result.add(message);
        }

        return result;
    }

    private double similarity(double minWeightSum, double aSum, double bSum) {
        log.debug("Расчет similarity");
        log.debug("Сумма min весов: {}, Сумма весов A: {}, Сумма весов B: {}", minWeightSum, aSum, bSum);
        double similarity = minWeightSum / (Math.sqrt(aSum) * Math.sqrt(bSum));
        log.debug("similarity = {}", similarity);
        return similarity;
    }

    private double getWeight(ActionTypeAvro actionType) {
        switch (actionType) {
            case VIEW:
                return 0.4;
            case REGISTER:
                return 0.8;
            case LIKE:
                return 1.0;
            default:
                throw new IllegalArgumentException(
                    "Can't find weight conversion for " + actionType);
        }
    }
}
