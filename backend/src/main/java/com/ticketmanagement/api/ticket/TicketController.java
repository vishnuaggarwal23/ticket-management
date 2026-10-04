package com.ticketmanagement.api.ticket;

import com.ticketmanagement.api.common.DataResponse;
import com.ticketmanagement.api.common.PageResponse;
import com.ticketmanagement.config.ApiProperties;
import com.ticketmanagement.domain.TicketStatus;
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

    private final TicketService tickets;
    private final ApiProperties apiProperties;

    public TicketController(TicketService tickets, ApiProperties apiProperties) {
        this.tickets = tickets;
        this.apiProperties = apiProperties;
    }

    @PostMapping
    public ResponseEntity<DataResponse<TicketDetailResponse>> create(@Valid @RequestBody CreateTicketRequest request) {
        TicketDetailResponse data = tickets.create(request);
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
        return tickets.list(page, pageSize, sort, q, status);
    }

    @GetMapping("/{id}")
    public DataResponse<TicketDetailResponse> get(@PathVariable String id) {
        return new DataResponse<>(tickets.getById(id));
    }

    @PatchMapping("/{id}")
    public DataResponse<TicketDetailResponse> patch(
            @PathVariable String id,
            @Valid @RequestBody UpdateTicketRequest request
    ) {
        return new DataResponse<>(tickets.updateFields(id, request));
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<DataResponse<CommentResponse>> addComment(
            @PathVariable String id,
            @Valid @RequestBody CreateCommentRequest request
    ) {
        CommentResponse data = tickets.addComment(id, request);
        return ResponseEntity.created(URI.create("/api/v1/tickets/" + id + "/comments/" + data.id()))
                .body(new DataResponse<>(data));
    }
}
