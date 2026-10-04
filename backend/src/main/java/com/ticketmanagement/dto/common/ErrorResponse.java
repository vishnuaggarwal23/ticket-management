package com.ticketmanagement.dto.common;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

public record ErrorResponse(ErrorBody error) {

    public record ErrorBody(
            int status,
            String code,
            String message,
            @JsonInclude(JsonInclude.Include.NON_EMPTY) List<ErrorDetail> details,
            Instant timestamp,
            String path
    ) {
    }

    public record ErrorDetail(String field, String message) {
    }
}
