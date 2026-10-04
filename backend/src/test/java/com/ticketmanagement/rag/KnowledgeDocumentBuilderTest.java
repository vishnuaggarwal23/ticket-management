package com.ticketmanagement.rag;

import com.ticketmanagement.domain.TicketCategory;
import com.ticketmanagement.domain.TicketPriority;
import com.ticketmanagement.domain.TicketStatus;
import com.ticketmanagement.entity.CommentEntity;
import com.ticketmanagement.entity.TicketEntity;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeDocumentBuilderTest {

    private final KnowledgeDocumentBuilder builder = new KnowledgeDocumentBuilder();

    @Test
    void assembledTextIncludesDescriptionCommentsAndResolution() {
        TicketEntity ticket = ticket("TKT-1001", "Card declined");
        ticket.setDescription("Processor code 05");
        ticket.setResolutionNotes("Updated card on file");
        CommentEntity first = comment("Verified expiry", Instant.parse("2026-01-02T10:00:00Z"));
        CommentEntity second = comment("Retry succeeded", Instant.parse("2026-01-02T14:30:00Z"));
        ticket.addComment(second);
        ticket.addComment(first);

        KnowledgeDocument document = builder.build(ticket);

        assertThat(document.embeddable()).isTrue();
        assertThat(document.assembledText()).contains("Processor code 05");
        assertThat(document.assembledText()).contains("Verified expiry");
        assertThat(document.assembledText()).contains("Retry succeeded");
        assertThat(document.assembledText()).contains("Updated card on file");
        assertThat(document.assembledText().indexOf("Verified expiry"))
                .isLessThan(document.assembledText().indexOf("Retry succeeded"));
        assertThat(document.metadataSnapshot().ticketId()).isEqualTo("TKT-1001");
        assertThat(document.metadataSnapshot().status()).isEqualTo(TicketStatus.OPEN);
        assertThat(document.metadataSnapshot().priority()).isEqualTo(TicketPriority.HIGH);
        assertThat(document.metadataSnapshot().assignee()).isEqualTo("desk");
        assertThat(document.metadataSnapshot().category()).isEqualTo(TicketCategory.PAYMENTS);
    }

    @Test
    void titleOnlyIsNotEmbeddable() {
        TicketEntity ticket = ticket("TKT-1002", "Title only");
        ticket.setDescription("");
        KnowledgeDocument document = builder.build(ticket);
        assertThat(document.embeddable()).isFalse();
        assertThat(document.assembledText()).contains("Ticket TKT-1002: Title only");
    }

    private static TicketEntity ticket(String id, String title) {
        TicketEntity entity = new TicketEntity();
        entity.setId(id);
        entity.setTitle(title);
        entity.setDescription("");
        entity.setStatus(TicketStatus.OPEN);
        entity.setPriority(TicketPriority.HIGH);
        entity.setAssignee("desk");
        entity.setCategory(TicketCategory.PAYMENTS);
        Instant now = Instant.parse("2026-10-04T00:00:00Z");
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return entity;
    }

    private static CommentEntity comment(String body, Instant createdAt) {
        CommentEntity comment = new CommentEntity();
        comment.setBody(body);
        comment.setCreatedAt(createdAt);
        return comment;
    }
}
