package com.Hack.AgriSaarthi.model;

import java.util.List;

public record KnowledgeResponse(
    List<EvidenceItem> evidence,
    WeatherInfo weather,
    List<SourceReference> sources,
    String retrievalStatus
) {
    public record EvidenceItem(
        String text,
        String title,
        String url
    ) {}
}