package com.booktrading.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.booktrading.entity.Book;
import com.booktrading.entity.Category;
import com.booktrading.exception.BusinessException;
import com.booktrading.mapper.CategoryMapper;
import com.booktrading.service.BookService;
import com.booktrading.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    @Autowired
    @Lazy
    private BookService bookService;

    @Override
    public List<Category> listAll() {
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Category::getSortOrder);
        return list(wrapper);
    }

    @Override
    public void createCategory(Category category) {
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Category::getName, category.getName());
        if (count(wrapper) > 0) {
            throw new BusinessException("分类名称已存在");
        }
        save(category);
    }

    @Override
    public void updateCategory(Category category) {
        if (getById(category.getId()) == null) {
            throw new BusinessException("分类不存在");
        }
        LambdaQueryWrapper<Category> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Category::getName, category.getName()).ne(Category::getId, category.getId());
        if (count(wrapper) > 0) {
            throw new BusinessException("分类名称已存在");
        }
        updateById(category);
    }

    @Override
    public void deleteCategory(Long id) {
        if (getById(id) == null) {
            throw new BusinessException("分类不存在");
        }
        long bookCount = bookService.count(new LambdaQueryWrapper<Book>()
                .eq(Book::getCategoryId, id));
        if (bookCount > 0) {
            throw new BusinessException("该分类下有" + bookCount + "本书籍，无法删除");
        }
        removeById(id);
    }
}
