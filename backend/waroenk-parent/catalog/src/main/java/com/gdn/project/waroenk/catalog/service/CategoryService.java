package com.gdn.project.waroenk.catalog.service;

import com.gdn.project.waroenk.catalog.CategoryNode;
import com.gdn.project.waroenk.catalog.CategoryTreeResponse;
import com.gdn.project.waroenk.catalog.FilterCategoryRequest;
import com.gdn.project.waroenk.catalog.MultipleCategoryResponse;
import com.gdn.project.waroenk.catalog.entity.Category;

public interface CategoryService {
  Category createCategory(Category category);
  Category updateCategory(String id, Category category);
  Category findCategoryById(String id);
  Category findCategoryBySlug(String slug);
  boolean deleteCategory(String id);
  MultipleCategoryResponse filterCategories(FilterCategoryRequest request);
  CategoryTreeResponse getCategoryTree();
  CategoryNode getCategoryNodes(Category category);
}










