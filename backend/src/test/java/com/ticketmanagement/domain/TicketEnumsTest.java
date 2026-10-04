package com.ticketmanagement.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TicketEnumsTest {

    @ParameterizedTest
    @EnumSource(TicketStatus.class)
    void statusRoundTrip(TicketStatus status) {
        assertThat(TicketStatus.valueOf(status.name())).isEqualTo(status);
    }

    @Test
    void unknownStatusThrows() {
        assertThatThrownBy(() -> TicketStatus.valueOf("DONE"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @EnumSource(TicketPriority.class)
    void priorityRoundTrip(TicketPriority priority) {
        assertThat(TicketPriority.valueOf(priority.name())).isEqualTo(priority);
    }

    @ParameterizedTest
    @ValueSource(strings = {"URGENT", "P1"})
    void unknownPriorityNameThrows(String name) {
        assertThatThrownBy(() -> TicketPriority.valueOf(name))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void prioritySortOrderMatchesSpec() {
        assertThat(TicketPriority.LOW.sortOrder()).isEqualTo(1);
        assertThat(TicketPriority.MEDIUM.sortOrder()).isEqualTo(2);
        assertThat(TicketPriority.HIGH.sortOrder()).isEqualTo(3);
        assertThat(TicketPriority.CRITICAL.sortOrder()).isEqualTo(4);
    }

    @ParameterizedTest
    @EnumSource(TicketCategory.class)
    void categoryRoundTrip(TicketCategory category) {
        assertThat(TicketCategory.valueOf(category.name())).isEqualTo(category);
    }

    @Test
    void unknownCategoryThrows() {
        assertThatThrownBy(() -> TicketCategory.valueOf("NETWORK"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
