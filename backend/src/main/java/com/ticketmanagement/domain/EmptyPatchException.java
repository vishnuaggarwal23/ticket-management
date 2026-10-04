package com.ticketmanagement.domain;

public class EmptyPatchException extends RuntimeException {

    public EmptyPatchException() {
        super("no updatable fields provided");
    }
}
