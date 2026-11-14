package ru.practicum.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.grpc.UserActionProto;
import ru.practicum.kafka.KafkaProducer;
import ru.practicum.mapper.ProtoToAvroMapper;

import java.time.Instant;

@RequiredArgsConstructor
@Component
@Slf4j
public class UserActionHandler {

    protected final KafkaProducer producer;

    public void handle(UserActionProto action) {

        UserActionAvro avro = mapToAvro(action);

        producer.send(avro);
    }

    private UserActionAvro mapToAvro(UserActionProto proto) {
        return UserActionAvro.newBuilder()
                .setUserId(proto.getUserId())
                .setEventId(proto.getEventId())
                .setActionType(ProtoToAvroMapper.mapToAvro(proto.getActionType()))
                .setTimestamp(Instant.ofEpochSecond(
                        proto.getTimestamp().getSeconds(),
                        proto.getTimestamp().getNanos()
                ))
                .build();
    }
}
