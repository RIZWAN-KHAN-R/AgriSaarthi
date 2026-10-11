package com.agrisaarthi.agrichat_service.dto;

public record KnowledgeRequest(
        String query,
        String language,
        Double latitude,
        Double longitude,
        String crop,
        String category
) {}
