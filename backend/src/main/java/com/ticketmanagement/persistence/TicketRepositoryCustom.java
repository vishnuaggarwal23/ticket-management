package com.ticketmanagement.persistence;

import com.ticketmanagement.domain.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TicketRepositoryCustom {

    Page<TicketEntity> search(TicketStatus status, String q, Pageable pageable);
}
