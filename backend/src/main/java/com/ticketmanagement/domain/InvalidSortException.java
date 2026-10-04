package com.ticketmanagement.domain;

public class InvalidSortException extends RuntimeException {

    private final String property;

    public InvalidSortException(String property) {
        super("property " + property + " is not sortable");
        this.property = property;
    }

    public String property() {
        return property;
    }
}
