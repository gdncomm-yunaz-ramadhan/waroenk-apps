package com.gdn.project.waroenk.member.repository;

import com.gdn.project.waroenk.member.entity.User;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserCustomRepositoryImpl implements UserCustomRepository {

  private final EntityManager entityManager;

  @Override
  public List<User> findUserLike(String query, String cursor, int size, String sortField, String sortDirection) {
    size = Math.max(size, 0);
    CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
    CriteriaQuery<User> criteriaQuery = criteriaBuilder.createQuery(User.class);
    Root<User> root = criteriaQuery.from(User.class);

    String field = StringUtils.isNotBlank(sortField) ? sortField.trim() : "id";
    String direction = StringUtils.isNotBlank(sortDirection) ? sortDirection.trim().toLowerCase() : "asc";

    Order order = SortDirection.interpret(direction) == SortDirection.ASCENDING ?
        criteriaBuilder.asc(root.get(field)) :
        criteriaBuilder.desc(root.get(field));

    List<Predicate> predicates = new ArrayList<>();

    if (StringUtils.isNotBlank(query)) {
      String likePattern = "%" + query.trim().toLowerCase() + "%";
      Predicate phoneLike = criteriaBuilder.like(criteriaBuilder.lower(root.get("phoneNumber")), likePattern);
      Predicate emailLike = criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), likePattern);
      Predicate fullNameLike = criteriaBuilder.like(criteriaBuilder.lower(root.get("fullName")), likePattern);
      predicates.add(criteriaBuilder.or(phoneLike, emailLike, fullNameLike));
    }

    if (StringUtils.isNotBlank(cursor)) {
      UUID parsedCursor = UUID.fromString(Objects.requireNonNull(ParserUtil.decodeBase64(cursor)));
      Predicate condition = order.isAscending() ?
          criteriaBuilder.greaterThan(root.get("id"), parsedCursor) :
          criteriaBuilder.lessThan(root.get("id"), parsedCursor);
      predicates.add(condition);
    }

    if (!predicates.isEmpty()) {
      criteriaQuery.where(criteriaBuilder.and(predicates.toArray(new Predicate[0])));
    }

    criteriaQuery.orderBy(order);

    TypedQuery<User> typedQuery = entityManager.createQuery(criteriaQuery);
    typedQuery.setMaxResults(size);
    return typedQuery.getResultList();
  }

  @Override
  public Long countAll() {
    String jpql = "SELECT COUNT(u) FROM User u";
    TypedQuery<Long> query = entityManager.createQuery(jpql, Long.class);
    return query.getSingleResult();
  }
}
