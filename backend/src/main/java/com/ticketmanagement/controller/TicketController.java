package com.ticketmanagement.controller;

import com.ticketmanagement.config.ApiProperties;
import com.ticketmanagement.domain.TicketStatus;
import com.ticketmanagement.dto.common.DataResponse;
import com.ticketmanagement.dto.common.PageResponse;
import com.ticketmanagement.dto.request.CreateCommentRequest;
import com.ticketmanagement.dto.request.CreateTicketRequest;
import com.ticketmanagement.dto.request.UpdateTicketRequest;
import com.ticketmanagement.dto.response.CommentResponse;
import com.ticketmanagement.dto.response.TicketDetailResponse;
import com.ticketmanagement.dto.response.TicketSummaryResponse;
import com.ticketmanagement.service.TicketService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@Validated
@RequestMapping("/api/v1/tickets")
public class TicketController {

    private final TicketService ticketService;
    private final ApiProperties apiProperties;

    public TicketController(TicketService ticketService, ApiProperties apiProperties) {
        this.ticketService = ticketService;
        this.apiProperties = apiProperties;
    }

    @PostMapping
    public ResponseEntity<DataResponse<TicketDetailResponse>> create(@Valid @RequestBody CreateTicketRequest request) {
        TicketDetailResponse data = ticketService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/tickets/" + data.id()))
                .body(new DataResponse<>(data));
    }

    @GetMapping
    public PageResponse<TicketSummaryResponse> list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(required = false) @Min(1) @Max(100) Integer size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) TicketStatus status
    ) {
        int pageSize = size == null ? apiProperties.pageSizeDefault() : size;
        return ticketService.list(page, pageSize, sort, q, status);
    }

    @GetMapping("/{id}")
    public DataResponse<TicketDetailResponse> get(@PathVariable String id) {
        return new DataResponse<>(ticketService.getById(id));
    }

    @PatchMapping("/{id}")
    public DataResponse<TicketDetailResponse> patch(
            @PathVariable String id,
            @Valid @RequestBody UpdateTicketRequest request
    ) {
        return new DataResponse<>(ticketService.updateFields(id, request));
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<DataResponse<CommentResponse>> addComment(
            @PathVariable String id,
            @Valid @RequestBody CreateCommentRequest request
    ) {
        CommentResponse data = ticketService.addComment(id, request);
        return ResponseEntity.created(URI.create("/api/v1/tickets/" + id + "/comments/" + data.id()))
                .body(new DataResponse<>(data));
    }
}
