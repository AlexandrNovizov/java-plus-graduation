package ru.practicum.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.view.EventSimilarityInfo;
import ru.practicum.grpc.RecommendedEventProto;

@UtilityClass
public class EventSimilarityMapper {

    public static RecommendedEventProto mapToRecommendedEvent(EventSimilarityInfo eventSimilarity) {
        return RecommendedEventProto.newBuilder()
                .setEventId(eventSimilarity.similarId())
                .setScore(eventSimilarity.score())
                .build();
    }
}
