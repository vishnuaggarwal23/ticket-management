package com.ticketmanagement.domain;

public class TicketNotFoundException extends RuntimeException {

    private final String ticketId;

    public TicketNotFoundException(String ticketId) {
        super("Ticket " + ticketId + " was not found.");
        this.ticketId = ticketId;
    }

    public String ticketId() {
        return ticketId;
    }
}
