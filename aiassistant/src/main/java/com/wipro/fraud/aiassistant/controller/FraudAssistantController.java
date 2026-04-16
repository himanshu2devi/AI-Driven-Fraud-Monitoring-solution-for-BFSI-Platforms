package com.wipro.fraud.aiassistant.controller;

import com.wipro.fraud.aiassistant.dto.AssistantRequest;
import com.wipro.fraud.aiassistant.dto.AssistantResponse;
import com.wipro.fraud.aiassistant.service.ConversationMemoryService;
import com.wipro.fraud.aiassistant.service.FraudAssistantService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/fraud-assistant")
@RequiredArgsConstructor
public class FraudAssistantController {

    private final FraudAssistantService fraudAssistantService;
    private final ConversationMemoryService conversationMemoryService;

    @PostMapping("/ask")
    public AssistantResponse askAssistant(@RequestBody AssistantRequest request) {

        return fraudAssistantService.processQuery(request);

    }

    @PostMapping("/analyze")
    public AssistantResponse analyze(@RequestBody AssistantRequest request) {
        return fraudAssistantService.processAnalysis(request);
    }

    @GetMapping("/recent")
    public List<String> getRecentQueries(
            @RequestParam String userId) {

        return conversationMemoryService.getRecentUserQueries(userId);
    }
}

