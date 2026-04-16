package com.wipro.fraud.aiassistant.Agent;

import com.wipro.fraud.aiassistant.dto.AssistantRequest;
import com.wipro.fraud.aiassistant.dto.AssistantResponse;
import com.wipro.fraud.aiassistant.service.FraudAssistantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
@Component
public class FraudReadOnlyAgent {

    @Autowired
    private FraudAssistantService service;

    public void execute(String query) {

        String sessionId = UUID.randomUUID().toString();
        String userId = "AGENT";

        System.out.println("\n🧠 Agent Received: " + query);

        AssistantRequest request = new AssistantRequest();
        request.setQuestion(query);
        request.setSessionId(sessionId);
        request.setAnalystId(userId);
        request.setAgentMode(true);

        AssistantResponse response = service.processQuery(request);

        if (response.getAnswer() != null && !response.getAnswer().isBlank()) {
            System.out.println("\n🤖 AI Response:");
            System.out.println(response.getAnswer());
        }

        if (response.getData() instanceof List<?>) {

            List<?> dataList = (List<?>) response.getData();

            if (!dataList.isEmpty()) {
                System.out.println("\n📊 Data:");
                printTable((List<Map<String, Object>>) dataList);
            }
        }

        System.out.println("\n--------------------------------------------------");
    }

    public List<Map<String, Object>> getData(String query) {

        String sessionId = UUID.randomUUID().toString();
        String userId = "AGENT";

        AssistantRequest request = new AssistantRequest();
        request.setQuestion(query);
        request.setSessionId(sessionId);
        request.setAnalystId(userId);
        request.setAgentMode(true);

        AssistantResponse response = service.processQuery(request);

        if (response.getData() instanceof List<?>) {
            return (List<Map<String, Object>>) response.getData();
        }

        return List.of();
    }

    public String executeWithResponse(String query) {

        String sessionId = UUID.randomUUID().toString();
        String userId = "AGENT";

        System.out.println("\n🧠 Agent Received: " + query);

        AssistantRequest request = new AssistantRequest();
        request.setQuestion(query);
        request.setSessionId(sessionId);
        request.setAnalystId(userId);

        AssistantResponse response = service.processQuery(request);

        if (response.getAnswer() != null) {
            System.out.println("\n🤖 AI Response:");
            System.out.println(response.getAnswer());
            return response.getAnswer(); // 🔥 RETURN IT
        }

        return "";
    }

    public List<Map<String, Object>> getFullData(String query) {

        String sessionId = UUID.randomUUID().toString();
        String userId = "AGENT";

        AssistantRequest request = new AssistantRequest();
        request.setQuestion(query + " all"); // 🔥 trick to bypass LIMIT logic
        request.setSessionId(sessionId);
        request.setAnalystId(userId);

        AssistantResponse response = service.processQuery(request);

        if (response.getData() instanceof List<?>) {
            return (List<Map<String, Object>>) response.getData();
        }

        return List.of();
    }



    @SuppressWarnings("unchecked")
    private void printTable(List<Map<String, Object>> data) {

        if (data == null || data.isEmpty()) {
            System.out.println("No data found.");
            return;
        }

        Set<String> headers = data.get(0).keySet();

        headers.forEach(h -> System.out.printf("%-20s", h.toUpperCase()));
        System.out.println();

        System.out.println("------------------------------------------------------------");

        for (Map<String, Object> row : data) {
            for (String h : headers) {
                System.out.printf("%-20s", String.valueOf(row.get(h)));
            }
            System.out.println();
        }
    }


}