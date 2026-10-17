package ru.practicum.main.entity.category;

import ru.practicum.main.dto.category.CategoriesParamDto;
import ru.practicum.main.dto.category.CategoryDto;
import ru.practicum.main.dto.category.NewCategoryDto;

import java.util.List;

public interface CategoryService {
    CategoryDto create(NewCategoryDto payload);

    void delete(long id);

    CategoryDto update(long id, CategoryDto payload);

    List<CategoryDto> findAll(CategoriesParamDto params);

    CategoryDto findById(long id);

}

