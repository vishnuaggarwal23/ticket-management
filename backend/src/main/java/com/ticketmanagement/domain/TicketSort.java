package com.ticketmanagement.domain;

public record TicketSort(String property, Direction direction) {

    public static final TicketSort DEFAULT = new TicketSort("createdAt", Direction.DESC);

    public enum Direction {
        ASC,
        DESC
    }
}
