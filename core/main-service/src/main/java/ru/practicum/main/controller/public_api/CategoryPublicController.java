package ru.practicum.main.controller.public_api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.main.dto.category.CategoriesParamDto;
import ru.practicum.main.dto.category.CategoryDto;
import ru.practicum.main.entity.category.CategoryService;

import java.util.List;

@RestController
@RequestMapping(path = "/categories")
@RequiredArgsConstructor
@Slf4j
@Validated
public class CategoryPublicController {
    private final CategoryService categoryService;

    @GetMapping
    public List<CategoryDto> findAll(@ModelAttribute @Valid CategoriesParamDto params) {
        return categoryService.findAll(params);
    }

    @GetMapping("/{id}")
    public CategoryDto findById(@Positive @PathVariable long id) {
        return categoryService.findById(id);
    }
}
