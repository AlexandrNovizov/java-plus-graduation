package ru.practicum.dto.util;

import lombok.experimental.UtilityClass;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.feign.CategoryFeignClient;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@UtilityClass
public class CategoryUtil {

    public static Map<Long, String> getCategoryNamesMap(CategoryFeignClient client, Set<Long> ids) {

        if (ids.isEmpty()) {
            return Map.of();
        }

        List<CategoryDto> categories = client.findByIds(ids);

        return categories.stream()
                .collect(Collectors.toMap(CategoryDto::getId, CategoryDto::getName));
    }
}
