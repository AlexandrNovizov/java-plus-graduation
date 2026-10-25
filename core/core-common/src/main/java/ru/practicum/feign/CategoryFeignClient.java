package ru.practicum.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import ru.practicum.dto.category.CategoryDto;

import java.util.List;
import java.util.Set;

@FeignClient(name = "category-service")
public interface CategoryFeignClient {

    @GetMapping("/categories/{categoryId}")
    @ResponseStatus(HttpStatus.OK)
    CategoryDto findById(@PathVariable Long categoryId);

    @GetMapping("/internal/categories")
    @ResponseStatus(HttpStatus.OK)
    List<CategoryDto> findByIds(@RequestParam(name = "ids") Set<Long> categoryIds);
}
