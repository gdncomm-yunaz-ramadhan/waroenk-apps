package com.gdn.project.waroenk.catalog.service;

import com.gdn.project.waroenk.catalog.MultipleSystemParameterRequest;
import com.gdn.project.waroenk.catalog.MultipleSystemParameterResponse;
import com.gdn.project.waroenk.catalog.entity.SystemParameter;
import com.gdn.project.waroenk.catalog.exceptions.ResourceNotFoundException;
import com.gdn.project.waroenk.catalog.mapper.SystemParameterMapper;
import com.gdn.project.waroenk.catalog.repository.MongoPageAble;
import com.gdn.project.waroenk.catalog.repository.SystemParameterRepository;
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
public class SystemParameterServiceImpl extends MongoPageAble<SystemParameter, String> implements SystemParameterService {
  private static final SystemParameterMapper mapper = SystemParameterMapper.INSTANCE;
  private static final String SYSTEM_PARAMETER_PREFIX = "systemParameter";
  private final SystemParameterRepository repository;
  private final CacheUtil<SystemParameter> cacheUtil;

  @Value("${default.item-per-page:10}")
  private Integer defaultItemPerPage;

  public SystemParameterServiceImpl(SystemParameterRepository repository,
      CacheUtil<SystemParameter> cacheUtil,
      CacheUtil<String> stringCacheUtil,
      MongoTemplate mongoTemplate) {
    super(SYSTEM_PARAMETER_PREFIX, stringCacheUtil, mongoTemplate, 10, TimeUnit.MINUTES, SystemParameter.class);
    this.repository = repository;
    this.cacheUtil = cacheUtil;
  }

  @Override
  public SystemParameter findOneSystemParameter(String variable) {
    String key = SYSTEM_PARAMETER_PREFIX + ":" + variable.trim();
    SystemParameter cached = cacheUtil.getValue(key);

    if (ObjectUtils.isNotEmpty(cached)) {
      return cached;
    }

    SystemParameter systemParameter = repository.findByVariable(variable.trim())
        .orElseThrow(() -> new ResourceNotFoundException("System parameter: " + variable + " is not found"));
    cacheUtil.putValue(key, systemParameter, 7, TimeUnit.DAYS);
    return systemParameter;
  }

  @Override
  public SystemParameter upsertSystemParameter(SystemParameter parameter) {
    String key = SYSTEM_PARAMETER_PREFIX + ":" + parameter.getVariable().trim();
    Optional<SystemParameter> existing = repository.findByVariable(parameter.getVariable().trim());

    if (existing.isPresent()) {
      SystemParameter existingEntity = existing.get();
      existingEntity.setDescription(parameter.getDescription());
      existingEntity.setData(parameter.getData());
      SystemParameter result = repository.save(existingEntity);
      cacheUtil.putValue(key, result, 7, TimeUnit.DAYS);
      return result;
    } else {
      SystemParameter saved = repository.save(parameter);
      cacheUtil.putValue(key, saved, 7, TimeUnit.DAYS);
      return saved;
    }
  }

  @Override
  public boolean deleteSystemParameter(String variable) {
    String key = SYSTEM_PARAMETER_PREFIX + ":" + variable.trim();
    SystemParameter existing = findOneSystemParameter(variable);
    repository.delete(existing);
    cacheUtil.removeValue(key);
    return true;
  }

  @Override
  public MultipleSystemParameterResponse findAllSystemParameters(MultipleSystemParameterRequest request) {
    int size = request.getSize() > 0 ? request.getSize() : defaultItemPerPage;

    CriteriaBuilder criteriaBuilder = () -> {
      List<Criteria> criteriaList = new ArrayList<>();
      if (StringUtils.isNotBlank(request.getVariable())) {
        criteriaList.add(Criteria.where("variable").regex(request.getVariable(), "i"));
      }
      return criteriaList;
    };

    ResultData<SystemParameter> entries = query(criteriaBuilder, size, request.getCursor(),
        mapper.toSortByDto(request.getSortBy()));
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
  public String getVariableData(String variable) {
    return getVariableData(variable, null);
  }

  @Override
  public String getVariableData(String variable, String defaultValue) {
    try {
      SystemParameter parameter = findOneSystemParameter(variable);
      return parameter.getData();
    } catch (Exception err) {
      log.warn("System parameter {} not found", variable);
    }
    return defaultValue;
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






