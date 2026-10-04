package com.ticketmanagement.api.ask;

import com.ticketmanagement.api.common.DataResponse;
import com.ticketmanagement.rag.AskService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AiAskController {

    private final AskService askService;

    public AiAskController(AskService askService) {
        this.askService = askService;
    }

    @PostMapping({"/api/ai/ask", "/api/v1/ai/ask"})
    public DataResponse<AskResponseData> ask(@Valid @RequestBody AskRequest request) {
        AskService.AskResult result = askService.ask(request.question().trim());
        return new DataResponse<>(new AskResponseData(result.answer(), result.citedTicketIds()));
    }
}
