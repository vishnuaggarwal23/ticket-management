package com.ticketmanagement.domain;

import com.ticketmanagement.exception.IllegalTicketTransitionException;

import java.util.Set;

public final class TicketStatusMachine {

    private record Edge(TicketStatus from, TicketStatus to) {
    }

    private static final Set<Edge> LEGAL = Set.of(
            new Edge(TicketStatus.OPEN, TicketStatus.IN_PROGRESS),
            new Edge(TicketStatus.IN_PROGRESS, TicketStatus.RESOLVED),
            new Edge(TicketStatus.RESOLVED, TicketStatus.CLOSED),
            new Edge(TicketStatus.OPEN, TicketStatus.CANCELLED),
            new Edge(TicketStatus.IN_PROGRESS, TicketStatus.CANCELLED)
    );

    public boolean isTransitionAllowed(TicketStatus current, TicketStatus target) {
        return LEGAL.contains(new Edge(current, target));
    }

    public void assertTransitionAllowed(TicketStatus current, TicketStatus target) {
        if (!isTransitionAllowed(current, target)) {
            throw new IllegalTicketTransitionException(current, target);
        }
    }
}
