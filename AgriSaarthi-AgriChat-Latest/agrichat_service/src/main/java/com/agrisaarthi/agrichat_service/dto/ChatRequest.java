package com.agrisaarthi.agrichat_service.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

public class ChatRequest {

    @Size(max = 10000, message = "Question must be at most 10000 characters")
    private String message;

    @Size(max = 10000, message = "Question must be at most 10000 characters")
    private String query;

    @Size(max = 100, message = "conversationId must be at most 100 characters")
    private String conversationId;

    @Size(max = 10, message = "language must be at most 10 characters")
    private String language;

    @Size(max = 120, message = "crop must be at most 120 characters")
    private String crop;

    @DecimalMin(value = "-90.0", message = "latitude must be at least -90")
    @DecimalMax(value = "90.0", message = "latitude must be at most 90")
    private Double latitude;

    @DecimalMin(value = "-180.0", message = "longitude must be at least -180")
    @DecimalMax(value = "180.0", message = "longitude must be at most 180")
    private Double longitude;

    public String effectiveQuery() {
        return message != null && !message.isBlank()
                ? message.trim()
                : query == null ? "" : query.trim();
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getConversationId() {
        return conversationId;
    }

    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getCrop() {
        return crop;
    }

    public void setCrop(String crop) {
        this.crop = crop;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }
}
