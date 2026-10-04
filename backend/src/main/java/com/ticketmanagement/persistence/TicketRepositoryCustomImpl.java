package com.ticketmanagement.persistence;

import com.ticketmanagement.domain.InvalidSortException;
import com.ticketmanagement.domain.TicketStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class TicketRepositoryCustomImpl implements TicketRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<TicketEntity> search(TicketStatus status, String q, Pageable pageable) {
        String where = whereClause(status, q);
        String order = orderClause(pageable);

        TypedQuery<TicketEntity> query = entityManager.createQuery(
                "SELECT t FROM TicketEntity t" + where + " ORDER BY " + order,
                TicketEntity.class);
        bind(query, status, q);
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());
        List<TicketEntity> content = query.getResultList();

        Query countQuery = entityManager.createQuery("SELECT COUNT(t) FROM TicketEntity t" + where);
        bind(countQuery, status, q);
        long total = (long) countQuery.getSingleResult();
        return new PageImpl<>(content, pageable, total);
    }

    private static String whereClause(TicketStatus status, String q) {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        if (status != null) {
            where.append(" AND t.status = :status");
        }
        if (hasKeyword(q)) {
            where.append("""
                     AND (LOWER(t.title) LIKE LOWER(CONCAT('%', :q, '%'))
                       OR LOWER(t.description) LIKE LOWER(CONCAT('%', :q, '%')))
                    """);
        }
        return where.toString();
    }

    private static String orderClause(Pageable pageable) {
        Sort.Order order = pageable.getSort().stream()
                .findFirst()
                .orElse(Sort.Order.desc("createdAt"));
        String expression = switch (order.getProperty()) {
            case "createdAt" -> "t.createdAt";
            case "updatedAt" -> "t.updatedAt";
            case "status" -> "t.status";
            case "priority" -> """
                    CASE t.priority
                      WHEN com.ticketmanagement.domain.TicketPriority.LOW THEN 1
                      WHEN com.ticketmanagement.domain.TicketPriority.MEDIUM THEN 2
                      WHEN com.ticketmanagement.domain.TicketPriority.HIGH THEN 3
                      WHEN com.ticketmanagement.domain.TicketPriority.CRITICAL THEN 4
                    END
                    """;
            default -> throw new InvalidSortException(order.getProperty());
        };
        return expression + (order.isAscending() ? " ASC" : " DESC");
    }

    private static void bind(Query query, TicketStatus status, String q) {
        if (status != null) {
            query.setParameter("status", status);
        }
        if (hasKeyword(q)) {
            query.setParameter("q", q);
        }
    }

    private static boolean hasKeyword(String q) {
        return q != null && !q.isBlank();
    }
}
