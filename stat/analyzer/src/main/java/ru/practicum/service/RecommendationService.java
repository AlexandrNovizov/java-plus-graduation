package ru.practicum.service;

import ru.practicum.grpc.InteractionsCountRequestProto;
import ru.practicum.grpc.RecommendedEventProto;
import ru.practicum.grpc.SimilarEventsRequestProto;
import ru.practicum.grpc.UserPredictionsRequestProto;

import java.util.List;

public interface RecommendationService {

    List<RecommendedEventProto> getSimilarEvents(SimilarEventsRequestProto request);

    List<RecommendedEventProto> getRecommendedForUser(UserPredictionsRequestProto request);

    List<RecommendedEventProto> getInteractionsCount(InteractionsCountRequestProto request);
}
