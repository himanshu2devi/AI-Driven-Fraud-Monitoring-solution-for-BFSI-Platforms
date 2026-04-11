package com.wipro.fraud.aiassistant.service;

import com.wipro.fraud.aiassistant.repository.ConversationMemoryRepository;
import com.wipro.fraud.aiassistant.entity.ConversationMemory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConversationMemoryService {

    private final ConversationMemoryRepository repository;

    // 🔹 OLD METHOD (keep it if needed)
    public List<ConversationMemory> getConversation(String sessionId){
        return repository
                .findTop10BySessionIdOrderByCreatedAtAsc(sessionId);
    }

    // 🔥 NEW METHOD (USER BASED)
    public List<ConversationMemory> getConversation(String sessionId, String userId){
        return repository
                .findTop10BySessionIdAndUserIdOrderByCreatedAtAsc(sessionId, userId);
    }

    public List<String> getRecentUserQueries(String userId) {

        return repository
                .findTop5ByUserIdAndRoleOrderByCreatedAtDesc(userId, "USER")
                .stream()
                .map(ConversationMemory::getMessage)
                .toList();
    }

    public void saveMessage(String sessionId, String userId, String role, String message) {

        ConversationMemory memory = new ConversationMemory();
        memory.setSessionId(sessionId);
        memory.setUserId(userId);
        memory.setRole(role);
        memory.setMessage(message);

        repository.save(memory);
    }
}