package com.ticketmanagement.service;

import com.ticketmanagement.config.ApiProperties;
import com.ticketmanagement.domain.TicketPriority;
import com.ticketmanagement.domain.TicketStatus;
import com.ticketmanagement.domain.TicketStatusMachine;
import com.ticketmanagement.dto.common.PageMeta;
import com.ticketmanagement.dto.common.PageResponse;
import com.ticketmanagement.dto.request.CreateCommentRequest;
import com.ticketmanagement.dto.request.CreateTicketRequest;
import com.ticketmanagement.dto.request.UpdateTicketRequest;
import com.ticketmanagement.dto.response.CommentResponse;
import com.ticketmanagement.dto.response.TicketDetailResponse;
import com.ticketmanagement.dto.response.TicketSummaryResponse;
import com.ticketmanagement.entity.CommentEntity;
import com.ticketmanagement.entity.TicketEntity;
import com.ticketmanagement.exception.EmptyPatchException;
import com.ticketmanagement.exception.TicketNotFoundException;
import com.ticketmanagement.exception.TicketValidationException;
import com.ticketmanagement.repository.TicketRepository;
import com.ticketmanagement.util.SortParser;
import com.ticketmanagement.util.TicketId;
import com.ticketmanagement.util.TicketSort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketService {

    private static final Logger log = LoggerFactory.getLogger(TicketService.class);

    private final TicketRepository ticketRepository;
    private final TicketMapper ticketMapper;
    private final ApiProperties apiProperties;
    private final TicketPatchApplicator patchApplicator;
    private final TicketIngestionService ingestionService;

    public TicketService(
            TicketRepository ticketRepository,
            TicketMapper ticketMapper,
            ApiProperties apiProperties,
            TicketStatusMachine statusMachine,
            TicketIngestionService ingestionService
    ) {
        this.ticketRepository = ticketRepository;
        this.ticketMapper = ticketMapper;
        this.apiProperties = apiProperties;
        this.patchApplicator = new TicketPatchApplicator(statusMachine);
        this.ingestionService = ingestionService;
    }

    @Transactional
    public TicketDetailResponse create(CreateTicketRequest request) {
        if (request.title() == null || request.title().isBlank()) {
            throw new TicketValidationException("title", "must not be blank");
        }
        TicketEntity ticket = new TicketEntity();
        ticket.setId(TicketId.format(ticketRepository.nextTicketNumber()));
        ticket.setTitle(request.title().trim());
        ticket.setDescription(request.description() == null ? "" : request.description());
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setPriority(request.priority() == null ? TicketPriority.MEDIUM : request.priority());
        ticket.setAssignee(request.assignee());
        ticket.setCategory(request.category());
        TicketEntity saved = ticketRepository.save(ticket);
        scheduleIngestAfterCommit(saved.getId());
        return ticketMapper.toDetail(saved);
    }

    @Transactional(readOnly = true)
    public TicketDetailResponse getById(String id) {
        TicketEntity ticket = ticketRepository.findWithCommentsById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));
        return ticketMapper.toDetail(ticket);
    }

    @Transactional(readOnly = true)
    public PageResponse<TicketSummaryResponse> list(int page, int size, String sort, String q, TicketStatus status) {
        validateListPagination(page, size);
        TicketSort parsedSort = SortParser.parse(sort);
        Sort springSort = Sort.by(
                parsedSort.direction() == TicketSort.Direction.ASC ? Sort.Direction.ASC : Sort.Direction.DESC,
                parsedSort.property());
        Page<TicketEntity> result = ticketRepository.search(status, q, PageRequest.of(page, size, springSort));
        String sortEcho = parsedSort.property() + "," + parsedSort.direction().name().toLowerCase();
        return new PageResponse<>(
                result.getContent().stream().map(ticketMapper::toSummary).toList(),
                new PageMeta(
                        result.getNumber(),
                        result.getSize(),
                        result.getTotalElements(),
                        result.getTotalPages(),
                        sortEcho)
        );
    }

    @Transactional
    public TicketDetailResponse updateFields(String id, UpdateTicketRequest request) {
        if (!request.hasUpdates()) {
            throw new EmptyPatchException();
        }
        TicketEntity ticket = ticketRepository.findWithCommentsById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));
        patchApplicator.apply(ticket, request);
        TicketEntity saved = ticketRepository.save(ticket);
        scheduleIngestAfterCommit(saved.getId());
        return ticketMapper.toDetail(saved);
    }

    @Transactional
    public CommentResponse addComment(String ticketId, CreateCommentRequest request) {
        if (request.body() == null || request.body().isBlank()) {
            throw new TicketValidationException("body", "must not be blank");
        }
        TicketEntity ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));
        CommentEntity comment = new CommentEntity();
        comment.setBody(request.body());
        ticket.addComment(comment);
        ticketRepository.saveAndFlush(ticket);
        scheduleIngestAfterCommit(ticket.getId());
        return ticketMapper.toComment(comment);
    }

    private void validateListPagination(int page, int size) {
        if (size < 1 || size > apiProperties.pageSizeMax()) {
            throw new TicketValidationException("size", "must be between 1 and " + apiProperties.pageSizeMax());
        }
        if (page < 0) {
            throw new TicketValidationException("page", "must be greater than or equal to 0");
        }
    }

    private void scheduleIngestAfterCommit(String ticketId) {
        TransactionAfterCommit.run(() -> {
            try {
                ingestionService.ingest(ticketId);
            } catch (RuntimeException ex) {
                log.error("RAG ingest failed ticketId={}", ticketId, ex);
            }
        });
    }
}
