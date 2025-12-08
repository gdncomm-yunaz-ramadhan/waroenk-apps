package com.gdn.project.waroenk.catalog.service;

import com.gdn.project.waroenk.catalog.FilterProductRequest;
import com.gdn.project.waroenk.catalog.MultipleProductResponse;
import com.gdn.project.waroenk.catalog.dto.product.AggregatedProductDto;
import com.gdn.project.waroenk.catalog.entity.Product;
import com.gdn.project.waroenk.catalog.exceptions.DuplicateResourceException;
import com.gdn.project.waroenk.catalog.exceptions.ResourceNotFoundException;
import com.gdn.project.waroenk.catalog.mapper.ProductMapper;
import com.gdn.project.waroenk.catalog.repository.MongoPageAble;
import com.gdn.project.waroenk.catalog.repository.ProductRepository;
import com.gdn.project.waroenk.catalog.repository.model.ResultData;
import com.gdn.project.waroenk.catalog.utility.CacheUtil;
import com.gdn.project.waroenk.catalog.utility.ParserUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ProductServiceImpl extends MongoPageAble<Product, String> implements ProductService {
  private static final ProductMapper mapper = ProductMapper.INSTANCE;
  private static final String PRODUCT_PREFIX = "product";
  private final ProductRepository repository;
  private final CacheUtil<Product> cacheUtil;
  private final SearchService searchService;

  @Value("${default.item-per-page:10}")
  private Integer defaultItemPerPage;

  public ProductServiceImpl(ProductRepository repository,
      CacheUtil<Product> cacheUtil,
      CacheUtil<String> stringCacheUtil,
      MongoTemplate mongoTemplate,
      @Lazy SearchService searchService) {
    super(PRODUCT_PREFIX, stringCacheUtil, mongoTemplate, 10, TimeUnit.MINUTES, Product.class);
    this.repository = repository;
    this.cacheUtil = cacheUtil;
    this.searchService = searchService;
  }

  @Override
  public Product createProduct(Product product) {
    if (repository.existsBySku(product.getSku())) {
      throw new DuplicateResourceException("Product with SKU " + product.getSku() + " already exists");
    }
    Product saved = repository.save(product);
    cacheUtil.putValue(PRODUCT_PREFIX + ":" + saved.getId(), saved, 7, TimeUnit.DAYS);
    cacheUtil.putValue(PRODUCT_PREFIX + ":sku:" + saved.getSku(), saved, 7, TimeUnit.DAYS);
    
    // Index to TypeSense asynchronously
    indexProductAsync(saved.getSku());
    
    return saved;
  }

  @Override
  public Product updateProduct(String id, Product product) {
    Product existing = findProductById(id);

    if (product.getTitle() != null) existing.setTitle(product.getTitle());
    if (product.getSku() != null && !existing.getSku().equals(product.getSku())) {
      if (repository.existsBySku(product.getSku())) {
        throw new DuplicateResourceException("Product with SKU " + product.getSku() + " already exists");
      }
      cacheUtil.removeValue(PRODUCT_PREFIX + ":sku:" + existing.getSku());
      existing.setSku(product.getSku());
    }
    if (product.getMerchantCode() != null) existing.setMerchantCode(product.getMerchantCode());
    if (product.getCategoryId() != null) existing.setCategoryId(product.getCategoryId());
    if (product.getBrandId() != null) existing.setBrandId(product.getBrandId());
    if (product.getSummary() != null) existing.setSummary(product.getSummary());
    if (product.getDetailRef() != null) existing.setDetailRef(product.getDetailRef());

    Product updated = repository.save(existing);
    cacheUtil.putValue(PRODUCT_PREFIX + ":" + updated.getId(), updated, 7, TimeUnit.DAYS);
    cacheUtil.putValue(PRODUCT_PREFIX + ":sku:" + updated.getSku(), updated, 7, TimeUnit.DAYS);
    
    // Update TypeSense index asynchronously
    indexProductAsync(updated.getSku());
    
    return updated;
  }

  @Override
  public Product findProductById(String id) {
    String key = PRODUCT_PREFIX + ":" + id;
    Product cached = cacheUtil.getValue(key);
    if (ObjectUtils.isNotEmpty(cached)) {
      return cached;
    }

    Product product = repository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Product with id " + id + " not found"));
    cacheUtil.putValue(key, product, 7, TimeUnit.DAYS);
    return product;
  }

  @Override
  public Product findProductBySku(String sku) {
    String key = PRODUCT_PREFIX + ":sku:" + sku;
    Product cached = cacheUtil.getValue(key);
    if (ObjectUtils.isNotEmpty(cached)) {
      return cached;
    }

    // First try to find by exact SKU
    var product = repository.findBySku(sku);
    
    // If not found and SKU has variant suffix (e.g., MCH00100-01000-V1), try base SKU
    if (product.isEmpty() && sku.matches(".*-V\\d+$")) {
      String baseSku = sku.replaceAll("-V\\d+$", "");
      product = repository.findBySku(baseSku);
    }
    
    Product found = product
        .orElseThrow(() -> new ResourceNotFoundException("Product with SKU " + sku + " not found"));
    cacheUtil.putValue(key, found, 7, TimeUnit.DAYS);
    return found;
  }

  @Override
  public boolean deleteProduct(String id) {
    Product existing = findProductById(id);
    repository.deleteById(id);
    cacheUtil.removeValue(PRODUCT_PREFIX + ":" + id);
    cacheUtil.removeValue(PRODUCT_PREFIX + ":sku:" + existing.getSku());
    
    // Remove from TypeSense index
    try {
      searchService.deleteProductIndex(id);
    } catch (Exception e) {
      log.warn("Failed to delete product {} from search index: {}", id, e.getMessage());
    }
    
    return true;
  }

  @Override
  public MultipleProductResponse filterProducts(FilterProductRequest request) {
    int size = request.getSize() > 0 ? request.getSize() : defaultItemPerPage;

    CriteriaBuilder criteriaBuilder = () -> {
      List<Criteria> criteriaList = new ArrayList<>();
      if (StringUtils.isNotBlank(request.getTitle())) {
        criteriaList.add(Criteria.where("title").regex(request.getTitle(), "i"));
      }
      if (StringUtils.isNotBlank(request.getSku())) {
        criteriaList.add(Criteria.where("sku").regex(request.getSku(), "i"));
      }
      if (StringUtils.isNotBlank(request.getMerchantCode())) {
        criteriaList.add(Criteria.where("merchantCode").is(request.getMerchantCode()));
      }
      if (StringUtils.isNotBlank(request.getCategoryId())) {
        criteriaList.add(Criteria.where("categoryId").is(request.getCategoryId()));
      }
      if (StringUtils.isNotBlank(request.getBrandId())) {
        criteriaList.add(Criteria.where("brandId").is(request.getBrandId()));
      }
      return criteriaList;
    };

    ResultData<Product> entries = query(criteriaBuilder, size, request.getCursor(),
        mapper.toSortByDto(request.getSortBy()));
    Long total = entries.getTotal();
    String nextToken = null;
    Optional<Product> offset = entries.getOffset();
    if (offset.isPresent()) {
      nextToken = ParserUtil.encodeBase64(offset.get().getId());
    }

    MultipleProductResponse.Builder builder = MultipleProductResponse.newBuilder();
    entries.getDataList().iterator().forEachRemaining(item -> builder.addData(mapper.toResponseGrpc(item)));
    if (StringUtils.isNotBlank(nextToken)) {
      builder.setNextToken(nextToken);
    }
    builder.setTotal(ObjectUtils.isNotEmpty(total) ? total.intValue() : 0);
    return builder.build();
  }

  @Override
  public List<Product> findAllProducts() {
    return repository.findAll();
  }

  private void indexProductAsync(String sku) {
    try {
      List<AggregatedProductDto> aggregated = searchService.buildAggregatedProduct(sku);
      if (aggregated != null) {
        aggregated.forEach(product -> {
          searchService.indexProduct(product);
          log.debug("Indexed product {} to search", product.subSku());
        });
      }
    } catch (Exception e) {
      log.warn("Failed to index product {} to search: {}", sku, e.getMessage());
    }
  }

  @Override
  protected String toId(String input) {
    return input;
  }

  @Override
  protected String getId(Product input) {
    return input.getId();
  }

  @Override
  protected String getIdFieldName() {
    return "_id";
  }
}
