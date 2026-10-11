
package com.agrisaarthi.agrichat_service.service;

import com.agrisaarthi.agrichat_service.dto.*;
import com.agrisaarthi.agrichat_service.model.ChatInteraction;
import com.agrisaarthi.agrichat_service.repository.ChatInteractionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ChatService {
    private final LanguageService languageService;
    private final QueryClassifier classifier;
    private final KnowledgeClient knowledgeClient;
    private final AiAnswerService aiAnswerService;
    private final ChatInteractionRepository repository;
    private final ObjectMapper objectMapper;

    public ChatService(LanguageService languageService, QueryClassifier classifier,
                       KnowledgeClient knowledgeClient, AiAnswerService aiAnswerService,
                       ChatInteractionRepository repository, ObjectMapper objectMapper) {
        this.languageService = languageService;
        this.classifier = classifier;
        this.knowledgeClient = knowledgeClient;
        this.aiAnswerService = aiAnswerService;
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ChatResponse chat(ChatRequest request) {
        String query = request.effectiveQuery();
        if (query.isBlank()) throw new IllegalArgumentException("Provide a non-empty 'message' or 'query'.");
        String language = languageService.detect(query, request.getLanguage());
        String category = classifier.classify(query);
        String conversationId = request.getConversationId() == null || request.getConversationId().isBlank()
                ? UUID.randomUUID().toString() : request.getConversationId().trim();

        KnowledgeRequest knowledgeRequest = new KnowledgeRequest(query, language, request.getLatitude(),
                request.getLongitude(), request.getCrop(), category);
        KnowledgeResponse knowledge = knowledgeClient.retrieve(knowledgeRequest);
        if (knowledge == null) {
            throw new IntegrationUnavailableException("Knowledge service returned an empty response.");
        }

        StringBuilder evidence = new StringBuilder();
        if (knowledge.evidence() != null) {
            for (KnowledgeResponse.EvidenceItem item : knowledge.evidence()) {
                if (item != null && item.text() != null && !item.text().isBlank()) {
                    evidence.append("- ");
                    if (item.title() != null) evidence.append(item.title()).append(": ");
                    evidence.append(item.text()).append('\n');
                }
            }
        }

        AiAnswerService.AiResult ai = aiAnswerService.answer(query, language, category, evidence.toString());
        List<SourceReference> sources = new ArrayList<>();
        if (knowledge.sources() != null) sources.addAll(knowledge.sources());
        for (SourceReference source : ai.sources()) {
            if (sources.stream().noneMatch(existing -> existing.url() != null && existing.url().equals(source.url()))) {
                sources.add(source);
            }
        }

        ChatInteraction entity = new ChatInteraction();
        entity.setConversationId(conversationId);
        entity.setQueryText(query);
        entity.setDetectedLanguage(language);
        entity.setCategory(category);
        entity.setAnswerText(ai.answer());
        entity.setRetrievalStatus(knowledge.retrievalStatus() == null ? "UNKNOWN" : knowledge.retrievalStatus());
        try {
            entity.setSourcesJson(objectMapper.writeValueAsString(sources));
        } catch (JsonProcessingException e) {
            entity.setSourcesJson("[]");
        }
        ChatInteraction saved = repository.save(entity);
        return new ChatResponse(saved.getId(), conversationId, query, language, category,
                ai.answer(), sources, saved.getRetrievalStatus(), saved.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public List<ChatResponse> history(String conversationId) {
        return repository.findTop50ByConversationIdOrderByCreatedAtDesc(conversationId).stream()
                .map(item -> {
                    List<SourceReference> sources;
                    try {
                        sources = item.getSourcesJson() == null ? List.of()
                                : objectMapper.readValue(item.getSourcesJson(),
                                    objectMapper.getTypeFactory().constructCollectionType(List.class, SourceReference.class));
                    } catch (Exception ex) {
                        sources = List.of();
                    }
                    return new ChatResponse(item.getId(), item.getConversationId(), item.getQueryText(),
                            item.getDetectedLanguage(), item.getCategory(), item.getAnswerText(), sources,
                            item.getRetrievalStatus(), item.getCreatedAt());
                }).toList();
    }

    public static class IntegrationUnavailableException extends RuntimeException {
        public IntegrationUnavailableException(String message) { super(message); }
    }
}
