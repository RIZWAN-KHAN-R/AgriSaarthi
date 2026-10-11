package com.agrisaarthi.agrichat_service;

import com.agrisaarthi.agrichat_service.dto.*;
import com.agrisaarthi.agrichat_service.model.ChatInteraction;
import com.agrisaarthi.agrichat_service.repository.ChatInteractionRepository;
import com.agrisaarthi.agrichat_service.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private LanguageService languageService;

    @Mock
    private QueryClassifier classifier;

    @Mock
    private KnowledgeClient knowledgeClient;

    @Mock
    private AiAnswerService aiAnswerService;

    @Mock
    private ChatInteractionRepository repository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private ChatService chatService;

    @BeforeEach
    void setUp() {
        chatService = new ChatService(languageService, classifier, knowledgeClient, aiAnswerService, repository, objectMapper);
    }

    @Test
    void rejectsEmptyQuery() {
        ChatRequest req = new ChatRequest();
        req.setMessage("   ");
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> chatService.chat(req));
        assertTrue(ex.getMessage().contains("Provide a non-empty"));
    }

    @Test
    void rejectsNullQuery() {
        ChatRequest req = new ChatRequest();
        assertThrows(IllegalArgumentException.class, () -> chatService.chat(req));
    }

    @Test
    void successfulChatOrchestrationAndPersistence() {
        ChatRequest req = new ChatRequest();
        req.setMessage("How to control tomato blight?");
        req.setCrop("tomato");
        req.setLanguage("en");

        when(languageService.detect("How to control tomato blight?", "en")).thenReturn("en");
        when(classifier.classify("How to control tomato blight?")).thenReturn("pest_disease");

        KnowledgeResponse.EvidenceItem evidenceItem = new KnowledgeResponse.EvidenceItem(
                "Use copper fungicide spray.", "TNAU Advisory", "https://tnau.ac.in/blight");
        SourceReference knowledgeSource = new SourceReference("TNAU Advisory", "https://tnau.ac.in/blight", "extension_doc");
        KnowledgeResponse knowledgeResponse = new KnowledgeResponse(
                List.of(evidenceItem), Map.of(), List.of(knowledgeSource), "SUCCESS");
        when(knowledgeClient.retrieve(any())).thenReturn(knowledgeResponse);

        SourceReference searchSource = new SourceReference("ICAR Portal", "https://icar.org.in/tomato", "gemini_google_search");
        AiAnswerService.AiResult aiResult = new AiAnswerService.AiResult("Apply copper fungicide as per package label.", List.of(searchSource));
        when(aiAnswerService.answer(eq("How to control tomato blight?"), eq("en"), eq("pest_disease"), anyString()))
                .thenReturn(aiResult);

        ChatInteraction savedEntity = new ChatInteraction();
        savedEntity.setConversationId("conv-123");
        savedEntity.setQueryText(req.getMessage());
        savedEntity.setDetectedLanguage("en");
        savedEntity.setCategory("pest_disease");
        savedEntity.setAnswerText(aiResult.answer());
        savedEntity.setRetrievalStatus("SUCCESS");
        when(repository.save(any(ChatInteraction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChatResponse response = chatService.chat(req);

        assertNotNull(response);
        assertEquals("How to control tomato blight?", response.query());
        assertEquals("en", response.detectedLanguage());
        assertEquals("pest_disease", response.category());
        assertEquals("Apply copper fungicide as per package label.", response.answer());
        assertEquals("SUCCESS", response.retrievalStatus());
        assertNotNull(response.conversationId());
        assertEquals(2, response.sources().size()); // 1 from knowledge + 1 from search

        // Verify entity persisted
        ArgumentCaptor<ChatInteraction> captor = ArgumentCaptor.forClass(ChatInteraction.class);
        verify(repository).save(captor.capture());
        ChatInteraction entity = captor.getValue();
        assertEquals("pest_disease", entity.getCategory());
        assertTrue(entity.getSourcesJson().contains("https://tnau.ac.in/blight"));
        assertTrue(entity.getSourcesJson().contains("https://icar.org.in/tomato"));
    }

    @Test
    void reusesProvidedConversationId() {
        ChatRequest req = new ChatRequest();
        req.setMessage("What about watering?");
        req.setConversationId("my-existing-conv-456");

        when(languageService.detect(anyString(), any())).thenReturn("en");
        when(classifier.classify(anyString())).thenReturn("cultivation");
        when(knowledgeClient.retrieve(any())).thenReturn(new KnowledgeResponse(List.of(), Map.of(), List.of(), "SUCCESS"));
        when(aiAnswerService.answer(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(new AiAnswerService.AiResult("Water once every 3 days.", List.of()));
        when(repository.save(any(ChatInteraction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChatResponse response = chatService.chat(req);
        assertEquals("my-existing-conv-456", response.conversationId());
    }

    @Test
    void throwsWhenKnowledgeServiceReturnsNull() {
        ChatRequest req = new ChatRequest();
        req.setMessage("Test query");

        when(languageService.detect(anyString(), any())).thenReturn("en");
        when(classifier.classify(anyString())).thenReturn("general_agriculture");
        when(knowledgeClient.retrieve(any())).thenReturn(null);

        assertThrows(ChatService.IntegrationUnavailableException.class, () -> chatService.chat(req));
        verify(repository, never()).save(any());
    }

    @Test
    void propagatesAiUnavailableExceptionWithoutSaving() {
        ChatRequest req = new ChatRequest();
        req.setMessage("Test query");

        when(languageService.detect(anyString(), any())).thenReturn("en");
        when(classifier.classify(anyString())).thenReturn("general_agriculture");
        when(knowledgeClient.retrieve(any())).thenReturn(new KnowledgeResponse(List.of(), Map.of(), List.of(), "SUCCESS"));
        when(aiAnswerService.answer(anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new AiAnswerService.AiUnavailableException("GEMINI_API_KEY is not configured."));

        assertThrows(AiAnswerService.AiUnavailableException.class, () -> chatService.chat(req));
        verify(repository, never()).save(any());
    }

    @Test
    void retrievesHistoryCorrectly() {
        ChatInteraction item = new ChatInteraction();
        item.setConversationId("conv-99");
        item.setQueryText("How much water?");
        item.setDetectedLanguage("en");
        item.setCategory("cultivation");
        item.setAnswerText("2 inches per week.");
        item.setRetrievalStatus("SUCCESS");
        item.setSourcesJson("[{\"title\":\"Water Guide\",\"url\":\"https://example.org/water\",\"type\":\"guide\"}]");

        when(repository.findTop50ByConversationIdOrderByCreatedAtDesc("conv-99"))
                .thenReturn(List.of(item));

        List<ChatResponse> history = chatService.history("conv-99");
        assertEquals(1, history.size());
        assertEquals("How much water?", history.get(0).query());
        assertEquals(1, history.get(0).sources().size());
        assertEquals("https://example.org/water", history.get(0).sources().get(0).url());
    }
}
