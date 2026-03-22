package com.wipro.fraud.aiassistant;

import com.wipro.fraud.aiassistant.entity.ConversationMemory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversationMemoryRepository
        extends JpaRepository<ConversationMemory,Long> {

    List<ConversationMemory> findTop10BySessionIdOrderByCreatedAtAsc(String sessionId);

}
