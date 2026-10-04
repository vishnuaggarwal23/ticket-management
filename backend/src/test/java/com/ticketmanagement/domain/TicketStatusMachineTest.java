package com.ticketmanagement.domain;

import com.ticketmanagement.exception.IllegalTicketTransitionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TicketStatusMachineTest {

    private final TicketStatusMachine machine = new TicketStatusMachine();

    @ParameterizedTest
    @CsvSource({
            "OPEN, IN_PROGRESS",
            "IN_PROGRESS, RESOLVED",
            "RESOLVED, CLOSED",
            "OPEN, CANCELLED",
            "IN_PROGRESS, CANCELLED"
    })
    void legalEdgesAreAllowed(TicketStatus from, TicketStatus to) {
        assertThat(machine.isTransitionAllowed(from, to)).isTrue();
        machine.assertTransitionAllowed(from, to);
    }

    @ParameterizedTest
    @CsvSource({
            "CLOSED, OPEN",
            "RESOLVED, OPEN",
            "CANCELLED, OPEN"
    })
    void pdfReopensAreIllegal(TicketStatus from, TicketStatus to) {
        assertIllegal(from, to);
    }

    @ParameterizedTest
    @EnumSource(TicketStatus.class)
    void selfTransitionIsIllegal(TicketStatus status) {
        assertIllegal(status, status);
    }

    @ParameterizedTest
    @MethodSource("illegalPairs")
    void allIllegalPairsThrow(TicketStatus from, TicketStatus to) {
        assertIllegal(from, to);
    }

    @Test
    void illegalRegisterHasTwentyPairs() {
        assertThat(illegalPairs().toList()).hasSize(20);
    }

    static Stream<Arguments> illegalPairs() {
        TicketStatusMachine machine = new TicketStatusMachine();
        List<Arguments> pairs = new ArrayList<>();
        for (TicketStatus from : TicketStatus.values()) {
            for (TicketStatus to : TicketStatus.values()) {
                if (!machine.isTransitionAllowed(from, to)) {
                    pairs.add(Arguments.of(from, to));
                }
            }
        }
        return pairs.stream();
    }

    private void assertIllegal(TicketStatus from, TicketStatus to) {
        assertThat(machine.isTransitionAllowed(from, to)).isFalse();
        assertThatThrownBy(() -> machine.assertTransitionAllowed(from, to))
                .isInstanceOf(IllegalTicketTransitionException.class)
                .hasMessage("Cannot transition from " + from + " to " + to);
    }
}
