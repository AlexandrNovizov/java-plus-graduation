package ru.practicum.feign.fallback;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.feign.CategoryFeignClient;
import ru.practicum.feign.exception.CategoryServiceUnavailableException;

import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class CategoryClientFallbackFactory implements FallbackFactory<CategoryFeignClient> {

    @Override
    public CategoryFeignClient create(Throwable cause) {
        return new CategoryFeignClient() {
            @Override
            public CategoryDto findById(Long categoryId) {
                log.warn("category-service unavailable while requesting category id={}", categoryId, cause);
                throw new CategoryServiceUnavailableException(
                        "category-service unavailable while requesting category id=" + categoryId, cause
                );
            }

            @Override
            public List<CategoryDto> findByIds(Set<Long> categoryIds) {
                log.warn("category-service unavailable while requesting categories with ids={}", categoryIds, cause);
                return List.of();
            }
        };
    }
}
