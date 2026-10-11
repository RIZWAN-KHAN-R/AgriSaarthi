
package com.Hack.AgriSaarthi.model;

public record KnowledgeRequest(
    String query,
    String language,
    Double latitude,
    Double longitude,
    String crop,
    String category
) {}
