package com.ticketmanagement.domain;

public class TicketValidationException extends RuntimeException {

    private final String field;

    public TicketValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String field() {
        return field;
    }
}
