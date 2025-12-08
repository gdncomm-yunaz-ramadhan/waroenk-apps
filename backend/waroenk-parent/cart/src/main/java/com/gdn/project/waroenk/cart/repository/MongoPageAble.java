package com.gdn.project.waroenk.cart.repository;

import com.gdn.project.waroenk.cart.repository.model.ResultData;
import com.gdn.project.waroenk.cart.utility.CacheUtil;
import com.gdn.project.waroenk.cart.utility.ParserUtil;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Abstract base class for MongoDB pagination support.
 */
public abstract class MongoPageAble<D, I> {

  private final String prefix;
  private final CacheUtil<String> stringCacheUtil;
  private final MongoTemplate mongoTemplate;
  private final int duration;
  private final TimeUnit timeUnit;
  private final Class<D> entityClass;

  public MongoPageAble(String prefix,
      CacheUtil<String> stringCacheUtil,
      MongoTemplate mongoTemplate,
      int duration,
      TimeUnit timeUnit,
      Class<D> entityClass) {
    this.stringCacheUtil = stringCacheUtil;
    this.duration = duration;
    this.timeUnit = timeUnit;
    this.mongoTemplate = mongoTemplate;
    this.prefix = prefix;
    this.entityClass = entityClass;
  }

  protected abstract I toId(String input);

  protected abstract I getId(D input);

  protected abstract String getIdFieldName();

  public ResultData<D> query(CriteriaBuilder criteriaBuilder, int limit, String cursor, SortInfo sort) {
    ResultData.ResultDataBuilder<D> builder = ResultData.builder();
    if (limit < 1) {
      return builder.build();
    }

    int limitWithOffset = limit + 1;

    String field = "id";
    String direction = "asc";
    if (ObjectUtils.isNotEmpty(sort)) {
      if (StringUtils.isNotBlank(sort.field())) {
        field = sort.field().trim();
      }
      if (StringUtils.isNotBlank(sort.direction())) {
        direction = sort.direction().trim().toLowerCase();
      }
    }
    boolean isAscending = "asc".equalsIgnoreCase(direction);

    Query query = new Query();
    List<Criteria> criteriaList = new ArrayList<>();

    if (criteriaBuilder != null) {
      criteriaList.addAll(criteriaBuilder.build());
    }

    if (StringUtils.isNotBlank(cursor)) {
      String parsedCursor = ParserUtil.decodeBase64(cursor);
      if (parsedCursor != null) {
        Criteria cursorCriteria = isAscending ?
            Criteria.where(getIdFieldName()).gte(parsedCursor) :
            Criteria.where(getIdFieldName()).lte(parsedCursor);
        criteriaList.add(cursorCriteria);
      }
    }

    if (!criteriaList.isEmpty()) {
      query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
    }

    Sort sortOrder = isAscending ? Sort.by(Sort.Direction.ASC, field) : Sort.by(Sort.Direction.DESC, field);
    query.with(sortOrder);
    query.limit(limitWithOffset);

    List<D> result = mongoTemplate.find(query, entityClass);

    // Count total
    Query countQuery = new Query();
    if (criteriaBuilder != null) {
      List<Criteria> countCriteria = criteriaBuilder.build();
      if (!countCriteria.isEmpty()) {
        countQuery.addCriteria(new Criteria().andOperator(countCriteria.toArray(new Criteria[0])));
      }
    }
    long total = mongoTemplate.count(countQuery, entityClass);

    Optional<D> lastData = result.size() < limitWithOffset ? Optional.empty() : Optional.of(result.getLast());

    return builder.dataList(result.subList(0, Math.min(limit, result.size()))).offset(lastData).total(total).build();
  }

  public ResultData<D> query(CriteriaBuilder criteriaBuilder, int limit, String cursor) {
    return query(criteriaBuilder, limit, cursor, new SortInfo("id", "asc"));
  }

  public ResultData<D> query(CriteriaBuilder criteriaBuilder, int limit) {
    return query(criteriaBuilder, limit, null);
  }

  public ResultData<D> query(int limit) {
    return query(null, limit, null);
  }

  public ResultData<D> query(int limit, String cursor) {
    return query(null, limit, cursor);
  }

  protected String resolvePrefix(String additionalKeys) {
    return resolvePrefix() + ":" + additionalKeys;
  }

  protected String resolvePrefix() {
    return this.prefix;
  }

  protected MongoTemplate getMongoTemplate() {
    return mongoTemplate;
  }

  protected CacheUtil<String> getStringCacheUtil() {
    return stringCacheUtil;
  }

  @FunctionalInterface
  public interface CriteriaBuilder {
    List<Criteria> build();
  }


  public record SortInfo(String field, String direction) {
    public static SortInfo of(String field, String direction) {
      return new SortInfo(StringUtils.isNotBlank(field) ? field : "id",
          StringUtils.isNotBlank(direction) ? direction : "asc");
    }

    public static SortInfo defaultSort() {
      return new SortInfo("id", "asc");
    }
  }
}



