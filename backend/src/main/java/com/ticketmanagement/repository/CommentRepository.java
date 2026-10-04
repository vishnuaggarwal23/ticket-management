package com.ticketmanagement.repository;

import com.ticketmanagement.entity.CommentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CommentRepository extends JpaRepository<CommentEntity, UUID> {

    List<CommentEntity> findByTicket_IdOrderByCreatedAtAsc(String ticketId);
}
