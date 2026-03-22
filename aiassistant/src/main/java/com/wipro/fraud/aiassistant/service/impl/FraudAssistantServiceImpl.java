package com.wipro.fraud.aiassistant.service.impl;

import com.wipro.fraud.aiassistant.client.BedrockKnowledgeBaseClient;
import com.wipro.fraud.aiassistant.dto.AssistantRequest;
import com.wipro.fraud.aiassistant.dto.AssistantResponse;
import com.wipro.fraud.aiassistant.entity.ConversationMemory;
import com.wipro.fraud.aiassistant.service.ConversationMemoryService;
import com.wipro.fraud.aiassistant.service.FraudAssistantService;
import com.wipro.fraud.aiassistant.util.PromptTemplateBuilder;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FraudAssistantServiceImpl implements FraudAssistantService {

    private final BedrockKnowledgeBaseClient bedrockClient;
    private final ConversationMemoryService memoryService;

    @Override
    public AssistantResponse processQuery(AssistantRequest request) {

        String sessionId = request.getSessionId();

        // 1️⃣ Load conversation history
        List<ConversationMemory> history =
                memoryService.getConversation(sessionId);

        // 2️⃣ Build prompt with history
        String prompt =
                PromptTemplateBuilder.buildFraudPrompt(request, history);

        // 3️⃣ Call Bedrock Knowledge Base
        String answer =
                bedrockClient.queryKnowledgeBase(prompt);

        // 4️⃣ Save conversation messages
        memoryService.saveMessage(sessionId,"USER",request.getQuestion());

        memoryService.saveMessage(sessionId,"ASSISTANT",answer);

        // 5️⃣ Return response
        return AssistantResponse.builder()
                .answer(answer)
                .source("Fraud Policy Knowledge Base")
                .build();
    }
}