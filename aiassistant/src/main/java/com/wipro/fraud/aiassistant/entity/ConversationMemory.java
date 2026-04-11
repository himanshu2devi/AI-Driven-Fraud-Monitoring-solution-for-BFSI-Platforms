package com.wipro.fraud.aiassistant.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import jakarta.persistence.PrePersist;

@Entity
@Table(name="conversation_memory")
@Data
public class ConversationMemory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sessionId;

    private String role;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(name = "user_id")
    private String userId;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
