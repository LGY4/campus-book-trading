package com.booktrading.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.booktrading.entity.Category;

import java.util.List;

public interface CategoryService extends IService<Category> {

    List<Category> listAll();

    void createCategory(Category category);

    void updateCategory(Category category);

    void deleteCategory(Long id);
}
