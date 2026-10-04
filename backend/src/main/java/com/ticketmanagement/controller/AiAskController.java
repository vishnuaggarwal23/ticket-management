package com.ticketmanagement.controller;

import com.ticketmanagement.dto.common.DataResponse;
import com.ticketmanagement.dto.request.AskRequest;
import com.ticketmanagement.dto.response.AskResponseData;
import com.ticketmanagement.service.AskService;
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
