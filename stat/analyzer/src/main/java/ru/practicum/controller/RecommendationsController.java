package ru.practicum.controller;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.grpc.*;
import ru.practicum.service.RecommendationService;

import java.util.List;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class RecommendationsController extends RecommendationsControllerGrpc.RecommendationsControllerImplBase {

    private final RecommendationService recommendationService;

    @Override
    public void getRecommendationsForUser(UserPredictionsRequestProto request, StreamObserver<RecommendedEventProto> responseObserver) {
        log.info("received request: userId: {}, maxRes: {}",request.getUserId(), request.getMaxResults());

        try {
            List<RecommendedEventProto> similarEvents = recommendationService.getRecommendedForUser(request);

            for (RecommendedEventProto event : similarEvents) {
                responseObserver.onNext(event);
            }

            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(new StatusRuntimeException(
                    Status.INTERNAL
                            .withDescription(e.getLocalizedMessage())
                            .withCause(e)
            ));
        }
    }

    @Override
    public void getSimilarEvents(SimilarEventsRequestProto request, StreamObserver<RecommendedEventProto> responseObserver) {
        log.info("received request: eventId: {}, userId: {}, maxRes: {}", request.getEventId(), request.getUserId(), request.getMaxResults());

        try {
            List<RecommendedEventProto> similarEvents = recommendationService.getSimilarEvents(request);

            for (RecommendedEventProto event : similarEvents) {
                responseObserver.onNext(event);
            }

            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(new StatusRuntimeException(
                    Status.INTERNAL
                            .withDescription(e.getLocalizedMessage())
                            .withCause(e)
            ));
        }
    }

    @Override
    public void getInteractionsCount(InteractionsCountRequestProto request, StreamObserver<RecommendedEventProto> responseObserver) {
        log.info("received request: eventIds: {}", request.getEventIdList());

        try {
            List<RecommendedEventProto> similarEvents = recommendationService.getInteractionsCount(request);

            for (RecommendedEventProto event : similarEvents) {
                responseObserver.onNext(event);
            }

            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(new StatusRuntimeException(
                    Status.INTERNAL
                            .withDescription(e.getLocalizedMessage())
                            .withCause(e)
            ));
        }
    }
}
