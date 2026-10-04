package com.ticketmanagement.dto.response;

import java.util.List;

public record AskResponseData(String answer, List<String> citedTicketIds) {
}
