package ru.practicum.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.model.UserActionType;

@UtilityClass
public class UserActionTypeMapper {

    public static UserActionType mapToEntity(ActionTypeAvro avro) {
        switch (avro) {
            case VIEW:
                return UserActionType.VIEW;
            case REGISTER:
                return UserActionType.REGISTER;
            case LIKE:
                return UserActionType.LIKE;
            case null, default:
                throw new IllegalArgumentException("Can't find UserActionType mapper for " +
                        (avro == null ? null : avro.name()));
        }
    }
}
