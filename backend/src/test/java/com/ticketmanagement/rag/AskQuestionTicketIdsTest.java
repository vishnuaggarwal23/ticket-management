package com.ticketmanagement.rag;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AskQuestionTicketIdsTest {

    @Test
    void extractsPublicIdsInFirstMentionOrderAndDedupes() {
        assertThat(AskQuestionTicketIds.extractInOrder(
                "Compare TKT-1004 with TKT-1001 and TKT-1004 again"))
                .containsExactly("TKT-1004", "TKT-1001");
    }

    @Test
    void blankQuestionHasNoIds() {
        assertThat(AskQuestionTicketIds.extractInOrder("  ")).isEmpty();
    }
}
