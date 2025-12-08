package com.gdn.project.waroenk.catalog.service;

import com.gdn.project.waroenk.catalog.FilterBrandRequest;
import com.gdn.project.waroenk.catalog.MultipleBrandResponse;
import com.gdn.project.waroenk.catalog.entity.Brand;
import com.gdn.project.waroenk.catalog.exceptions.DuplicateResourceException;
import com.gdn.project.waroenk.catalog.exceptions.ResourceNotFoundException;
import com.gdn.project.waroenk.catalog.mapper.BrandMapper;
import com.gdn.project.waroenk.catalog.repository.BrandRepository;
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
public class BrandServiceImpl extends MongoPageAble<Brand, String> implements BrandService {
  private static final BrandMapper mapper = BrandMapper.INSTANCE;
  private static final String BRAND_PREFIX = "brand";
  private final BrandRepository repository;
  private final CacheUtil<Brand> cacheUtil;

  @Value("${default.item-per-page:10}")
  private Integer defaultItemPerPage;

  public BrandServiceImpl(BrandRepository repository,
      CacheUtil<Brand> cacheUtil,
      CacheUtil<String> stringCacheUtil,
      MongoTemplate mongoTemplate) {
    super(BRAND_PREFIX, stringCacheUtil, mongoTemplate, 10, TimeUnit.MINUTES, Brand.class);
    this.repository = repository;
    this.cacheUtil = cacheUtil;
  }

  @Override
  public Brand createBrand(Brand brand) {
    if (repository.existsBySlug(brand.getSlug())) {
      throw new DuplicateResourceException("Brand with slug " + brand.getSlug() + " already exists");
    }
    Brand saved = repository.save(brand);
    cacheUtil.putValue(BRAND_PREFIX + ":" + saved.getId(), saved, 7, TimeUnit.DAYS);
    cacheUtil.putValue(BRAND_PREFIX + ":slug:" + saved.getSlug(), saved, 7, TimeUnit.DAYS);
    return saved;
  }

  @Override
  public Brand updateBrand(String id, Brand brand) {
    Brand existing = findBrandById(id);

    if (brand.getName() != null) existing.setName(brand.getName());
    if (brand.getSlug() != null && !existing.getSlug().equals(brand.getSlug())) {
      if (repository.existsBySlug(brand.getSlug())) {
        throw new DuplicateResourceException("Brand with slug " + brand.getSlug() + " already exists");
      }
      cacheUtil.removeValue(BRAND_PREFIX + ":slug:" + existing.getSlug());
      existing.setSlug(brand.getSlug());
    }

    Brand updated = repository.save(existing);
    cacheUtil.putValue(BRAND_PREFIX + ":" + updated.getId(), updated, 7, TimeUnit.DAYS);
    cacheUtil.putValue(BRAND_PREFIX + ":slug:" + updated.getSlug(), updated, 7, TimeUnit.DAYS);
    return updated;
  }

  @Override
  public Brand findBrandById(String id) {
    String key = BRAND_PREFIX + ":" + id;
    Brand cached = cacheUtil.getValue(key);
    if (ObjectUtils.isNotEmpty(cached)) {
      return cached;
    }

    Brand brand = repository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Brand with id " + id + " not found"));
    cacheUtil.putValue(key, brand, 7, TimeUnit.DAYS);
    return brand;
  }

  @Override
  public Brand findBrandBySlug(String slug) {
    String key = BRAND_PREFIX + ":slug:" + slug;
    Brand cached = cacheUtil.getValue(key);
    if (ObjectUtils.isNotEmpty(cached)) {
      return cached;
    }

    Brand brand = repository.findBySlug(slug)
        .orElseThrow(() -> new ResourceNotFoundException("Brand with slug " + slug + " not found"));
    cacheUtil.putValue(key, brand, 7, TimeUnit.DAYS);
    return brand;
  }

  @Override
  public boolean deleteBrand(String id) {
    Brand existing = findBrandById(id);
    repository.deleteById(id);
    cacheUtil.removeValue(BRAND_PREFIX + ":" + id);
    cacheUtil.removeValue(BRAND_PREFIX + ":slug:" + existing.getSlug());
    return true;
  }

  @Override
  public MultipleBrandResponse filterBrands(FilterBrandRequest request) {
    int size = request.getSize() > 0 ? request.getSize() : defaultItemPerPage;

    CriteriaBuilder criteriaBuilder = () -> {
      List<Criteria> criteriaList = new ArrayList<>();
      if (StringUtils.isNotBlank(request.getName())) {
        criteriaList.add(Criteria.where("name").regex(request.getName(), "i"));
      }
      if (StringUtils.isNotBlank(request.getSlug())) {
        criteriaList.add(Criteria.where("slug").regex(request.getSlug(), "i"));
      }
      return criteriaList;
    };

    ResultData<Brand> entries = query(criteriaBuilder, size, request.getCursor(),
        mapper.toSortByDto(request.getSortBy()));
    Long total = entries.getTotal();
    String nextToken = null;
    Optional<Brand> offset = entries.getOffset();
    if (offset.isPresent()) {
      nextToken = ParserUtil.encodeBase64(offset.get().getId());
    }

    MultipleBrandResponse.Builder builder = MultipleBrandResponse.newBuilder();
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
  protected String getId(Brand input) {
    return input.getId();
  }

  @Override
  protected String getIdFieldName() {
    return "_id";
  }
}






