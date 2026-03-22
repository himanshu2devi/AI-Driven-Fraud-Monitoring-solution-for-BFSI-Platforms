package com.wipro.fraud.aiassistant.service;

import com.wipro.fraud.aiassistant.ConversationMemoryRepository;
import com.wipro.fraud.aiassistant.entity.ConversationMemory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConversationMemoryService {

    private final ConversationMemoryRepository repository;

    public List<ConversationMemory> getConversation(String sessionId){

        return repository
                .findTop10BySessionIdOrderByCreatedAtAsc(sessionId);

    }

    public void saveMessage(String sessionId,String role,String message){

        ConversationMemory memory = new ConversationMemory();

        memory.setSessionId(sessionId);
        memory.setRole(role);
        memory.setMessage(message);

        repository.save(memory);
    }
}
