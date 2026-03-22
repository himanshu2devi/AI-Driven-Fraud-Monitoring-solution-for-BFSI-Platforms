package com.wipro.fraud.aiassistant.util;

import com.wipro.fraud.aiassistant.dto.AssistantRequest;
import com.wipro.fraud.aiassistant.entity.ConversationMemory;

import java.util.List;

public class PromptTemplateBuilder {

    public static String buildFraudPrompt(
            AssistantRequest request,
            List<ConversationMemory> history) {

        StringBuilder conversationContext = new StringBuilder();

        for (ConversationMemory message : history) {

            conversationContext
                    .append(message.getRole())
                    .append(": ")
                    .append(message.getMessage())
                    .append("\n");
        }

        return """
You are an AI Fraud Monitoring Assistant used by fraud analysts in BFSI platforms.

Your responsibilities include:
- Explaining fraud monitoring rules
- Guiding fraud analysts during investigation
- Providing AML and fraud policy explanations

STRICT INSTRUCTIONS:

1. Use ONLY information retrieved from the fraud policy documents in the knowledge base.
2. Do NOT rely on general knowledge.
3. If the retrieved documents do not contain the answer, respond exactly with:
   "No relevant fraud monitoring policy found in the knowledge base."
4. Provide explanations suitable for a fraud analyst.

Conversation History:
%s

Current Question:
%s
""".formatted(conversationContext.toString(), request.getQuestion());
    }
}