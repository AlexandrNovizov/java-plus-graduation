package ru.practicum;

import com.google.protobuf.Empty;
import com.google.protobuf.Timestamp;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.practicum.grpc.ActionTypeProto;
import ru.practicum.grpc.UserActionControllerGrpc;
import ru.practicum.grpc.UserActionProto;

import java.time.Instant;

@Component
public class CollectorClient {

    @GrpcClient("collector")
    private UserActionControllerGrpc.UserActionControllerBlockingStub client;

    private static final Logger log = LoggerFactory.getLogger(CollectorClient.class);

    public void collectUserAction(long userId, long eventId, ActionTypeProto actionType) {

        Instant now = Instant.now();

        UserActionProto request = UserActionProto.newBuilder()
                .setUserId(userId)
                .setEventId(eventId)
                .setActionType(actionType)
                .setTimestamp(Timestamp.newBuilder()
                        .setSeconds(now.getEpochSecond())
                        .setNanos(now.getNano())
                        .build()
                )
                .build();

        log.info("Отправил данные {}", request);
        Empty empty = client.collectUserAction(request);
        log.info("Получил ответ {}", empty);

    }
}
