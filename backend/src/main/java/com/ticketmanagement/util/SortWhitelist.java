package com.ticketmanagement.util;

import java.util.Set;

public final class SortWhitelist {

    public static final Set<String> PROPERTIES = Set.of("createdAt", "updatedAt", "priority", "status");

    private SortWhitelist() {
    }

    public static boolean isAllowed(String property) {
        return PROPERTIES.contains(property);
    }
}
