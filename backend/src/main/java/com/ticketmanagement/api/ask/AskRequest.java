package com.ticketmanagement.api.ask;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AskRequest(
        @NotBlank @Size(max = 2000) String question
) {
}
