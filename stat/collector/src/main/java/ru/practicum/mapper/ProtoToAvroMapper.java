package ru.practicum.mapper;

import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.grpc.ActionTypeProto;

public class ProtoToAvroMapper {

    public static ActionTypeAvro mapToAvro(ActionTypeProto proto) {
        switch (proto) {
            case ACTION_LIKE:
                return ActionTypeAvro.LIKE;
            case ACTION_VIEW:
                return ActionTypeAvro.VIEW;
            case ACTION_REGISTER:
                return ActionTypeAvro.REGISTER;
            case null, default:
                throw new IllegalArgumentException("Can't find ActionTypeAvro mapper for " +
                        (proto == null ? null : proto.name())
                );
        }
    }
}
