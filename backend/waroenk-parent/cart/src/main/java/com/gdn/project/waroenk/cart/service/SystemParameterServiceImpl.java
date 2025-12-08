package com.gdn.project.waroenk.cart.service;

import com.gdn.project.waroenk.cart.MultipleSystemParameterRequest;
import com.gdn.project.waroenk.cart.MultipleSystemParameterResponse;
import com.gdn.project.waroenk.cart.entity.SystemParameter;
import com.gdn.project.waroenk.cart.exceptions.ResourceNotFoundException;
import com.gdn.project.waroenk.cart.mapper.SystemParameterMapper;
import com.gdn.project.waroenk.cart.repository.MongoPageAble;
import com.gdn.project.waroenk.cart.repository.SystemParameterRepository;
import com.gdn.project.waroenk.cart.repository.model.ResultData;
import com.gdn.project.waroenk.cart.utility.CacheUtil;
import com.gdn.project.waroenk.cart.utility.ParserUtil;
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
public class SystemParameterServiceImpl extends MongoPageAble<SystemParameter, String>
    implements SystemParameterService {

  private static final SystemParameterMapper mapper = SystemParameterMapper.INSTANCE;
  private static final String SYSPARAM_PREFIX = "sysparam";
  private final SystemParameterRepository repository;
  private final CacheUtil<SystemParameter> cacheUtil;

  @Value("${default.item-per-page:10}")
  private Integer defaultItemPerPage;

  public SystemParameterServiceImpl(SystemParameterRepository repository,
      CacheUtil<SystemParameter> cacheUtil,
      CacheUtil<String> stringCacheUtil,
      MongoTemplate mongoTemplate) {
    super(SYSPARAM_PREFIX, stringCacheUtil, mongoTemplate, 10, TimeUnit.MINUTES, SystemParameter.class);
    this.repository = repository;
    this.cacheUtil = cacheUtil;
  }

  @Override
  public SystemParameter upsert(SystemParameter param) {
    Optional<SystemParameter> existing = repository.findByVariable(param.getVariable());

    if (existing.isPresent()) {
      // Update existing
      SystemParameter entity = existing.get();
      entity.setData(param.getData());
      if (param.getDescription() != null) {
        entity.setDescription(param.getDescription());
      }
      if (param.getType() != null) {
        entity.setType(param.getType());
      }
      SystemParameter saved = repository.save(entity);
      updateCache(saved);
      return saved;
    } else {
      // Create new
      SystemParameter saved = repository.save(param);
      updateCache(saved);
      return saved;
    }
  }

  @Override
  public SystemParameter get(String variable) {
    String key = SYSPARAM_PREFIX + ":" + variable;
    SystemParameter cached = cacheUtil.getValue(key);
    if (ObjectUtils.isNotEmpty(cached)) {
      return cached;
    }

    SystemParameter param = repository.findByVariable(variable)
        .orElseThrow(() -> new ResourceNotFoundException("System parameter '" + variable + "' not found"));

    cacheUtil.putValue(key, param, 30, TimeUnit.MINUTES);
    return param;
  }

  @Override
  public boolean delete(String variable) {
    SystemParameter existing = get(variable);
    repository.delete(existing);
    invalidateCache(variable);
    return true;
  }

  @Override
  public MultipleSystemParameterResponse filter(MultipleSystemParameterRequest request) {
    int size = request.getSize() > 0 ? request.getSize() : defaultItemPerPage;

    CriteriaBuilder criteriaBuilder = () -> {
      List<Criteria> criteriaList = new ArrayList<>();
      if (StringUtils.isNotBlank(request.getVariable())) {
        criteriaList.add(Criteria.where("variable").regex(request.getVariable(), "i"));
      }
      return criteriaList;
    };

    ResultData<SystemParameter> entries =
        query(criteriaBuilder, size, request.getCursor(), mapper.toSortInfo(request.getSortBy()));
    Long total = entries.getTotal();
    String nextToken = null;
    Optional<SystemParameter> offset = entries.getOffset();
    if (offset.isPresent()) {
      nextToken = ParserUtil.encodeBase64(offset.get().getId());
    }

    MultipleSystemParameterResponse.Builder builder = MultipleSystemParameterResponse.newBuilder();
    entries.getDataList().iterator().forEachRemaining(item -> builder.addData(mapper.toResponseGrpc(item)));
    if (StringUtils.isNotBlank(nextToken)) {
      builder.setNextToken(nextToken);
    }
    builder.setTotal(ObjectUtils.isNotEmpty(total) ? total.intValue() : 0);
    return builder.build();
  }

  @Override
  public String getString(String variable, String defaultValue) {
    try {
      SystemParameter param = get(variable);
      return param.getData() != null ? param.getData() : defaultValue;
    } catch (ResourceNotFoundException e) {
      return defaultValue;
    }
  }

  @Override
  public Integer getInt(String variable, Integer defaultValue) {
    try {
      SystemParameter param = get(variable);
      return param.getAsInteger() != null ? param.getAsInteger() : defaultValue;
    } catch (ResourceNotFoundException e) {
      return defaultValue;
    }
  }

  @Override
  public Long getLong(String variable, Long defaultValue) {
    try {
      SystemParameter param = get(variable);
      return param.getAsLong() != null ? param.getAsLong() : defaultValue;
    } catch (ResourceNotFoundException e) {
      return defaultValue;
    }
  }

  @Override
  public Boolean getBoolean(String variable, Boolean defaultValue) {
    try {
      SystemParameter param = get(variable);
      return param.getAsBoolean() != null ? param.getAsBoolean() : defaultValue;
    } catch (ResourceNotFoundException e) {
      return defaultValue;
    }
  }

  private void updateCache(SystemParameter param) {
    String key = SYSPARAM_PREFIX + ":" + param.getVariable();
    cacheUtil.putValue(key, param, 30, TimeUnit.MINUTES);
  }

  private void invalidateCache(String variable) {
    String key = SYSPARAM_PREFIX + ":" + variable;
    cacheUtil.removeValue(key);
  }

  @Override
  protected String toId(String input) {
    return input;
  }

  @Override
  protected String getId(SystemParameter input) {
    return input.getId();
  }

  @Override
  protected String getIdFieldName() {
    return "_id";
  }
}



