package com.wipro.fraud.aiassistant.service;

import com.wipro.fraud.aiassistant.dto.AssistantRequest;
import com.wipro.fraud.aiassistant.dto.AssistantResponse;
import java.util.List;
import java.util.Map;

public interface FraudAssistantService {

    AssistantResponse processQuery(AssistantRequest request);
    List<Map<String, Object>> getAllUsers();


}