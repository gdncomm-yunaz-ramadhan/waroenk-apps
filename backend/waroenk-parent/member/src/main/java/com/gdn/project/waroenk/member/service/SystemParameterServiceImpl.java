package com.gdn.project.waroenk.member.service;

import com.gdn.project.waroenk.member.MultipleSystemParameterRequest;
import com.gdn.project.waroenk.member.MultipleSystemParameterResponse;
import com.gdn.project.waroenk.member.entity.SystemParameter;
import com.gdn.project.waroenk.member.exceptions.ResourceNotFoundException;
import com.gdn.project.waroenk.member.mapper.SystemParameterMapper;
import com.gdn.project.waroenk.member.repository.PageAble;
import com.gdn.project.waroenk.member.repository.SystemParameterRepository;
import com.gdn.project.waroenk.member.repository.model.ResultData;
import com.gdn.project.waroenk.member.utility.CacheUtil;
import com.gdn.project.waroenk.member.utility.ParserUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class SystemParameterServiceImpl extends PageAble<SystemParameter, UUID> implements SystemParameterService {
  private static final SystemParameterMapper mapper = SystemParameterMapper.INSTANCE;
  private static final String SYSTEM_PARAMETER_PREFIX = "systemParameter";
  private final SystemParameterRepository repository;
  private final CacheUtil<SystemParameter> cacheUtil;
  private final EntityManager entityManager;
  @Value("${default.item-per-page:10}")
  private Integer defaultItemPerPage;

  public SystemParameterServiceImpl(SystemParameterRepository repository,
      CacheUtil<SystemParameter> cacheUtil,
      CacheUtil<String> stringCacheUtil,
      EntityManager entityManager) {
    super(SYSTEM_PARAMETER_PREFIX, stringCacheUtil, entityManager, 10, TimeUnit.MINUTES);
    this.entityManager = entityManager;
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
        .orElseThrow(() -> new ResourceNotFoundException("System parameter : " + variable + " is not found"));
    cacheUtil.putValue(key, systemParameter, 7, TimeUnit.DAYS);
    return systemParameter;
  }

  @Override
  public SystemParameter upsertSystemParameter(SystemParameter parameter) {
    try {
      // Attempt insert
      return repository.save(parameter);
    } catch (DataIntegrityViolationException e) {
      String key = SYSTEM_PARAMETER_PREFIX + ":" + parameter.getVariable().trim();

      // Unique constraint violation, find and update
      SystemParameter existingEntity = findOneSystemParameter(parameter.getVariable());
      existingEntity.setDescription(parameter.getDescription());
      existingEntity.setData(parameter.getData());
      SystemParameter result = repository.save(existingEntity);
      cacheUtil.putValue(key, result, 7, TimeUnit.DAYS);
      return result;
    }
  }

  @Override
  public boolean deleteSystemParameter(String variable) {
    String key = SYSTEM_PARAMETER_PREFIX + ":" + variable.trim();
    SystemParameter existing = findOneSystemParameter(variable);
    if (ObjectUtils.isEmpty(existing)) {
      throw new ResourceNotFoundException("System parameter : " + variable + " is not found");
    }
    repository.delete(existing);
    cacheUtil.removeValue(key);
    return true;
  }

  @Override
  public MultipleSystemParameterResponse findAllSystemParameters(MultipleSystemParameterRequest request) {
    int size = request.getSize() > 0 ? request.getSize() : defaultItemPerPage;

    PredicateBuilder<SystemParameter> predicateBuilder = (root, criteriaBuilder) -> {
      List<Predicate> predicateList = new ArrayList<>();
      predicateList.add(criteriaBuilder.like(root.get("variable"), "%" + request.getVariable() + "%"));
      return predicateList;
    };

    ResultData<SystemParameter> entries =
        query(predicateBuilder, size, request.getCursor(),
            PageAble.SortInfo.of(request.getSortBy().getField(), request.getSortBy().getDirection()));
    Long total = entries.getTotal();
    String nextToken = null;
    Optional<SystemParameter> offset = entries.getOffset();
    if (offset.isPresent()) {
      nextToken = ParserUtil.encodeBase64(offset.get().getId().toString());
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
  protected UUID toId(String input) {
    return UUID.fromString(input);
  }

  @Override
  protected UUID getId(SystemParameter input) {
    return input.getId();
  }
}
