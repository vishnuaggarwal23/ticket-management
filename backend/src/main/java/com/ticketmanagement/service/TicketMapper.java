package com.ticketmanagement.service;

import com.ticketmanagement.dto.response.CommentResponse;
import com.ticketmanagement.dto.response.TicketDetailResponse;
import com.ticketmanagement.dto.response.TicketSummaryResponse;
import com.ticketmanagement.entity.CommentEntity;
import com.ticketmanagement.entity.TicketEntity;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class TicketMapper {

    public TicketSummaryResponse toSummary(TicketEntity ticket) {
        return new TicketSummaryResponse(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getStatus(),
                ticket.getPriority(),
                ticket.getAssignee(),
                ticket.getCategory(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt()
        );
    }

    public TicketDetailResponse toDetail(TicketEntity ticket) {
        List<CommentResponse> comments = ticket.getComments() == null
                ? List.of()
                : ticket.getComments().stream()
                .sorted(Comparator.comparing(CommentEntity::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::toComment)
                .toList();
        return new TicketDetailResponse(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getStatus(),
                ticket.getPriority(),
                ticket.getAssignee(),
                ticket.getCategory(),
                ticket.getResolutionNotes(),
                comments,
                ticket.getCreatedAt(),
                ticket.getUpdatedAt()
        );
    }

    public CommentResponse toComment(CommentEntity comment) {
        return new CommentResponse(comment.getId(), comment.getBody(), comment.getCreatedAt());
    }
}
