package ru.practicum.controller.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.entity.category.CategoryService;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping(path = "/internal/categories")
@RequiredArgsConstructor
public class CategoryInternalController {

    private final CategoryService categoryService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<CategoryDto> getByIds(@RequestParam Set<Long> ids) {
        return categoryService.findByIds(ids);
    }

}
