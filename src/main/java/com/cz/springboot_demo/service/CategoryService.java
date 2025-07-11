package com.cz.springboot_demo.service;

import com.cz.springboot_demo.exception.CategoryAlreadyExistException;
import com.cz.springboot_demo.exception.CategoryLevelIsLowestException;
import com.cz.springboot_demo.exception.CategoryNotFoundException;
import com.cz.springboot_demo.pojo.Category;
import com.cz.springboot_demo.pojo.dto.CategoryCreateDTO;
import com.cz.springboot_demo.pojo.dto.CategoryEditDTO;
import com.cz.springboot_demo.repository.CategoryRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

// Since 2025/5/25 by CZ
@Service
public class CategoryService implements ICategoryService {
    @Autowired
    CategoryRepository categoryRepository;

    @CacheEvict(value = {"categories", "category"}, allEntries = true)
    public Category addCategory(CategoryCreateDTO categoryCreateDTO) {
        if (categoryRepository.findByCategoryName(categoryCreateDTO.getCategoryName()).isPresent()) {
            throw new CategoryAlreadyExistException("Category already exist");
        } else {
            Category category = new Category();
            BeanUtils.copyProperties(categoryCreateDTO, category);
            return categoryRepository.save(category);
        }
    }

    @Override
    @CacheEvict(value = {"categories", "category"}, allEntries = true)
    public void deleteCategory(Long categoryId) {
        if (categoryRepository.findById(categoryId).isPresent()) {
            categoryRepository.deleteById(categoryId);
        } else {
            throw new CategoryNotFoundException("Category not exist");
        }
    }

    @Override
    @CacheEvict(value = {"categories", "category"}, allEntries = true)
    public Category updateCategory(Long categoryId, CategoryEditDTO categoryEditDTO) {
//        Optional<Category> optionalCategory = categoryRepository.findByCategoryName(categoryEditDTO.getCategoryName());
        Optional<Category> optionalCategory = categoryRepository.findById(categoryId);
        if (!optionalCategory.isPresent()) {
            throw new CategoryNotFoundException("Category Not Found");
        }

        Category category = optionalCategory.get();

        // ✅ 更新除 id 之外的属性（如果你不想覆盖某些字段，可以手动更新）
        BeanUtils.copyProperties(categoryEditDTO, category, "id");

        // ✅ 保存更新后的 product
        return categoryRepository.save(category);
    }

    @Override
    @Cacheable(value = "category", key = "#categoryId")
    public Category getCategoryByCategoryId(Long categoryId) {
        return categoryRepository.findById(categoryId).orElseThrow(() -> new CategoryNotFoundException("Category not exist"));
    }

    @Override
    @Cacheable(value = "category", key = "'name:' + #categoryName")
    public Category getCategoryByCategoryName(String categoryName) {
        return categoryRepository.findByCategoryName(categoryName).orElseThrow(() -> new CategoryNotFoundException("Category not exist"));
    }

    @Override
    @Cacheable(value = "categories", key = "'level:0'")
    public List<Category> getFirstClassCategory() {
        return categoryRepository.findByCategoryLevel(0);
    }

    @Override
    @Cacheable(value = "categories", key = "'level:1'")
    public List<Category> getSecondClassCategory() {
        return categoryRepository.findByCategoryLevel(1);
    }

    @Override
    @Cacheable(value = "categories", key = "'level:2'")
    public List<Category> getThirdClassCategory() {
        return categoryRepository.findByCategoryLevel(2);
    }

    @Override
    @Cacheable(value = "categories", key = "'all'")
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    @Override
    @Cacheable(value = "categories", key = "'children:' + #categoryId")
    public List<Category> getChildById(Long categoryId) {
        if (!categoryRepository.findById(categoryId).isPresent()) {
            throw new CategoryNotFoundException("Category not exist");
        } else {
            Category targetCategory = categoryRepository.findById(categoryId).get();
            if (targetCategory.getCategoryLevel() == 2) {
                throw new CategoryLevelIsLowestException("Target Category is lowest");
            } else {
                return categoryRepository.findByCategoryParentId(targetCategory.getCategoryId());
            }
        }
    }
}
