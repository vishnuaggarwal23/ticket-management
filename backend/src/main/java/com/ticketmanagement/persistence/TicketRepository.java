package com.ticketmanagement.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TicketRepository extends JpaRepository<TicketEntity, String>, TicketRepositoryCustom {

    @Query(value = "SELECT nextval('ticket_number_seq')", nativeQuery = true)
    long nextTicketNumber();

    @Query("SELECT DISTINCT t FROM TicketEntity t LEFT JOIN FETCH t.comments WHERE t.id = :id")
    Optional<TicketEntity> findWithCommentsById(@Param("id") String id);
}
