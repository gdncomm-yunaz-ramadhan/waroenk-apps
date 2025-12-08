package com.gdn.project.waroenk.member.repository;

import com.gdn.project.waroenk.member.entity.SystemParameter;
import com.gdn.project.waroenk.member.utility.ParserUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.query.SortDirection;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SystemParameterCustomRepositoryImpl implements SystemParameterCustomRepository {
  private final EntityManager entityManager;

  @Override
  public List<SystemParameter> findSystemParametersLike(String variable, String cursor, int size, String sortField, String sortDirection) {
    size = Math.max(size, 0);
    CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
    CriteriaQuery<SystemParameter> criteriaQuery = criteriaBuilder.createQuery(SystemParameter.class);
    Root<SystemParameter> root = criteriaQuery.from(SystemParameter.class);

    String field = StringUtils.isNotBlank(sortField) ? sortField.trim() : "id";
    String direction = StringUtils.isNotBlank(sortDirection) ? sortDirection.trim().toLowerCase() : "asc";

    Order order = SortDirection.interpret(direction) == SortDirection.ASCENDING ?
        criteriaBuilder.asc(root.get(field)) :
        criteriaBuilder.desc(root.get(field));

    if (StringUtils.isNotBlank(variable)) {
      Predicate condition = criteriaBuilder.like(root.get("variable"), "%" + variable + "%");
      criteriaQuery.where(condition);
    }

    if (StringUtils.isNotBlank(cursor)) {
      UUID parsedCursor = UUID.fromString(Objects.requireNonNull(ParserUtil.decodeBase64(cursor)));
      Predicate condition = order.isAscending() ?
          criteriaBuilder.greaterThan(root.get("id"), parsedCursor) :
          criteriaBuilder.lessThan(root.get("id"), parsedCursor);
      criteriaQuery.where(condition);
    }

    criteriaQuery.orderBy(order);

    TypedQuery<SystemParameter> typedQuery = entityManager.createQuery(criteriaQuery);
    typedQuery.setMaxResults(size);
    return typedQuery.getResultList();
  }

  @Override
  public Long countAll() {
    String jpql = "SELECT COUNT(s) FROM SystemParameter s";
    TypedQuery<Long> query = entityManager.createQuery(jpql, Long.class);
    return query.getSingleResult();
  }
}
