package com.wipro.fraud.aiassistant.repository;

import com.wipro.fraud.aiassistant.entity.ConversationMemory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversationMemoryRepository
        extends JpaRepository<ConversationMemory,Long> {

    List<ConversationMemory> findTop10BySessionIdOrderByCreatedAtAsc(String sessionId);

    List<ConversationMemory> findTop10BySessionIdAndUserIdOrderByCreatedAtAsc(String sessionId, String userId);
    List<ConversationMemory> findTop5ByUserIdAndRoleOrderByCreatedAtDesc(
            String userId,
            String role
    );
}
