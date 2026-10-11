package com.agrisaarthi.agrichat_service;

import com.agrisaarthi.agrichat_service.controller.ApiExceptionHandler;
import com.agrisaarthi.agrichat_service.controller.ChatController;
import com.agrisaarthi.agrichat_service.dto.ChatRequest;
import com.agrisaarthi.agrichat_service.dto.ChatResponse;
import com.agrisaarthi.agrichat_service.dto.SourceReference;
import com.agrisaarthi.agrichat_service.service.AiAnswerService;
import com.agrisaarthi.agrichat_service.service.ChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {ChatController.class, ApiExceptionHandler.class})
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ChatService chatService;

    @Test
    void infoEndpointReturnsSuccess() throws Exception {
        mockMvc.perform(get("/api/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("AGRI-CHAT-SERVICE"));
    }

    @Test
    void chatRejectsInvalidCoordinates() throws Exception {
        ChatRequest req = new ChatRequest();
        req.setMessage("Valid message");
        req.setLatitude(95.0); // Invalid: > 90

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("latitude")));
    }

    @Test
    void chatRejectsEmptyEffectiveQuery() throws Exception {
        ChatRequest req = new ChatRequest();
        req.setMessage("   ");

        when(chatService.chat(any(ChatRequest.class)))
                .thenThrow(new IllegalArgumentException("Provide a non-empty 'message' or 'query'."));

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Provide a non-empty 'message' or 'query'."));
    }

    @Test
    void chatReturns503WhenAiUnavailable() throws Exception {
        ChatRequest req = new ChatRequest();
        req.setMessage("Tomato crop care");

        when(chatService.chat(any(ChatRequest.class)))
                .thenThrow(new AiAnswerService.AiUnavailableException("GEMINI_API_KEY is not configured."));

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.detail").value("A required service is unavailable. Check the service configuration and try again."));
    }

    @Test
    void chatReturns503WhenKnowledgeServiceUnavailable() throws Exception {
        ChatRequest req = new ChatRequest();
        req.setMessage("Tomato crop care");

        when(chatService.chat(any(ChatRequest.class)))
                .thenThrow(new ChatService.IntegrationUnavailableException("Knowledge service down"));

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503));
    }

    @Test
    void chatReturnsSuccessWhenOrchestrated() throws Exception {
        ChatRequest req = new ChatRequest();
        req.setMessage("Tomato fertilizer tips");

        ChatResponse mockResponse = new ChatResponse(
                1L, "conv-101", "Tomato fertilizer tips", "en", "cultivation",
                "Apply NPK 10:26:26 according to soil test recommendations.",
                List.of(new SourceReference("TNAU Agritech", "https://agritech.tnau.ac.in", "extension")),
                "SUCCESS", Instant.now());

        when(chatService.chat(any(ChatRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversationId").value("conv-101"))
                .andExpect(jsonPath("$.answer").value("Apply NPK 10:26:26 according to soil test recommendations."))
                .andExpect(jsonPath("$.sources[0].title").value("TNAU Agritech"))
                .andExpect(jsonPath("$.retrievalStatus").value("SUCCESS"));
    }

    @Test
    void historyReturnsBadRequestWhenConversationIdEmpty() throws Exception {
        mockMvc.perform(get("/api/chat/history").param("conversationId", "  "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("conversationId is required"));
    }

    @Test
    void historyReturnsListWhenValid() throws Exception {
        ChatResponse item = new ChatResponse(
                1L, "conv-101", "Tomato query", "en", "pest_disease",
                "Check for leaf spots.", List.of(), "SUCCESS", Instant.now());

        when(chatService.history("conv-101")).thenReturn(List.of(item));

        mockMvc.perform(get("/api/chat/history").param("conversationId", "conv-101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].conversationId").value("conv-101"))
                .andExpect(jsonPath("$[0].answer").value("Check for leaf spots."));
    }
}
