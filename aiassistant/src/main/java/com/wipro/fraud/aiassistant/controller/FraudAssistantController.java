package com.wipro.fraud.aiassistant.controller;

import com.wipro.fraud.aiassistant.dto.AssistantRequest;
import com.wipro.fraud.aiassistant.dto.AssistantResponse;
import com.wipro.fraud.aiassistant.service.FraudAssistantService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/fraud-assistant")
@RequiredArgsConstructor
public class FraudAssistantController {

    private final FraudAssistantService fraudAssistantService;

    @PostMapping("/ask")
    public AssistantResponse askAssistant(@RequestBody AssistantRequest request) {

        return fraudAssistantService.processQuery(request);

    }
}