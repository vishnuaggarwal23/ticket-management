package com.ticketmanagement.service;

import com.ticketmanagement.api.ticket.CreateCommentRequest;
import com.ticketmanagement.api.ticket.CreateTicketRequest;
import com.ticketmanagement.api.ticket.TicketDetailResponse;
import com.ticketmanagement.api.ticket.UpdateTicketRequest;
import com.ticketmanagement.config.ApiProperties;
import com.ticketmanagement.domain.EmptyPatchException;
import com.ticketmanagement.domain.IllegalTicketTransitionException;
import com.ticketmanagement.domain.TicketCategory;
import com.ticketmanagement.domain.TicketNotFoundException;
import com.ticketmanagement.domain.TicketPriority;
import com.ticketmanagement.domain.TicketStatus;
import com.ticketmanagement.domain.TicketStatusMachine;
import com.ticketmanagement.domain.TicketValidationException;
import com.ticketmanagement.persistence.TicketEntity;
import com.ticketmanagement.persistence.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketRepository tickets;

    private TicketStatusMachine statusMachine;
    private TicketService service;

    @BeforeEach
    void setUp() {
        statusMachine = spy(new TicketStatusMachine());
        service = new TicketService(tickets, new TicketMapper(), new ApiProperties(20, 100), statusMachine);
    }

    @Test
    void createAppliesDefaults() {
        when(tickets.nextTicketNumber()).thenReturn(1001L);
        when(tickets.save(any(TicketEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TicketDetailResponse created = service.create(
                new CreateTicketRequest("Need help", null, null, null, null));

        ArgumentCaptor<TicketEntity> captor = ArgumentCaptor.forClass(TicketEntity.class);
        verify(tickets).save(captor.capture());
        TicketEntity saved = captor.getValue();
        assertThat(saved.getId()).isEqualTo("TKT-1001");
        assertThat(saved.getStatus()).isEqualTo(TicketStatus.OPEN);
        assertThat(saved.getPriority()).isEqualTo(TicketPriority.MEDIUM);
        assertThat(saved.getDescription()).isEmpty();
        assertThat(created.id()).isEqualTo("TKT-1001");
        assertThat(created.comments()).isEmpty();
    }

    @Test
    void blankTitleDoesNotSave() {
        assertThatThrownBy(() -> service.create(
                new CreateTicketRequest("  ", null, null, null, null)))
                .isInstanceOf(TicketValidationException.class);
        verify(tickets, never()).save(any());
        verify(tickets, never()).nextTicketNumber();
    }

    @Test
    void getUnknownThrows() {
        when(tickets.findWithCommentsById("TKT-9")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getById("TKT-9"))
                .isInstanceOf(TicketNotFoundException.class);
    }

    @Test
    void patchOneField() {
        TicketEntity entity = existing("TKT-1002", "Old");
        when(tickets.findWithCommentsById("TKT-1002")).thenReturn(Optional.of(entity));
        when(tickets.save(entity)).thenReturn(entity);

        TicketDetailResponse updated = service.updateFields(
                "TKT-1002",
                new UpdateTicketRequest("New title", null, null, null, null, null, null));

        assertThat(updated.title()).isEqualTo("New title");
        assertThat(entity.getDescription()).isEqualTo("desc");
        verify(tickets).save(entity);
    }

    @Test
    void emptyPatchDoesNotSave() {
        assertThatThrownBy(() -> service.updateFields(
                "TKT-1002",
                new UpdateTicketRequest(null, null, null, null, null, null, null)))
                .isInstanceOf(EmptyPatchException.class);
        verify(tickets, never()).save(any());
    }

    @Test
    void addComment() {
        TicketEntity entity = existing("TKT-1003", "T");
        when(tickets.findById("TKT-1003")).thenReturn(Optional.of(entity));
        when(tickets.saveAndFlush(entity)).thenAnswer(invocation -> {
            entity.getComments().getFirst().setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
            return entity;
        });

        var response = service.addComment("TKT-1003", new CreateCommentRequest("hello"));

        assertThat(response.body()).isEqualTo("hello");
        assertThat(entity.getComments()).hasSize(1);
        verify(tickets).saveAndFlush(entity);
    }

    @Test
    void listDelegatesFilters() {
        TicketEntity entity = existing("TKT-1004", "Pay");
        when(tickets.search(eq(TicketStatus.OPEN), eq("pay"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entity), PageRequest.of(0, 20), 1));

        var page = service.list(0, 20, "createdAt,desc", "pay", TicketStatus.OPEN);

        assertThat(page.data()).hasSize(1);
        assertThat(page.meta().totalElements()).isEqualTo(1);
        assertThat(page.meta().sort()).isEqualTo("createdAt,desc");
        verify(tickets).search(eq(TicketStatus.OPEN), eq("pay"), any(Pageable.class));
    }

    @Test
    void addCommentUnknownTicketDoesNotSave() {
        when(tickets.findById("TKT-missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addComment("TKT-missing", new CreateCommentRequest("hello")))
                .isInstanceOf(TicketNotFoundException.class);
        verify(tickets, never()).saveAndFlush(any());
    }

    @Test
    void legalT1PersistsNewStatus() {
        TicketEntity entity = existing("TKT-1002", "Old");
        when(tickets.findWithCommentsById("TKT-1002")).thenReturn(Optional.of(entity));
        when(tickets.save(entity)).thenReturn(entity);

        TicketDetailResponse updated = service.updateFields(
                "TKT-1002",
                new UpdateTicketRequest(null, null, null, null, null, null, TicketStatus.IN_PROGRESS));

        assertThat(updated.status()).isEqualTo(TicketStatus.IN_PROGRESS);
        assertThat(entity.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
        verify(tickets).save(entity);
        verify(statusMachine).assertTransitionAllowed(TicketStatus.OPEN, TicketStatus.IN_PROGRESS);
    }

    @Test
    void illegalTransitionDoesNotSaveOrApplyOtherFields() {
        TicketEntity entity = existing("TKT-1002", "Old");
        when(tickets.findWithCommentsById("TKT-1002")).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> service.updateFields(
                "TKT-1002",
                new UpdateTicketRequest("should-not-apply", null, null, null, null, null, TicketStatus.RESOLVED)))
                .isInstanceOf(IllegalTicketTransitionException.class)
                .hasMessage("Cannot transition from OPEN to RESOLVED");

        assertThat(entity.getTitle()).isEqualTo("Old");
        assertThat(entity.getStatus()).isEqualTo(TicketStatus.OPEN);
        verify(tickets, never()).save(any());
    }

    @Test
    void fieldOnlyPatchDoesNotInvokeMachine() {
        TicketEntity entity = existing("TKT-1002", "Old");
        when(tickets.findWithCommentsById("TKT-1002")).thenReturn(Optional.of(entity));
        when(tickets.save(entity)).thenReturn(entity);

        service.updateFields(
                "TKT-1002",
                new UpdateTicketRequest("New title", null, null, null, null, null, null));

        verify(statusMachine, never()).assertTransitionAllowed(any(), any());
        assertThat(entity.getStatus()).isEqualTo(TicketStatus.OPEN);
    }

    @Test
    void statusPatchOnMissingTicketDoesNotInvokeMachine() {
        when(tickets.findWithCommentsById("TKT-missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateFields(
                "TKT-missing",
                new UpdateTicketRequest(null, null, null, null, null, null, TicketStatus.IN_PROGRESS)))
                .isInstanceOf(TicketNotFoundException.class);
        verify(statusMachine, never()).assertTransitionAllowed(any(), any());
        verify(tickets, never()).save(any());
    }

    private static TicketEntity existing(String id, String title) {
        TicketEntity entity = new TicketEntity();
        entity.setId(id);
        entity.setTitle(title);
        entity.setDescription("desc");
        entity.setStatus(TicketStatus.OPEN);
        entity.setPriority(TicketPriority.MEDIUM);
        entity.setCategory(TicketCategory.PAYMENTS);
        Instant now = Instant.parse("2026-10-04T00:00:00Z");
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return entity;
    }
}
