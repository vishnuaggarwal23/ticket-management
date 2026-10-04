package com.ticketmanagement.domain;

public final class TicketId {

    public static final String PREFIX = "TKT-";

    private TicketId() {
    }

    public static String format(long sequenceValue) {
        return PREFIX + sequenceValue;
    }
}
