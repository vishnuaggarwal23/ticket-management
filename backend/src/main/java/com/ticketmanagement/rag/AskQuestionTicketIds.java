package com.ticketmanagement.rag;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Public ticket ids ({@code TKT-{n}}) explicitly mentioned in an ask question.
 */
public final class AskQuestionTicketIds {

    private static final Pattern TICKET_ID = Pattern.compile("TKT-\\d+");

    private AskQuestionTicketIds() {
    }

    public static List<String> extractInOrder(String question) {
        if (question == null || question.isBlank()) {
            return List.of();
        }
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        Matcher matcher = TICKET_ID.matcher(question);
        while (matcher.find()) {
            ids.add(matcher.group());
        }
        return new ArrayList<>(ids);
    }
}
