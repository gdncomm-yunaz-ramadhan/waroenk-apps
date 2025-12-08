package com.gdn.project.waroenk.catalog.service;

import com.gdn.project.waroenk.catalog.FilterVariantRequest;
import com.gdn.project.waroenk.catalog.FindVariantsBySkuRequest;
import com.gdn.project.waroenk.catalog.MultipleVariantResponse;
import com.gdn.project.waroenk.catalog.entity.Product;
import com.gdn.project.waroenk.catalog.entity.Variant;
import com.gdn.project.waroenk.catalog.exceptions.ResourceNotFoundException;
import com.gdn.project.waroenk.catalog.mapper.VariantMapper;
import com.gdn.project.waroenk.catalog.repository.MongoPageAble;
import com.gdn.project.waroenk.catalog.repository.ProductRepository;
import com.gdn.project.waroenk.catalog.repository.VariantRepository;
import com.gdn.project.waroenk.catalog.repository.model.ResultData;
import com.gdn.project.waroenk.catalog.utility.CacheUtil;
import com.gdn.project.waroenk.catalog.utility.ParserUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class VariantServiceImpl extends MongoPageAble<Variant, String> implements VariantService {
  private static final VariantMapper mapper = VariantMapper.INSTANCE;
  private static final String VARIANT_PREFIX = "variant";
  private final VariantRepository repository;
  private final ProductRepository productRepository;
  private final MongoTemplate mongoTemplate;
  private final CacheUtil<Variant> cacheUtil;

  @Value("${default.item-per-page:10}")
  private Integer defaultItemPerPage;

  public VariantServiceImpl(VariantRepository repository,
      ProductRepository productRepository,
      CacheUtil<Variant> cacheUtil,
      CacheUtil<String> stringCacheUtil,
      MongoTemplate mongoTemplate) {
    super(VARIANT_PREFIX, stringCacheUtil, mongoTemplate, 10, TimeUnit.MINUTES, Variant.class);
    this.repository = repository;
    this.productRepository = productRepository;
    this.mongoTemplate = mongoTemplate;
    this.cacheUtil = cacheUtil;
  }

  @Override
  public Variant createVariant(Variant variant) {
    // Auto-generate subSku if not provided
    if (StringUtils.isBlank(variant.getSubSku())) {
      variant.setSubSku(variant.getSku() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    }
    
    // If this is the first variant or marked as default, handle default setting
    if (variant.getIsDefault() != null && variant.getIsDefault()) {
      // Unset isDefault on other variants with the same SKU
      unsetDefaultForSku(variant.getSku());
    } else {
      // Check if there are any existing variants for this SKU
      boolean hasExistingVariants = repository.existsBySku(variant.getSku());
      if (!hasExistingVariants) {
        // First variant should be default
        variant.setIsDefault(true);
      }
    }

    Variant saved = repository.save(variant);
    cacheUtil.putValue(VARIANT_PREFIX + ":" + saved.getId(), saved, 7, TimeUnit.DAYS);
    cacheUtil.putValue(VARIANT_PREFIX + ":subSku:" + saved.getSubSku(), saved, 7, TimeUnit.DAYS);
    
    // If this is the default variant, update the product
    if (saved.getIsDefault() != null && saved.getIsDefault()) {
      updateProductFromVariant(saved);
    }
    
    return saved;
  }

  @Override
  public Variant updateVariant(String id, Variant variant) {
    Variant existing = findVariantById(id);

    if (variant.getSku() != null) existing.setSku(variant.getSku());
    if (variant.getTitle() != null) existing.setTitle(variant.getTitle());
    if (variant.getPrice() != null) existing.setPrice(variant.getPrice());
    if (variant.getAttributes() != null) existing.setAttributes(variant.getAttributes());
    if (variant.getThumbnail() != null) existing.setThumbnail(variant.getThumbnail());
    if (variant.getMedia() != null) existing.setMedia(variant.getMedia());
    
    // Handle isDefault change
    if (variant.getIsDefault() != null && variant.getIsDefault() && !Boolean.TRUE.equals(existing.getIsDefault())) {
      unsetDefaultForSku(existing.getSku());
      existing.setIsDefault(true);
    }

    Variant updated = repository.save(existing);
    cacheUtil.putValue(VARIANT_PREFIX + ":" + updated.getId(), updated, 7, TimeUnit.DAYS);
    cacheUtil.putValue(VARIANT_PREFIX + ":subSku:" + updated.getSubSku(), updated, 7, TimeUnit.DAYS);
    
    // If this is the default variant, update the product
    if (updated.getIsDefault() != null && updated.getIsDefault()) {
      updateProductFromVariant(updated);
    }
    
    return updated;
  }

  @Override
  @Transactional
  public Variant setDefaultVariant(String variantId) {
    Variant variant = findVariantById(variantId);
    
    // Unset isDefault on all other variants with the same SKU
    unsetDefaultForSku(variant.getSku());
    
    // Set this variant as default
    variant.setIsDefault(true);
    Variant updated = repository.save(variant);
    
    // Update cache
    cacheUtil.putValue(VARIANT_PREFIX + ":" + updated.getId(), updated, 7, TimeUnit.DAYS);
    cacheUtil.putValue(VARIANT_PREFIX + ":subSku:" + updated.getSubSku(), updated, 7, TimeUnit.DAYS);
    
    // Update the parent product with this variant's title and create summary from attributes
    updateProductFromVariant(updated);
    
    return updated;
  }

  @Override
  public Variant findDefaultVariantBySku(String sku) {
    return repository.findBySkuAndIsDefault(sku, true)
        .orElse(null);
  }

  private void unsetDefaultForSku(String sku) {
    Query query = new Query(Criteria.where("sku").is(sku).and("isDefault").is(true));
    Update update = new Update().set("isDefault", false);
    mongoTemplate.updateMulti(query, update, Variant.class);
  }

  private void updateProductFromVariant(Variant variant) {
    productRepository.findBySku(variant.getSku()).ifPresent(product -> {
      if (variant.getTitle() != null) {
        product.setTitle(variant.getTitle());
      }
      
      // Update summary with variant info
      if (product.getSummary() == null) {
        product.setSummary(new Product.ProductSummary());
      }
      
      // Create a short description from attributes if available
      if (variant.getAttributes() != null && !variant.getAttributes().isEmpty()) {
        StringBuilder desc = new StringBuilder();
        variant.getAttributes().forEach((key, value) -> {
          if (desc.length() > 0) desc.append(", ");
          desc.append(key).append(": ").append(value);
        });
        if (desc.length() > 0) {
          product.getSummary().setShortDescription(desc.toString());
        }
      }
      
      productRepository.save(product);
      log.info("Updated product {} with default variant {} info", product.getId(), variant.getId());
    });
  }

  @Override
  public Variant findVariantById(String id) {
    String key = VARIANT_PREFIX + ":" + id;
    Variant cached = cacheUtil.getValue(key);
    if (ObjectUtils.isNotEmpty(cached)) {
      return cached;
    }

    Variant variant = repository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Variant with id " + id + " not found"));
    cacheUtil.putValue(key, variant, 7, TimeUnit.DAYS);
    return variant;
  }

  @Override
  public Variant findVariantBySubSku(String subSku) {
    String key = VARIANT_PREFIX + ":subSku:" + subSku;
    Variant cached = cacheUtil.getValue(key);
    if (ObjectUtils.isNotEmpty(cached)) {
      return cached;
    }

    Variant variant = repository.findBySubSku(subSku)
        .orElseThrow(() -> new ResourceNotFoundException("Variant with subSku " + subSku + " not found"));
    cacheUtil.putValue(key, variant, 7, TimeUnit.DAYS);
    return variant;
  }

  @Override
  public MultipleVariantResponse findVariantsBySku(FindVariantsBySkuRequest request) {
    int size = request.getSize() > 0 ? request.getSize() : defaultItemPerPage;

    CriteriaBuilder criteriaBuilder = () -> {
      List<Criteria> criteriaList = new ArrayList<>();
      criteriaList.add(Criteria.where("sku").is(request.getSku()));
      return criteriaList;
    };

    ResultData<Variant> entries = query(criteriaBuilder, size, request.getCursor(),
        mapper.toSortByDto(request.getSortBy()));
    Long total = entries.getTotal();
    String nextToken = null;
    Optional<Variant> offset = entries.getOffset();
    if (offset.isPresent()) {
      nextToken = ParserUtil.encodeBase64(offset.get().getId());
    }

    MultipleVariantResponse.Builder builder = MultipleVariantResponse.newBuilder();
    entries.getDataList().iterator().forEachRemaining(item -> builder.addData(mapper.toResponseGrpc(item)));
    if (StringUtils.isNotBlank(nextToken)) {
      builder.setNextToken(nextToken);
    }
    builder.setTotal(ObjectUtils.isNotEmpty(total) ? total.intValue() : 0);
    return builder.build();
  }

  @Override
  public boolean deleteVariant(String id) {
    Variant existing = findVariantById(id);
    repository.deleteById(id);
    cacheUtil.removeValue(VARIANT_PREFIX + ":" + id);
    cacheUtil.removeValue(VARIANT_PREFIX + ":subSku:" + existing.getSubSku());
    return true;
  }

  @Override
  public MultipleVariantResponse filterVariants(FilterVariantRequest request) {
    int size = request.getSize() > 0 ? request.getSize() : defaultItemPerPage;

    CriteriaBuilder criteriaBuilder = () -> {
      List<Criteria> criteriaList = new ArrayList<>();
      if (StringUtils.isNotBlank(request.getSku())) {
        criteriaList.add(Criteria.where("sku").regex(request.getSku(), "i"));
      }
      if (StringUtils.isNotBlank(request.getSubSku())) {
        criteriaList.add(Criteria.where("subSku").regex(request.getSubSku(), "i"));
      }
      if (ObjectUtils.isNotEmpty(request.getMinPrice())) {
        criteriaList.add(Criteria.where("price").gte(request.getMinPrice()));
      }
      if (ObjectUtils.isNotEmpty(request.getMaxPrice())) {
        criteriaList.add(Criteria.where("price").lte(request.getMaxPrice()));
      }
      return criteriaList;
    };

    ResultData<Variant> entries = query(criteriaBuilder, size, request.getCursor(),
        mapper.toSortByDto(request.getSortBy()));
    Long total = entries.getTotal();
    String nextToken = null;
    Optional<Variant> offset = entries.getOffset();
    if (offset.isPresent()) {
      nextToken = ParserUtil.encodeBase64(offset.get().getId());
    }

    MultipleVariantResponse.Builder builder = MultipleVariantResponse.newBuilder();
    entries.getDataList().iterator().forEachRemaining(item -> builder.addData(mapper.toResponseGrpc(item)));
    if (StringUtils.isNotBlank(nextToken)) {
      builder.setNextToken(nextToken);
    }
    builder.setTotal(ObjectUtils.isNotEmpty(total) ? total.intValue() : 0);
    return builder.build();
  }

  @Override
  protected String toId(String input) {
    return input;
  }

  @Override
  protected String getId(Variant input) {
    return input.getId();
  }

  @Override
  protected String getIdFieldName() {
    return "_id";
  }
}
