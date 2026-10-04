package com.ticketmanagement.domain;

public class IllegalTicketTransitionException extends RuntimeException {

    public IllegalTicketTransitionException(TicketStatus from, TicketStatus to) {
        super("Cannot transition from " + from + " to " + to);
    }
}
