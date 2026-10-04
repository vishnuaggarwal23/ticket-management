package com.ticketmanagement.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SortParserTest {

    @Test
    void omittedSortUsesCreatedAtDesc() {
        assertThat(SortParser.parse(null)).isEqualTo(TicketSort.DEFAULT);
        assertThat(SortParser.parse("  ")).isEqualTo(TicketSort.DEFAULT);
        assertThat(TicketSort.DEFAULT).isEqualTo(new TicketSort("createdAt", TicketSort.Direction.DESC));
    }

    @ParameterizedTest
    @CsvSource({
            "'createdAt,desc', createdAt, DESC",
            "'updatedAt,asc', updatedAt, ASC",
            "'priority,asc', priority, ASC",
            "'status,desc', status, DESC",
            "createdAt, createdAt, DESC"
    })
    void parsesWhitelistedSort(String raw, String property, TicketSort.Direction direction) {
        assertThat(SortParser.parse(raw)).isEqualTo(new TicketSort(property, direction));
    }

    @ParameterizedTest
    @ValueSource(strings = {"title,desc", "assignee,asc", "foo"})
    void rejectsUnknownProperty(String raw) {
        assertThatThrownBy(() -> SortParser.parse(raw))
                .isInstanceOf(InvalidSortException.class)
                .hasMessageContaining("is not sortable");
    }

    @Test
    void rejectsInvalidDirection() {
        assertThatThrownBy(() -> SortParser.parse("createdAt,up"))
                .isInstanceOf(InvalidSortException.class);
    }
}
