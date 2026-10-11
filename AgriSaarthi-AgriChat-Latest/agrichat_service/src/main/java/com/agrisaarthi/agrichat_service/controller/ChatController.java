package com.agrisaarthi.agrichat_service.controller;

import com.agrisaarthi.agrichat_service.dto.ChatRequest;
import com.agrisaarthi.agrichat_service.dto.ChatResponse;
import com.agrisaarthi.agrichat_service.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ChatController {
    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/chat")
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        return chatService.chat(request);
    }

    @GetMapping("/chat/history")
    public List<ChatResponse> history(@RequestParam String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            throw new IllegalArgumentException("conversationId is required");
        }
        return chatService.history(conversationId.trim());
    }

    @GetMapping("/info")
    public Map<String, String> info() {
        return Map.of("service", "AGRI-CHAT-SERVICE", "status", "UP", "api", "/api/chat");
    }
}
