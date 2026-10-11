package com.agrisaarthi.agrichat_service.dto;

import java.time.Instant;
import java.util.List;

public record ChatResponse(
        Long interactionId,
        String conversationId,
        String query,
        String detectedLanguage,
        String category,
        String answer,
        List<SourceReference> sources,
        String retrievalStatus,
        Instant createdAt
) {}
