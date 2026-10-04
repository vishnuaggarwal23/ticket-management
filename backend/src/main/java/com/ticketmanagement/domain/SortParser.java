package com.ticketmanagement.domain;

public final class SortParser {

    private SortParser() {
    }

    public static TicketSort parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return TicketSort.DEFAULT;
        }
        String trimmed = raw.trim();
        int comma = trimmed.indexOf(',');
        String property;
        String directionPart;
        if (comma < 0) {
            property = trimmed;
            directionPart = "desc";
        } else {
            property = trimmed.substring(0, comma).trim();
            directionPart = trimmed.substring(comma + 1).trim();
            if (directionPart.contains(",")) {
                throw new InvalidSortException(property);
            }
        }
        if (!SortWhitelist.isAllowed(property)) {
            throw new InvalidSortException(property);
        }
        TicketSort.Direction direction = parseDirection(directionPart, property);
        return new TicketSort(property, direction);
    }

    private static TicketSort.Direction parseDirection(String directionPart, String property) {
        if (directionPart.equalsIgnoreCase("asc")) {
            return TicketSort.Direction.ASC;
        }
        if (directionPart.equalsIgnoreCase("desc")) {
            return TicketSort.Direction.DESC;
        }
        throw new InvalidSortException(property);
    }
}
