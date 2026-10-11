package com.agrisaarthi.agrichat_service.dto;

import java.util.List;
import java.util.Map;

public record KnowledgeResponse(
        List<EvidenceItem> evidence,
        Map<String, Object> weather,
        List<SourceReference> sources,
        String retrievalStatus
) {
    public record EvidenceItem(String text, String title, String url) {}

    public static KnowledgeResponse unavailable() {
        return new KnowledgeResponse(List.of(), Map.of(), List.of(), "UNAVAILABLE");
    }
}
