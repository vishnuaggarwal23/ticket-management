package com.ticketmanagement.repository;

import com.ticketmanagement.domain.TicketPriority;
import com.ticketmanagement.domain.TicketStatus;
import com.ticketmanagement.entity.TicketEntity;
import com.ticketmanagement.exception.InvalidSortException;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

public final class TicketSpecifications {

    private TicketSpecifications() {
    }

    public static Specification<TicketEntity> listFilter(TicketStatus status, String q) {
        return Specification.where(statusEquals(status)).and(keywordInTitleOrDescription(q));
    }

    public static Specification<TicketEntity> byIdWithComments(String id) {
        return Specification.where(idEquals(id)).and(fetchComments());
    }

    public static Specification<TicketEntity> orderBy(Pageable pageable) {
        return (root, query, cb) -> {
            if (isCountQuery(query)) {
                return cb.conjunction();
            }
            Sort.Order order = pageable.getSort().stream()
                    .findFirst()
                    .orElse(Sort.Order.desc("createdAt"));
            query.orderBy(toOrder(root, cb, order));
            return cb.conjunction();
        };
    }

    private static Specification<TicketEntity> statusEquals(TicketStatus status) {
        return (root, query, cb) ->
                status == null ? cb.conjunction() : cb.equal(root.get("status"), status);
    }

    private static Specification<TicketEntity> keywordInTitleOrDescription(String q) {
        return (root, query, cb) -> {
            if (q == null || q.isBlank()) {
                return cb.conjunction();
            }
            String pattern = "%" + q.toLowerCase() + "%";
            Predicate title = cb.like(cb.lower(root.get("title")), pattern);
            Predicate description = cb.like(cb.lower(root.get("description")), pattern);
            return cb.or(title, description);
        };
    }

    private static Specification<TicketEntity> idEquals(String id) {
        return (root, query, cb) -> cb.equal(root.get("id"), id);
    }

    private static Specification<TicketEntity> fetchComments() {
        return (root, query, cb) -> {
            if (!isCountQuery(query)) {
                root.fetch("comments", JoinType.LEFT);
                query.distinct(true);
            }
            return cb.conjunction();
        };
    }

    private static boolean isCountQuery(jakarta.persistence.criteria.CriteriaQuery<?> query) {
        Class<?> resultType = query.getResultType();
        return resultType == Long.class || resultType == long.class;
    }

    private static Order toOrder(Root<TicketEntity> root, CriteriaBuilder cb, Sort.Order order) {
        Expression<?> expression = switch (order.getProperty()) {
            case "createdAt" -> root.get("createdAt");
            case "updatedAt" -> root.get("updatedAt");
            case "status" -> root.get("status");
            case "priority" -> cb.selectCase()
                    .when(cb.equal(root.get("priority"), TicketPriority.LOW), 1)
                    .when(cb.equal(root.get("priority"), TicketPriority.MEDIUM), 2)
                    .when(cb.equal(root.get("priority"), TicketPriority.HIGH), 3)
                    .when(cb.equal(root.get("priority"), TicketPriority.CRITICAL), 4)
                    .otherwise(0);
            default -> throw new InvalidSortException(order.getProperty());
        };
        return order.isAscending() ? cb.asc(expression) : cb.desc(expression);
    }
}
