package com.gdn.project.waroenk.catalog.service;

import com.gdn.project.waroenk.catalog.CategoryNode;
import com.gdn.project.waroenk.catalog.CategoryTreeResponse;
import com.gdn.project.waroenk.catalog.FilterCategoryRequest;
import com.gdn.project.waroenk.catalog.MultipleCategoryResponse;
import com.gdn.project.waroenk.catalog.dto.category.CategoryTreeNodeDto;
import com.gdn.project.waroenk.catalog.entity.Category;
import com.gdn.project.waroenk.catalog.exceptions.DuplicateResourceException;
import com.gdn.project.waroenk.catalog.exceptions.ResourceNotFoundException;
import com.gdn.project.waroenk.catalog.mapper.CategoryMapper;
import com.gdn.project.waroenk.catalog.repository.CategoryRepository;
import com.gdn.project.waroenk.catalog.repository.MongoPageAble;
import com.gdn.project.waroenk.catalog.repository.model.ResultData;
import com.gdn.project.waroenk.catalog.utility.CacheUtil;
import com.gdn.project.waroenk.catalog.utility.ParserUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class CategoryServiceImpl extends MongoPageAble<Category, String> implements CategoryService {
  private static final CategoryMapper mapper = CategoryMapper.INSTANCE;
  private static final String CATEGORY_PREFIX = "category";
  private static final String CATEGORY_NODE_PREFIX = "categoryNodes";
  private final CategoryRepository repository;
  private final CacheUtil<Category> cacheUtil;
  private final CacheUtil<CategoryTreeNodeDto> categoryTreeNodeCacheUtil;

  @Value("${default.item-per-page:10}")
  private Integer defaultItemPerPage;

  public CategoryServiceImpl(CategoryRepository repository,
      CacheUtil<Category> cacheUtil,
      CacheUtil<CategoryTreeNodeDto> categoryTreeNodeCacheUtil,
      CacheUtil<String> stringCacheUtil,
      MongoTemplate mongoTemplate) {
    super(CATEGORY_PREFIX, stringCacheUtil, mongoTemplate, 10, TimeUnit.MINUTES, Category.class);
    this.repository = repository;
    this.cacheUtil = cacheUtil;
    this.categoryTreeNodeCacheUtil = categoryTreeNodeCacheUtil;
  }

  @Override
  public Category createCategory(Category category) {
    if (repository.existsBySlug(category.getSlug())) {
      throw new DuplicateResourceException("Category with slug " + category.getSlug() + " already exists");
    }
    Category saved = repository.save(category);
    cacheUtil.putValue(CATEGORY_PREFIX + ":" + saved.getId(), saved, 7, TimeUnit.DAYS);
    cacheUtil.putValue(CATEGORY_PREFIX + ":slug:" + saved.getSlug(), saved, 7, TimeUnit.DAYS);
    return saved;
  }

  @Override
  public Category updateCategory(String id, Category category) {
    Category existing = findCategoryById(id);

    if (category.getName() != null)
      existing.setName(category.getName());
    if (category.getSlug() != null && !existing.getSlug().equals(category.getSlug())) {
      if (repository.existsBySlug(category.getSlug())) {
        throw new DuplicateResourceException("Category with slug " + category.getSlug() + " already exists");
      }
      cacheUtil.removeValue(CATEGORY_PREFIX + ":slug:" + existing.getSlug());
      existing.setSlug(category.getSlug());
    }
    if (category.getParentId() != null)
      existing.setParentId(category.getParentId());

    Category updated = repository.save(existing);
    cacheUtil.putValue(CATEGORY_PREFIX + ":" + updated.getId(), updated, 7, TimeUnit.DAYS);
    cacheUtil.putValue(CATEGORY_PREFIX + ":slug:" + updated.getSlug(), updated, 7, TimeUnit.DAYS);
    clearCategoryNodeCache(updated);
    return updated;
  }

  @Override
  public Category findCategoryById(String id) {
    String key = CATEGORY_PREFIX + ":" + id;
    Category cached = cacheUtil.getValue(key);
    if (ObjectUtils.isNotEmpty(cached)) {
      return cached;
    }

    Category category = repository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Category with id " + id + " not found"));
    cacheUtil.putValue(key, category, 7, TimeUnit.DAYS);
    return category;
  }

  @Override
  public Category findCategoryBySlug(String slug) {
    String key = CATEGORY_PREFIX + ":slug:" + slug;
    Category cached = cacheUtil.getValue(key);
    if (ObjectUtils.isNotEmpty(cached)) {
      return cached;
    }

    Category category = repository.findBySlug(slug)
        .orElseThrow(() -> new ResourceNotFoundException("Category with slug " + slug + " not found"));
    cacheUtil.putValue(key, category, 7, TimeUnit.DAYS);
    return category;
  }

  @Override
  public boolean deleteCategory(String id) {
    Category existing = findCategoryById(id);
    repository.deleteById(id);
    cacheUtil.removeValue(CATEGORY_PREFIX + ":" + id);
    cacheUtil.removeValue(CATEGORY_PREFIX + ":slug:" + existing.getSlug());
    clearCategoryNodeCache(existing);
    return true;
  }

  @Override
  public MultipleCategoryResponse filterCategories(FilterCategoryRequest request) {
    int size = request.getSize() > 0 ? request.getSize() : defaultItemPerPage;

    CriteriaBuilder criteriaBuilder = () -> {
      List<Criteria> criteriaList = new ArrayList<>();
      if (StringUtils.isNotBlank(request.getName())) {
        criteriaList.add(Criteria.where("name").regex(request.getName(), "i"));
      }
      if (StringUtils.isNotBlank(request.getSlug())) {
        criteriaList.add(Criteria.where("slug").regex(request.getSlug(), "i"));
      }
      if (StringUtils.isNotBlank(request.getParentId())) {
        criteriaList.add(Criteria.where("parentId").is(request.getParentId()));
      }
      return criteriaList;
    };

    ResultData<Category> entries =
        query(criteriaBuilder, size, request.getCursor(), mapper.toSortByDto(request.getSortBy()));
    Long total = entries.getTotal();
    String nextToken = null;
    Optional<Category> offset = entries.getOffset();
    if (offset.isPresent()) {
      nextToken = ParserUtil.encodeBase64(offset.get().getId());
    }

    MultipleCategoryResponse.Builder builder = MultipleCategoryResponse.newBuilder();
    entries.getDataList().iterator().forEachRemaining(item -> builder.addData(mapper.toResponseGrpc(item)));
    if (StringUtils.isNotBlank(nextToken)) {
      builder.setNextToken(nextToken);
    }
    builder.setTotal(ObjectUtils.isNotEmpty(total) ? total.intValue() : 0);
    return builder.build();
  }

  @Override
  public CategoryTreeResponse getCategoryTree() {
    List<Category> rootCategories = repository.findByParentIdIsNull();
    CategoryTreeResponse.Builder builder = CategoryTreeResponse.newBuilder();
    for (Category root : rootCategories) {
      builder.addNodes(getCategoryNodes(root));
    }
    return builder.build();
  }

  @Override
  public CategoryNode getCategoryNodes(Category category) {
    String key = CATEGORY_NODE_PREFIX + ":" + category.getId();
    CategoryNode.Builder nodeBuilder = CategoryNode.newBuilder();
    if (ObjectUtils.isEmpty(category)) {
      return nodeBuilder.build();
    }

    CategoryTreeNodeDto cached = categoryTreeNodeCacheUtil.getValue(key);
    if (ObjectUtils.isNotEmpty(cached)) {
      return mapper.toCategoryNodeGrpc(cached);
    }

    nodeBuilder.setId(category.getId());
    nodeBuilder.setName(category.getName());
    nodeBuilder.setSlug(category.getSlug());

    List<Category> children = repository.findByParentId(category.getId());
    for (Category child : children) {
      nodeBuilder.addChildren(getCategoryNodes(child));
    }

    CategoryNode categoryNode = nodeBuilder.build();
    categoryTreeNodeCacheUtil.putValue(key, mapper.toTreeNodeDto(categoryNode), 7, TimeUnit.DAYS);
    return categoryNode;
  }

  private void clearCategoryNodeCache(Category category) {
    if (ObjectUtils.isEmpty(category)) {
      return;
    }

    CategoryNode nodes = getCategoryNodes(category);
    nodes.getChildrenList().forEach(child -> {
      Category childCategory = findCategoryById(child.getId());
      clearCategoryNodeCache(childCategory);
    });

    String key = CATEGORY_NODE_PREFIX + ":" + nodes.getId();
    categoryTreeNodeCacheUtil.removeValue(key);

    if (ObjectUtils.isNotEmpty(category.getParentId())) {
      String parentKey = CATEGORY_NODE_PREFIX + ":" + category.getParentId();
      categoryTreeNodeCacheUtil.removeValue(parentKey);
    }
  }

  @Override
  protected String toId(String input) {
    return input;
  }

  @Override
  protected String getId(Category input) {
    return input.getId();
  }

  @Override
  protected String getIdFieldName() {
    return "_id";
  }
}






