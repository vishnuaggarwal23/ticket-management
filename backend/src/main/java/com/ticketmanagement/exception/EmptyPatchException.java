package com.ticketmanagement.exception;

public class EmptyPatchException extends RuntimeException {

    public EmptyPatchException() {
        super("no updatable fields provided");
    }
}
