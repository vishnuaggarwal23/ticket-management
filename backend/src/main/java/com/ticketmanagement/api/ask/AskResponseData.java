package com.ticketmanagement.api.ask;

import java.util.List;

public record AskResponseData(String answer, List<String> citedTicketIds) {
}
