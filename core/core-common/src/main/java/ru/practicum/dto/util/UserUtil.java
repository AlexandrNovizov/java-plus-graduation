package ru.practicum.dto.util;

import lombok.experimental.UtilityClass;
import ru.practicum.dto.user.UserDto;
import ru.practicum.dto.user.UserFullDto;
import ru.practicum.dto.user.UserParamDto;
import ru.practicum.feign.UserFeignClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@UtilityClass
public class UserUtil {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static boolean isBanned(UserFullDto dto) {
        return dto.getBannedUntil() != null &&
                LocalDateTime.parse(dto.getBannedUntil(), formatter).isAfter(LocalDateTime.now());
    }

    public static boolean isWarningsLimitExceeded(UserFullDto dto) {
        return dto.getAdminWarnings() >= 2;
    }

    public static Map<Long, String> getUserNamesMap(UserFeignClient client, Set<Long> ids) {

        UserParamDto dto = new UserParamDto();
        dto.setIds(ids);
        List<UserDto> users = client.getUsers(dto);

        return users.stream()
                .collect(Collectors.toMap(UserDto::getId, UserDto::getName));
    }
}
