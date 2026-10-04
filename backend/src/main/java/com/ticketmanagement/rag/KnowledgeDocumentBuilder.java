package com.ticketmanagement.rag;

import com.ticketmanagement.persistence.CommentEntity;
import com.ticketmanagement.persistence.TicketEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

@Component
public class KnowledgeDocumentBuilder {

    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_INSTANT;

    public KnowledgeDocument build(TicketEntity ticket) {
        Instant now = Instant.now();
        List<CommentEntity> comments = ticket.getComments().stream()
                .sorted(Comparator.comparing(CommentEntity::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        boolean embeddable = hasEmbeddableText(ticket, comments);
        String assembled = assemble(ticket, comments);
        RagChunkMetadata snapshot = new RagChunkMetadata(
                ticket.getId(),
                ticket.getStatus(),
                ticket.getPriority(),
                ticket.getAssignee(),
                ticket.getCategory(),
                0,
                now
        );
        return new KnowledgeDocument(ticket.getId(), ticket.getTitle(), assembled, snapshot, now, embeddable);
    }

    static boolean hasEmbeddableText(TicketEntity ticket, List<CommentEntity> comments) {
        if (ticket.getDescription() != null && !ticket.getDescription().isBlank()) {
            return true;
        }
        if (ticket.getResolutionNotes() != null && !ticket.getResolutionNotes().isBlank()) {
            return true;
        }
        return comments.stream().anyMatch(c -> c.getBody() != null && !c.getBody().isBlank());
    }

    private static String assemble(TicketEntity ticket, List<CommentEntity> comments) {
        String assignee = ticket.getAssignee() == null ? "" : ticket.getAssignee();
        String category = ticket.getCategory() == null ? "" : ticket.getCategory().name();
        StringBuilder text = new StringBuilder();
        text.append("Ticket ").append(ticket.getId()).append(": ").append(nullToEmpty(ticket.getTitle())).append('\n');
        text.append("Status: ").append(ticket.getStatus())
                .append(" | Priority: ").append(ticket.getPriority())
                .append(" | Assignee: ").append(assignee)
                .append(" | Category: ").append(category)
                .append("\n\n");
        text.append("Description:\n").append(nullToEmpty(ticket.getDescription())).append("\n\n");
        text.append("Comments:\n");
        if (comments.isEmpty()) {
            text.append("(none)\n");
        } else {
            for (CommentEntity comment : comments) {
                Instant created = comment.getCreatedAt() == null ? Instant.EPOCH : comment.getCreatedAt();
                text.append("- [")
                        .append(ISO.format(created.atOffset(ZoneOffset.UTC).toInstant()))
                        .append("] ")
                        .append(nullToEmpty(comment.getBody()))
                        .append('\n');
            }
        }
        text.append('\n');
        String resolution = ticket.getResolutionNotes() == null || ticket.getResolutionNotes().isBlank()
                ? "(none)"
                : ticket.getResolutionNotes();
        text.append("Resolution:\n").append(resolution);
        return text.toString();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
