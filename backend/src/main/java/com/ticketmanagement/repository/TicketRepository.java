package com.ticketmanagement.repository;

import com.ticketmanagement.domain.TicketStatus;
import com.ticketmanagement.entity.TicketEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface TicketRepository extends JpaRepository<TicketEntity, String>, JpaSpecificationExecutor<TicketEntity> {

    /**
     * Native SQL only where the database sequence is required (formatted id is assigned in the service).
     */
    @Query(value = "SELECT nextval('ticket_number_seq')", nativeQuery = true)
    long nextTicketNumber();

    default Optional<TicketEntity> findWithCommentsById(String id) {
        return findOne(TicketSpecifications.byIdWithComments(id));
    }

    default Page<TicketEntity> search(TicketStatus status, String q, Pageable pageable) {
        Specification<TicketEntity> spec = TicketSpecifications.listFilter(status, q)
                .and(TicketSpecifications.orderBy(pageable));
        Pageable unsorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.unsorted());
        return findAll(spec, unsorted);
    }
}
