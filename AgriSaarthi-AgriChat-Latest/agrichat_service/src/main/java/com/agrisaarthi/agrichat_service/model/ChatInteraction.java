package com.agrisaarthi.agrichat_service.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "chat_interactions", indexes = {
        @Index(name = "idx_chat_conversation", columnList = "conversation_id"),
        @Index(name = "idx_chat_created", columnList = "created_at")
})
public class ChatInteraction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "conversation_id", nullable = false, length = 100)
    private String conversationId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String queryText;

    @Column(nullable = false, length = 10)
    private String detectedLanguage;

    @Column(nullable = false, length = 60)
    private String category;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String answerText;

    @Column(columnDefinition = "TEXT")
    private String sourcesJson;

    @Column(nullable = false, length = 30)
    private String retrievalStatus;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    void beforeInsert() {
        if (createdAt == null) createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }
    public String getQueryText() { return queryText; }
    public void setQueryText(String queryText) { this.queryText = queryText; }
    public String getDetectedLanguage() { return detectedLanguage; }
    public void setDetectedLanguage(String detectedLanguage) { this.detectedLanguage = detectedLanguage; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getAnswerText() { return answerText; }
    public void setAnswerText(String answerText) { this.answerText = answerText; }
    public String getSourcesJson() { return sourcesJson; }
    public void setSourcesJson(String sourcesJson) { this.sourcesJson = sourcesJson; }
    public String getRetrievalStatus() { return retrievalStatus; }
    public void setRetrievalStatus(String retrievalStatus) { this.retrievalStatus = retrievalStatus; }
    public Instant getCreatedAt() { return createdAt; }
}
