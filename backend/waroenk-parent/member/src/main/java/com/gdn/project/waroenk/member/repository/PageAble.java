package com.gdn.project.waroenk.member.repository;

import com.gdn.project.waroenk.member.repository.model.ResultData;
import com.gdn.project.waroenk.member.utility.CacheUtil;
import com.gdn.project.waroenk.member.utility.ParserUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Parameter;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.query.SortDirection;

import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public abstract class PageAble<D, I> {

  @FunctionalInterface
  public interface PredicateBuilder<D> {
    List<Predicate> build(Root<D> root, CriteriaBuilder criteriaBuilder);
  }

  public record SortInfo(String field, String direction) {
    public static SortInfo of(String field, String direction) {
      return new SortInfo(
          StringUtils.isNotBlank(field) ? field : "id",
          StringUtils.isNotBlank(direction) ? direction : "asc"
      );
    }
    
    public static SortInfo defaultSort() {
      return new SortInfo("id", "asc");
    }
  }

  private final String prefix;
  private final CacheUtil<String> stringCacheUtil;
  private final EntityManager entityManager;
  private final int duration;
  private final TimeUnit timeUnit;
  private final Class<D> type;
  private final Class<I> idx;

  public PageAble(String prefix,
      CacheUtil<String> stringCacheUtil,
      EntityManager entityManager,
      int duration,
      TimeUnit timeUnit) {
    this.stringCacheUtil = stringCacheUtil;
    this.duration = duration;
    this.timeUnit = timeUnit;
    this.entityManager = entityManager;
    this.prefix = prefix;
    Map.Entry<Class<D>, Class<I>> parameterizedInfo = constructParameterizedInfo();
    this.type = parameterizedInfo.getKey();
    this.idx = parameterizedInfo.getValue();
  }


  @SuppressWarnings("unchecked")
  private Map.Entry<Class<D>, Class<I>> constructParameterizedInfo() {
    Class<?> clazz = getClass();
    while (!Modifier.isAbstract(clazz.getSuperclass().getModifiers())) {
      clazz = clazz.getSuperclass();
    }
    Class<?> finalClazz = clazz;
    Class<D> data = (Class<D>) resolveParameterType(getParameterizedType(finalClazz, 0));
    Class<I> index = (Class<I>) resolveParameterType(getParameterizedType(finalClazz, 1));
    return new AbstractMap.SimpleEntry<>(data, index);
  }

  private Type getParameterizedType(Class<?> inputClass, int index) {
    if (index < 0) {
      return null;
    } else {
      Type[] parameters = ((ParameterizedType) inputClass.getGenericSuperclass()).getActualTypeArguments();
      if (index > parameters.length - 1) {
        return null;
      } else {
        return parameters[index];
      }
    }
  }

  private Class<?> resolveParameterType(Type type) {
    if (type instanceof ParameterizedType) {
      return ((ParameterizedType) type).getRawType().getClass();
    } else {
      return (Class<?>) type;
    }
  }

  protected abstract I toId(String input);

  protected abstract I getId(D input);

  Long countTotalMatch(Root<D> root, CriteriaQuery<Long> query) {
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    query.select(builder.count(root));
    return entityManager.createQuery(query).getSingleResult();
  }

  List<D> bulkFindDataByIds(List<String> ids, SortInfo sort) {
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<D> criteriaQuery = builder.createQuery(type);
    Root<D> root = criteriaQuery.from(type);

    String field = sort.field();
    String direction = sort.direction();

    Order order = SortDirection.interpret(direction) == SortDirection.ASCENDING ?
        builder.asc(root.get(field)) :
        builder.desc(root.get(field));

    CriteriaBuilder.In<I> inClause = builder.in(root.get("id"));
    for (String id : ids) {
      inClause.value(toId(id));
    }
    criteriaQuery.where(inClause).orderBy(order);

    TypedQuery<D> typedQuery = entityManager.createQuery(criteriaQuery);

    return typedQuery.getResultList();
  }

  List<D> bulkFind(CriteriaQuery<D> query, int limit) {
    TypedQuery<D> typedQuery = entityManager.createQuery(query).setMaxResults(limit);
    return typedQuery.getResultList();
  }

  public ResultData<D> query(PredicateBuilder<D> predicateBuilder, int limit, String cursor, SortInfo sort) {
    ResultData.ResultDataBuilder<D> builder = ResultData.builder();
    if (limit < 1) {
      return builder.build();
    }

    int limitWithOffset = limit + 1;
    Map.Entry<Root<D>, CriteriaQuery<I>> idsQueryEntry = constructQuery(predicateBuilder, cursor, sort, idx);
    Root<D> idsRoot = idsQueryEntry.getKey();
    CriteriaQuery<I> idsQuery = idsQueryEntry.getValue();
    Optional<List<String>> ids = getCacheAbleIds(idsRoot, idsQuery, limitWithOffset);
    List<D> result = new ArrayList<>();
    if (ObjectUtils.isNotEmpty(ids)) {
      if (ids.isPresent()) {
        List<String> savedIds = ids.get();
        if (ObjectUtils.isNotEmpty(savedIds) && !savedIds.isEmpty()) {
          result = bulkFindDataByIds(savedIds, sort);
        }
      }
    }
    if (ObjectUtils.isEmpty(result)) {
      Map.Entry<Root<D>, CriteriaQuery<D>> recordQueryEntry = constructQuery(predicateBuilder, cursor, sort, type);
      CriteriaQuery<D> recordQuery = recordQueryEntry.getValue();
      result = bulkFind(recordQuery, limitWithOffset);
      String key = getIdsKey(idsQuery, limitWithOffset);
      for (D parameter : result) {
        stringCacheUtil.addList(key, getId(parameter).toString(), duration, timeUnit);
      }
    }

    Map.Entry<Root<D>, CriteriaQuery<Long>> countQueryEntry = constructQuery(predicateBuilder, cursor, sort, Long.class);
    Root<D> countRoot = countQueryEntry.getKey();
    CriteriaQuery<Long> countQuery = countQueryEntry.getValue();
    Long total = getCacheAbleTotal(countRoot, countQuery);
    Optional<D> lastData = result.size() < limitWithOffset ? Optional.empty() : Optional.of(result.getLast());

    return builder.dataList(result.subList(0, Math.min(limit, result.size()))).offset(lastData).total(total).build();
  }

  <T> Map.Entry<Root<D>, CriteriaQuery<T>> constructQuery(
      PredicateBuilder<D> predicateBuilder,
      String cursor,
      SortInfo sort,
      Class<T> output) {
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<T> criteriaQuery = builder.createQuery(output);
    Root<D> root = criteriaQuery.from(type);
    
    String field = sort.field();
    String direction = sort.direction();
    boolean isAscending = SortDirection.interpret(direction) == SortDirection.ASCENDING;

    List<Predicate> predicates = new ArrayList<>();
    if (predicateBuilder != null) {
      predicates.addAll(predicateBuilder.build(root, builder));
    }

    if (StringUtils.isNotBlank(cursor)) {
      UUID parsedCursor = UUID.fromString(Objects.requireNonNull(ParserUtil.decodeBase64(cursor)));
      Predicate condition = isAscending ?
          builder.greaterThanOrEqualTo(root.get("id"), parsedCursor) :
          builder.lessThanOrEqualTo(root.get("id"), parsedCursor);
      predicates.add(condition);
    }

    if (!predicates.isEmpty()) {
      criteriaQuery.where(builder.and(predicates.toArray(new Predicate[0])));
    }

    if (!Long.class.equals(output)) {
      Order order = isAscending ?
          builder.asc(root.get(field)) :
          builder.desc(root.get(field));
      criteriaQuery.orderBy(order);
    }

    return new AbstractMap.SimpleEntry<>(root, criteriaQuery);
  }

  String getIdsKey(CriteriaQuery<?> criteriaQuery, int limitWithOffset) {
    return resolveKey(resolvePrefix("ids:" + limitWithOffset + ":"), criteriaQuery);
  }

  Optional<List<String>> getCacheAbleIds(Root<D> root, CriteriaQuery<?> criteriaQuery, int limitWithOffset) {
    criteriaQuery.select(root.get("id"));
    String key = getIdsKey(criteriaQuery, limitWithOffset);
    return Optional.ofNullable(stringCacheUtil.getList(key));
  }

  LinkedHashMap<String, Object> getCriteria(CriteriaQuery<?> cq) {
    Query jpaQuery = entityManager.createQuery(cq);

    LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
    for (Parameter<?> param : jpaQuery.getParameters()) {
      parameters.put(param.getName(), jpaQuery.getParameterValue(param));
    }
    return parameters;
  }

  Long getCacheAbleTotal(Root<D> root, CriteriaQuery<Long> criteriaQuery) {
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();

    criteriaQuery.select(builder.count(root));
    String key = resolveKey(resolvePrefix("count:"), criteriaQuery);

    String stored = stringCacheUtil.getValue(key);
    if (StringUtils.isNotBlank(stored)) {
      return Long.parseLong(stored);
    } else {
      Long total = countTotalMatch(root, criteriaQuery);
      stringCacheUtil.putValue(key, String.valueOf(total), duration, timeUnit);
      return total;
    }
  }

  String resolveKey(String prefix, CriteriaQuery<?> criteriaQuery) {
    LinkedHashMap<String, Object> identifiers = getCriteria(criteriaQuery);
    StringBuilder builder = new StringBuilder();
    if (ObjectUtils.isNotEmpty(identifiers)) {
      for (String key : identifiers.keySet()) {
        builder.append(":");
        builder.append(key);
        builder.append("-");
        builder.append(identifiers.get(key));
      }
    }
    return prefix + ParserUtil.encodeBase64(builder.toString());
  }

  public ResultData<D> query(PredicateBuilder<D> predicateBuilder, int limit, String cursor) {
    return query(predicateBuilder, limit, cursor, SortInfo.defaultSort());
  }

  public ResultData<D> query(PredicateBuilder<D> predicateBuilder, int limit) {
    return query(predicateBuilder, limit, null);
  }

  public ResultData<D> query(int limit) {
    return query(null, limit, null);
  }

  public ResultData<D> query(int limit, String cursor) {
    return query(null, limit, cursor);
  }

  String resolvePrefix(String additionalKeys) {
    return resolvePrefix() + ":" + additionalKeys;
  }

  String resolvePrefix() {
    return this.prefix;
  }
}
