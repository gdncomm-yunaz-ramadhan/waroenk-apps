package com.gdn.project.waroenk.catalog.service;

import com.gdn.project.waroenk.catalog.FilterMerchantRequest;
import com.gdn.project.waroenk.catalog.MultipleMerchantResponse;
import com.gdn.project.waroenk.catalog.entity.Merchant;
import com.gdn.project.waroenk.catalog.exceptions.DuplicateResourceException;
import com.gdn.project.waroenk.catalog.exceptions.ResourceNotFoundException;
import com.gdn.project.waroenk.catalog.mapper.MerchantMapper;
import com.gdn.project.waroenk.catalog.repository.MerchantRepository;
import com.gdn.project.waroenk.catalog.repository.MongoPageAble;
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
public class MerchantServiceImpl extends MongoPageAble<Merchant, String> implements MerchantService {
  private static final MerchantMapper mapper = MerchantMapper.INSTANCE;
  private static final String MERCHANT_PREFIX = "merchant";
  private final MerchantRepository repository;
  private final CacheUtil<Merchant> cacheUtil;
  private final SearchService searchService;

  @Value("${default.item-per-page:10}")
  private Integer defaultItemPerPage;

  public MerchantServiceImpl(MerchantRepository repository,
      CacheUtil<Merchant> cacheUtil,
      CacheUtil<String> stringCacheUtil,
      MongoTemplate mongoTemplate,
      @Lazy SearchService searchService) {
    super(MERCHANT_PREFIX, stringCacheUtil, mongoTemplate, 10, TimeUnit.MINUTES, Merchant.class);
    this.repository = repository;
    this.cacheUtil = cacheUtil;
    this.searchService = searchService;
  }

  @Override
  public Merchant createMerchant(Merchant merchant) {
    if (repository.existsByCode(merchant.getCode())) {
      throw new DuplicateResourceException("Merchant with code " + merchant.getCode() + " already exists");
    }
    Merchant saved = repository.save(merchant);
    cacheUtil.putValue(MERCHANT_PREFIX + ":" + saved.getId(), saved, 7, TimeUnit.DAYS);
    cacheUtil.putValue(MERCHANT_PREFIX + ":code:" + saved.getCode(), saved, 7, TimeUnit.DAYS);
    
    // Index to TypeSense
    indexMerchantAsync(saved);
    
    return saved;
  }

  @Override
  public Merchant updateMerchant(String id, Merchant merchant) {
    Merchant existing = findMerchantById(id);

    if (merchant.getName() != null) existing.setName(merchant.getName());
    if (merchant.getCode() != null && !existing.getCode().equals(merchant.getCode())) {
      if (repository.existsByCode(merchant.getCode())) {
        throw new DuplicateResourceException("Merchant with code " + merchant.getCode() + " already exists");
      }
      cacheUtil.removeValue(MERCHANT_PREFIX + ":code:" + existing.getCode());
      existing.setCode(merchant.getCode());
    }
    if (merchant.getIconUrl() != null) existing.setIconUrl(merchant.getIconUrl());
    if (merchant.getLocation() != null) existing.setLocation(merchant.getLocation());
    if (merchant.getContact() != null) existing.setContact(merchant.getContact());
    if (merchant.getRating() != null) existing.setRating(merchant.getRating());

    Merchant updated = repository.save(existing);
    cacheUtil.putValue(MERCHANT_PREFIX + ":" + updated.getId(), updated, 7, TimeUnit.DAYS);
    cacheUtil.putValue(MERCHANT_PREFIX + ":code:" + updated.getCode(), updated, 7, TimeUnit.DAYS);
    
    // Update TypeSense index
    indexMerchantAsync(updated);
    
    return updated;
  }

  @Override
  public Merchant findMerchantById(String id) {
    String key = MERCHANT_PREFIX + ":" + id;
    Merchant cached = cacheUtil.getValue(key);
    if (ObjectUtils.isNotEmpty(cached)) {
      return cached;
    }

    Merchant merchant = repository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Merchant with id " + id + " not found"));
    cacheUtil.putValue(key, merchant, 7, TimeUnit.DAYS);
    return merchant;
  }

  @Override
  public Merchant findMerchantByCode(String code) {
    String key = MERCHANT_PREFIX + ":code:" + code;
    Merchant cached = cacheUtil.getValue(key);
    if (ObjectUtils.isNotEmpty(cached)) {
      return cached;
    }

    Merchant merchant = repository.findByCode(code)
        .orElseThrow(() -> new ResourceNotFoundException("Merchant with code " + code + " not found"));
    cacheUtil.putValue(key, merchant, 7, TimeUnit.DAYS);
    return merchant;
  }

  @Override
  public boolean deleteMerchant(String id) {
    Merchant existing = findMerchantById(id);
    repository.deleteById(id);
    cacheUtil.removeValue(MERCHANT_PREFIX + ":" + id);
    cacheUtil.removeValue(MERCHANT_PREFIX + ":code:" + existing.getCode());
    
    // Remove from TypeSense index
    try {
      searchService.deleteMerchantIndex(id);
    } catch (Exception e) {
      log.warn("Failed to delete merchant {} from search index: {}", id, e.getMessage());
    }
    
    return true;
  }

  @Override
  public MultipleMerchantResponse filterMerchants(FilterMerchantRequest request) {
    int size = request.getSize() > 0 ? request.getSize() : defaultItemPerPage;

    CriteriaBuilder criteriaBuilder = () -> {
      List<Criteria> criteriaList = new ArrayList<>();
      if (StringUtils.isNotBlank(request.getName())) {
        criteriaList.add(Criteria.where("name").regex(request.getName(), "i"));
      }
      if (StringUtils.isNotBlank(request.getCode())) {
        criteriaList.add(Criteria.where("code").regex(request.getCode(), "i"));
      }
      return criteriaList;
    };

    ResultData<Merchant> entries = query(criteriaBuilder, size, request.getCursor(),
        mapper.toSortByDto(request.getSortBy()));
    Long total = entries.getTotal();
    String nextToken = null;
    Optional<Merchant> offset = entries.getOffset();
    if (offset.isPresent()) {
      nextToken = ParserUtil.encodeBase64(offset.get().getId());
    }

    MultipleMerchantResponse.Builder builder = MultipleMerchantResponse.newBuilder();
    entries.getDataList().iterator().forEachRemaining(item -> builder.addData(mapper.toResponseGrpc(item)));
    if (StringUtils.isNotBlank(nextToken)) {
      builder.setNextToken(nextToken);
    }
    builder.setTotal(ObjectUtils.isNotEmpty(total) ? total.intValue() : 0);
    return builder.build();
  }

  @Override
  public List<Merchant> findAllMerchants() {
    return repository.findAll();
  }

  private void indexMerchantAsync(Merchant merchant) {
    try {
      searchService.indexMerchant(merchant);
      log.debug("Indexed merchant {} to search", merchant.getId());
    } catch (Exception e) {
      log.warn("Failed to index merchant {} to search: {}", merchant.getId(), e.getMessage());
    }
  }

  @Override
  protected String toId(String input) {
    return input;
  }

  @Override
  protected String getId(Merchant input) {
    return input.getId();
  }

  @Override
  protected String getIdFieldName() {
    return "_id";
  }
}
