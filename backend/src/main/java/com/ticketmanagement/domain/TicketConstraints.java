package com.ticketmanagement.domain;

public final class TicketConstraints {

    public static final int TITLE_MAX = 500;
    public static final int DESCRIPTION_MAX = 100_000;
    public static final int ASSIGNEE_MAX = 320;
    public static final int COMMENT_BODY_MAX = 50_000;
    public static final int RESOLUTION_NOTES_MAX = 100_000;

    private TicketConstraints() {
    }
}
