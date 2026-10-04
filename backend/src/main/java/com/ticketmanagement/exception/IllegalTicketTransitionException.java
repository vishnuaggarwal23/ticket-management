package com.ticketmanagement.exception;

import com.ticketmanagement.domain.TicketStatus;

public class IllegalTicketTransitionException extends RuntimeException {

    public IllegalTicketTransitionException(TicketStatus from, TicketStatus to) {
        super("Cannot transition from " + from + " to " + to);
    }
}
