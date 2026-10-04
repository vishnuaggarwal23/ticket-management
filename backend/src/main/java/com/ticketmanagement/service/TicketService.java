package com.ticketmanagement.service;

import com.ticketmanagement.api.common.PageMeta;
import com.ticketmanagement.api.common.PageResponse;
import com.ticketmanagement.api.ticket.CommentResponse;
import com.ticketmanagement.api.ticket.CreateCommentRequest;
import com.ticketmanagement.api.ticket.CreateTicketRequest;
import com.ticketmanagement.api.ticket.TicketDetailResponse;
import com.ticketmanagement.api.ticket.TicketSummaryResponse;
import com.ticketmanagement.api.ticket.UpdateTicketRequest;
import com.ticketmanagement.config.ApiProperties;
import com.ticketmanagement.domain.EmptyPatchException;
import com.ticketmanagement.domain.SortParser;
import com.ticketmanagement.domain.TicketId;
import com.ticketmanagement.domain.TicketNotFoundException;
import com.ticketmanagement.domain.TicketPriority;
import com.ticketmanagement.domain.TicketSort;
import com.ticketmanagement.domain.TicketStatus;
import com.ticketmanagement.domain.TicketStatusMachine;
import com.ticketmanagement.domain.TicketValidationException;
import com.ticketmanagement.persistence.CommentEntity;
import com.ticketmanagement.persistence.TicketEntity;
import com.ticketmanagement.persistence.TicketRepository;
import com.ticketmanagement.rag.TicketIngestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class TicketService {

    private static final Logger log = LoggerFactory.getLogger(TicketService.class);

    private final TicketRepository tickets;
    private final TicketMapper mapper;
    private final ApiProperties apiProperties;
    private final TicketStatusMachine statusMachine;
    private final TicketIngestionService ingestion;

    public TicketService(
            TicketRepository tickets,
            TicketMapper mapper,
            ApiProperties apiProperties,
            TicketStatusMachine statusMachine,
            TicketIngestionService ingestion
    ) {
        this.tickets = tickets;
        this.mapper = mapper;
        this.apiProperties = apiProperties;
        this.statusMachine = statusMachine;
        this.ingestion = ingestion;
    }

    @Transactional
    public TicketDetailResponse create(CreateTicketRequest request) {
        if (request.title() == null || request.title().isBlank()) {
            throw new TicketValidationException("title", "must not be blank");
        }
        TicketEntity ticket = new TicketEntity();
        ticket.setId(TicketId.format(tickets.nextTicketNumber()));
        ticket.setTitle(request.title().trim());
        ticket.setDescription(request.description() == null ? "" : request.description());
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setPriority(request.priority() == null ? TicketPriority.MEDIUM : request.priority());
        ticket.setAssignee(request.assignee());
        ticket.setCategory(request.category());
        TicketEntity saved = tickets.save(ticket);
        ingestAfterCommit(saved.getId());
        return mapper.toDetail(saved);
    }

    @Transactional(readOnly = true)
    public TicketDetailResponse getById(String id) {
        TicketEntity ticket = tickets.findWithCommentsById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));
        return mapper.toDetail(ticket);
    }

    @Transactional(readOnly = true)
    public PageResponse<TicketSummaryResponse> list(int page, int size, String sort, String q, TicketStatus status) {
        if (size < 1 || size > apiProperties.pageSizeMax()) {
            throw new TicketValidationException("size", "must be between 1 and " + apiProperties.pageSizeMax());
        }
        if (page < 0) {
            throw new TicketValidationException("page", "must be greater than or equal to 0");
        }
        TicketSort parsed = SortParser.parse(sort);
        Sort springSort = Sort.by(
                parsed.direction() == TicketSort.Direction.ASC ? Sort.Direction.ASC : Sort.Direction.DESC,
                parsed.property());
        Page<TicketEntity> result = tickets.search(status, q, PageRequest.of(page, size, springSort));
        String echo = parsed.property() + "," + parsed.direction().name().toLowerCase();
        return new PageResponse<>(
                result.getContent().stream().map(mapper::toSummary).toList(),
                new PageMeta(result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages(), echo)
        );
    }

    @Transactional
    public TicketDetailResponse updateFields(String id, UpdateTicketRequest request) {
        if (!request.hasUpdates()) {
            throw new EmptyPatchException();
        }
        TicketEntity ticket = tickets.findWithCommentsById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));
        if (request.status() != null) {
            statusMachine.assertTransitionAllowed(ticket.getStatus(), request.status());
        }
        if (request.title() != null) {
            ticket.setTitle(request.title());
        }
        if (request.description() != null) {
            ticket.setDescription(request.description());
        }
        if (request.priority() != null) {
            ticket.setPriority(request.priority());
        }
        if (request.assignee() != null) {
            ticket.setAssignee(request.assignee());
        }
        if (request.category() != null) {
            ticket.setCategory(request.category());
        }
        if (request.resolutionNotes() != null) {
            ticket.setResolutionNotes(request.resolutionNotes());
        }
        if (request.status() != null) {
            ticket.setStatus(request.status());
        }
        TicketEntity saved = tickets.save(ticket);
        ingestAfterCommit(saved.getId());
        return mapper.toDetail(saved);
    }

    @Transactional
    public CommentResponse addComment(String ticketId, CreateCommentRequest request) {
        if (request.body() == null || request.body().isBlank()) {
            throw new TicketValidationException("body", "must not be blank");
        }
        TicketEntity ticket = tickets.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));
        CommentEntity comment = new CommentEntity();
        comment.setBody(request.body());
        ticket.addComment(comment);
        tickets.saveAndFlush(ticket);
        ingestAfterCommit(ticket.getId());
        return mapper.toComment(comment);
    }

    private void ingestAfterCommit(String ticketId) {
        Runnable ingest = () -> {
            try {
                ingestion.ingest(ticketId);
            } catch (RuntimeException ex) {
                log.error("RAG ingest failed ticketId={}", ticketId, ex);
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    ingest.run();
                }
            });
        } else {
            ingest.run();
        }
    }
}
