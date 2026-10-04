package com.ticketmanagement.rag;

import com.ticketmanagement.domain.TicketCategory;
import com.ticketmanagement.domain.TicketPriority;
import com.ticketmanagement.domain.TicketStatus;
import com.ticketmanagement.dto.request.CreateCommentRequest;
import com.ticketmanagement.dto.request.CreateTicketRequest;
import com.ticketmanagement.dto.request.UpdateTicketRequest;
import com.ticketmanagement.dto.response.TicketDetailResponse;
import com.ticketmanagement.entity.CommentEntity;
import com.ticketmanagement.exception.EmptyPatchException;
import com.ticketmanagement.exception.IllegalTicketTransitionException;
import com.ticketmanagement.repository.CommentRepository;
import com.ticketmanagement.service.TicketService;
import com.ticketmanagement.support.AbstractPostgresIntegrationTest;
import com.ticketmanagement.support.ChunkMetadataAssertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TicketIngestionHookIT extends AbstractPostgresIntegrationTest {

    @Autowired
    private TicketService tickets;

    @Autowired
    private VectorChunkStore store;

    @Autowired
    private CommentRepository comments;

    @Test
    void createTriggersInitialIngestWithMetadata() {
        TicketDetailResponse created = tickets.create(
                new CreateTicketRequest(
                        "Create hook",
                        "initial ingest body unique phrase",
                        TicketPriority.HIGH,
                        "agent@example.com",
                        TicketCategory.PAYMENTS));
        List<VectorChunkStore.StoredChunk> chunks = store.findByTicketId(created.id());
        assertThat(chunks).isNotEmpty();
        for (VectorChunkStore.StoredChunk chunk : chunks) {
            ChunkMetadataAssertions.assertPdfAndTechnicalMetadataKeys(chunk.metadataJson(), created.id());
            assertThat(chunk.metadataJson()).contains("\"priority\":\"HIGH\"");
            assertThat(chunk.metadataJson()).contains("\"assignee\":\"agent@example.com\"");
            assertThat(chunk.metadataJson()).contains("\"category\":\"PAYMENTS\"");
        }
    }

    @Test
    void illegalStatusTransitionDoesNotReIngestOrChangeChunks() {
        TicketDetailResponse created = tickets.create(
                new CreateTicketRequest("Illegal hook", "body for illegal transition ingest test", null, null, null));
        List<UUID> chunkIdsBefore = store.findByTicketId(created.id()).stream()
                .map(VectorChunkStore.StoredChunk::id)
                .toList();
        assertThat(chunkIdsBefore).isNotEmpty();

        assertThatThrownBy(() -> tickets.updateFields(created.id(), status(TicketStatus.CLOSED)))
                .isInstanceOf(IllegalTicketTransitionException.class);

        List<UUID> chunkIdsAfter = store.findByTicketId(created.id()).stream()
                .map(VectorChunkStore.StoredChunk::id)
                .toList();
        assertThat(chunkIdsAfter).containsExactlyElementsOf(chunkIdsBefore);
    }

    @Test
    void emptyPatchDoesNotReplaceVectorChunks() {
        TicketDetailResponse created = tickets.create(
                new CreateTicketRequest("Empty patch hook", "content before empty patch", null, null, null));
        List<UUID> chunkIdsBefore = store.findByTicketId(created.id()).stream()
                .map(VectorChunkStore.StoredChunk::id)
                .toList();

        assertThatThrownBy(() -> tickets.updateFields(
                created.id(),
                new UpdateTicketRequest(null, null, null, null, null, null, null)))
                .isInstanceOf(EmptyPatchException.class);

        List<UUID> chunkIdsAfter = store.findByTicketId(created.id()).stream()
                .map(VectorChunkStore.StoredChunk::id)
                .toList();
        assertThat(chunkIdsAfter).containsExactlyElementsOf(chunkIdsBefore);
    }

    @Test
    void updateDescriptionReplacesStoredContent() {
        TicketDetailResponse created = tickets.create(
                new CreateTicketRequest("Hook ticket", "alpha payment gateway timeout unique", null, null, null));
        List<VectorChunkStore.StoredChunk> first = store.findByTicketId(created.id());
        assertThat(first).isNotEmpty();
        assertThat(first.getFirst().content()).contains("alpha payment");
        assertThat(first.getFirst().metadataJson()).contains("\"status\":\"OPEN\"");

        tickets.updateFields(
                created.id(),
                new UpdateTicketRequest(null, "beta payment gateway rewritten unique", null, null, null, null, null));
        List<VectorChunkStore.StoredChunk> second = store.findByTicketId(created.id());
        assertThat(second).isNotEmpty();
        assertThat(second.getFirst().content()).contains("beta payment");
        assertThat(second.stream().map(VectorChunkStore.StoredChunk::id).toList())
                .doesNotContainAnyElementsOf(first.stream().map(VectorChunkStore.StoredChunk::id).toList());
    }

    @Test
    void closeRefreshesStatusMetadata() {
        TicketDetailResponse created = tickets.create(
                new CreateTicketRequest("Close ingest", "resolution will follow for ingest metadata", null, null, null));
        tickets.updateFields(created.id(), status(TicketStatus.IN_PROGRESS));
        tickets.updateFields(created.id(), status(TicketStatus.RESOLVED));
        tickets.updateFields(
                created.id(),
                new UpdateTicketRequest(null, null, null, null, null, "fixed by retry", TicketStatus.CLOSED));

        List<VectorChunkStore.StoredChunk> chunks = store.findByTicketId(created.id());
        assertThat(chunks).isNotEmpty();
        assertThat(chunks.getFirst().metadataJson()).contains("\"status\":\"CLOSED\"");
        assertThat(chunks.getFirst().content()).contains("fixed by retry");
    }

    @Test
    void addCommentReIngestsCommentText() {
        TicketDetailResponse created = tickets.create(
                new CreateTicketRequest("Comment ingest", "initial payment description unique", null, null, null));
        tickets.addComment(created.id(), new CreateCommentRequest(
                "follow-up note about shipment tracking unique"));

        List<VectorChunkStore.StoredChunk> chunks = store.findByTicketId(created.id());
        assertThat(chunks.stream().map(VectorChunkStore.StoredChunk::content).toList())
                .anyMatch(content -> content.contains("follow-up note about shipment tracking unique"));
    }

    @Test
    void reIngestAfterCommentBodyChangeRemovesStaleChunkText() {
        String stale = "obsolete-comment-body-ac-rag-ing-06";
        String fresh = "replacement-comment-body-ac-rag-ing-06";
        TicketDetailResponse created = tickets.create(
                new CreateTicketRequest("Comment replace", "base description for ingest", null, null, null));
        tickets.addComment(created.id(), new CreateCommentRequest(stale));

        String combinedBefore = store.findByTicketId(created.id()).stream()
                .map(VectorChunkStore.StoredChunk::content)
                .collect(Collectors.joining("\n"));
        assertThat(combinedBefore).contains(stale);

        CommentEntity comment = comments.findByTicket_IdOrderByCreatedAtAsc(created.id()).getFirst();
        comment.setBody(fresh);
        comments.save(comment);

        tickets.updateFields(
                created.id(),
                new UpdateTicketRequest("Comment replace refreshed", null, null, null, null, null, null));

        String combinedAfter = store.findByTicketId(created.id()).stream()
                .map(VectorChunkStore.StoredChunk::content)
                .collect(Collectors.joining("\n"));
        assertThat(combinedAfter).doesNotContain(stale);
        assertThat(combinedAfter).contains(fresh);
    }

    private static UpdateTicketRequest status(TicketStatus status) {
        return new UpdateTicketRequest(null, null, null, null, null, null, status);
    }
}
