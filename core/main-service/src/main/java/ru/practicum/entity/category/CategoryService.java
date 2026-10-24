package ru.practicum.entity.category;

import ru.practicum.dto.category.CategoriesParamDto;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.dto.category.NewCategoryDto;

import java.util.List;
import java.util.Set;

public interface CategoryService {
    CategoryDto create(NewCategoryDto payload);

    void delete(long id);

    CategoryDto update(long id, CategoryDto payload);

    List<CategoryDto> findAll(CategoriesParamDto params);

    CategoryDto findById(long id);

    // Feign
    List<CategoryDto> findByIds(Set<Long> ids);
}

