package com.ticketmanagement.persistence;

import com.ticketmanagement.domain.TicketId;
import com.ticketmanagement.domain.TicketPriority;
import com.ticketmanagement.domain.TicketStatus;
import com.ticketmanagement.support.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TicketRepositoryIT extends AbstractPostgresIntegrationTest {

    @Autowired
    private TicketRepository tickets;

    @Autowired
    private CommentRepository comments;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @Order(1)
    void firstAllocatedIdIsTkt1001() {
        jdbcTemplate.update("DELETE FROM ticket_comment");
        jdbcTemplate.update("DELETE FROM ticket");
        jdbcTemplate.update("ALTER SEQUENCE ticket_number_seq RESTART WITH 1001");

        TicketEntity saved = persistTicket("First ticket", "hello", TicketStatus.OPEN, TicketPriority.MEDIUM);

        assertThat(saved.getId()).isEqualTo("TKT-1001");
        assertThat(tickets.findById("TKT-1001")).isPresent();
    }

    @Test
    void unknownIdIsEmpty() {
        assertThat(tickets.findById("TKT-999999")).isEmpty();
    }

    @Test
    void commentsAreOrderedByCreatedAtAsc() {
        TicketEntity ticket = persistTicket("Comment order", "", TicketStatus.OPEN, TicketPriority.LOW);
        Instant t0 = Instant.parse("2026-01-01T00:00:00Z");

        CommentEntity later = new CommentEntity();
        later.setBody("second");
        later.setCreatedAt(t0.plusSeconds(10));
        ticket.addComment(later);

        CommentEntity earlier = new CommentEntity();
        earlier.setBody("first");
        earlier.setCreatedAt(t0);
        ticket.addComment(earlier);

        tickets.saveAndFlush(ticket);

        List<CommentEntity> ordered = comments.findByTicket_IdOrderByCreatedAtAsc(ticket.getId());
        assertThat(ordered).extracting(CommentEntity::getBody).containsExactly("first", "second");

        TicketEntity loaded = tickets.findWithCommentsById(ticket.getId()).orElseThrow();
        assertThat(loaded.getComments()).extracting(CommentEntity::getBody).containsExactlyInAnyOrder("first", "second");
    }

    @Test
    void keywordMatchesTitle() {
        persistTicket("unique-title-widget-search", "other body", TicketStatus.OPEN, TicketPriority.MEDIUM);

        Page<TicketEntity> page = tickets.search(
                null, "widget-search", PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")));

        assertThat(page.getContent()).extracting(TicketEntity::getTitle)
                .contains("unique-title-widget-search");
    }

    @Test
    void keywordMatchIsCaseInsensitive() {
        persistTicket("CaseMixWidgetTitle", "body", TicketStatus.OPEN, TicketPriority.MEDIUM);

        Page<TicketEntity> page = tickets.search(
                null, "casemixwidget", PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")));

        assertThat(page.getContent()).extracting(TicketEntity::getTitle)
                .contains("CaseMixWidgetTitle");
    }

    @Test
    void keywordMatchesDescription() {
        persistTicket("plain title", "unique-desc-gadget-search", TicketStatus.OPEN, TicketPriority.MEDIUM);

        Page<TicketEntity> page = tickets.search(
                null, "gadget-search", PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")));

        assertThat(page.getContent()).extracting(TicketEntity::getDescription)
                .contains("unique-desc-gadget-search");
    }

    @Test
    void statusFilterAndKeywordTogether() {
        persistTicket("combo-open-needle", "x", TicketStatus.OPEN, TicketPriority.LOW);
        persistTicket("combo-closed-needle", "x", TicketStatus.CLOSED, TicketPriority.LOW);

        Page<TicketEntity> page = tickets.search(
                TicketStatus.OPEN, "combo-", PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")));

        assertThat(page.getContent()).extracting(TicketEntity::getTitle)
                .contains("combo-open-needle")
                .doesNotContain("combo-closed-needle");
    }

    @Test
    void statusFilterAlone() {
        persistTicket("status-only-in-progress", "n", TicketStatus.IN_PROGRESS, TicketPriority.HIGH);

        Page<TicketEntity> page = tickets.search(
                TicketStatus.IN_PROGRESS, null, PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")));

        assertThat(page.getContent()).extracting(TicketEntity::getStatus)
                .containsOnly(TicketStatus.IN_PROGRESS);
        assertThat(page.getContent()).extracting(TicketEntity::getTitle)
                .contains("status-only-in-progress");
    }

    @Test
    void pageMetaCounts() {
        persistTicket("page-meta-alpha", "", TicketStatus.RESOLVED, TicketPriority.MEDIUM);
        persistTicket("page-meta-beta", "", TicketStatus.RESOLVED, TicketPriority.MEDIUM);

        Page<TicketEntity> page = tickets.search(
                TicketStatus.RESOLVED, "page-meta-", PageRequest.of(0, 1, Sort.by(Sort.Direction.ASC, "createdAt")));

        assertThat(page.getSize()).isEqualTo(1);
        assertThat(page.getTotalElements()).isGreaterThanOrEqualTo(2);
        assertThat(page.getTotalPages()).isGreaterThanOrEqualTo(2);
        assertThat(page.getContent()).hasSize(1);
    }

    @Test
    void keywordInCommentBodyIsNotMatched() {
        TicketEntity ticket = persistTicket("comment-search-title", "no match here", TicketStatus.OPEN, TicketPriority.MEDIUM);
        CommentEntity comment = new CommentEntity();
        comment.setBody("unique-comment-only-phrase");
        ticket.addComment(comment);
        tickets.saveAndFlush(ticket);

        Page<TicketEntity> page = tickets.search(
                null, "unique-comment-only-phrase", PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")));

        assertThat(page.getContent()).extracting(TicketEntity::getId).doesNotContain(ticket.getId());
    }

    @Test
    void emptySearchIsEmptyPageNotError() {
        Page<TicketEntity> page = tickets.search(
                TicketStatus.CANCELLED, "definitely-not-a-ticket-zzzz",
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")));

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isZero();
    }

    @Test
    void prioritySortUsesEnumOrder() {
        persistTicket("prio-low", "", TicketStatus.OPEN, TicketPriority.LOW);
        persistTicket("prio-critical", "", TicketStatus.OPEN, TicketPriority.CRITICAL);
        persistTicket("prio-medium", "", TicketStatus.OPEN, TicketPriority.MEDIUM);
        persistTicket("prio-high", "", TicketStatus.OPEN, TicketPriority.HIGH);

        Page<TicketEntity> page = tickets.search(
                TicketStatus.OPEN, "prio-", PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "priority")));

        assertThat(page.getContent()).extracting(TicketEntity::getPriority)
                .containsExactly(
                        TicketPriority.LOW,
                        TicketPriority.MEDIUM,
                        TicketPriority.HIGH,
                        TicketPriority.CRITICAL);
    }

    private TicketEntity persistTicket(
            String title, String description, TicketStatus status, TicketPriority priority) {
        TicketEntity ticket = new TicketEntity();
        ticket.setId(TicketId.format(tickets.nextTicketNumber()));
        ticket.setTitle(title);
        ticket.setDescription(description);
        ticket.setStatus(status);
        ticket.setPriority(priority);
        return tickets.saveAndFlush(ticket);
    }
}
