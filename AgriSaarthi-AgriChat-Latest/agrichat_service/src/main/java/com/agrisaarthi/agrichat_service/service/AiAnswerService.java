
package com.agrisaarthi.agrichat_service.service;

import com.agrisaarthi.agrichat_service.dto.SourceReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiAnswerService {
    private final RestClient restClient;
    private final ObjectMapper mapper;
    private final String apiKey;
    private final String model;

    public AiAnswerService(RestClient.Builder builder, ObjectMapper mapper,
            @Value("${agrisaarthi.ai.api-key:}") String apiKey,
            @Value("${agrisaarthi.ai.model:gemini-2.5-flash}") String model) {
        this.restClient = builder.baseUrl("https://generativelanguage.googleapis.com").build();
        this.mapper = mapper;
        this.apiKey = apiKey;
        this.model = model;
    }

    public AiResult answer(String query, String language, String category, String evidence) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new AiUnavailableException("GEMINI_API_KEY is not configured. Set it before calling /api/chat.");
        }

        String languageName = switch (language == null ? "" : language.trim().toLowerCase()) {
            case "ta" -> "Tamil";
            case "hi" -> "Hindi";
            case "te" -> "Telugu";
            case "kn" -> "Kannada";
            case "ml" -> "Malayalam";
            case "mr" -> "Marathi";
            case "bn" -> "Bengali";
            case "gu" -> "Gujarati";
            case "pa" -> "Punjabi";
            case "or" -> "Odia";
            case "en", "" -> "English";
            default -> language;
        };
        String prompt = """
            You are AgriSaarthi, an agricultural information assistant.
            Answer in %s. Query category: %s.
            Use the provided retrieved agricultural evidence first. Google Search grounding is enabled
            to find current, relevant public information. Prefer official agricultural universities,
            government agriculture departments, ICAR, IMD, and official scheme portals.
            Never invent pesticide doses, market prices, weather readings, eligibility rules, or URLs.
            If reliable evidence is insufficient or sources disagree, say so and advise the farmer to
            contact the local agriculture extension office. Keep the answer practical.

            Farmer's question:
            %s

            Evidence from AGRI-KNOWLEDGE-SERVICE:
            %s
            """.formatted(languageName, category, query, evidence == null || evidence.isBlank()
                ? "No evidence was returned by the internal knowledge service." : evidence);

        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("contents", List.of(Map.of("role", "user",
                    "parts", List.of(Map.of("text", prompt)))));
            body.put("tools", List.of(Map.of("google_search", Map.of())));
            JsonNode response = restClient.post()
                    .uri("/v1beta/models/{model}:generateContent?key={key}", model, apiKey)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            JsonNode candidates = response == null ? null : response.path("candidates");
            if (candidates == null || !candidates.isArray() || candidates.isEmpty()) {
                throw new AiUnavailableException("Gemini returned no answer candidates.");
            }
            JsonNode candidate = candidates.get(0);
            StringBuilder answer = new StringBuilder();
            JsonNode parts = candidate.path("content").path("parts");
            if (parts.isArray()) {
                for (JsonNode part : parts) {
                    if (part.hasNonNull("text")) answer.append(part.get("text").asText());
                }
            }
            if (answer.isEmpty()) throw new AiUnavailableException("Gemini returned an empty answer.");

            List<SourceReference> groundedSources = new ArrayList<>();
            JsonNode chunks = candidate.path("groundingMetadata").path("groundingChunks");
            if (chunks.isArray()) {
                for (JsonNode chunk : chunks) {
                    JsonNode web = chunk.path("web");
                    String uri = web.path("uri").asText("");
                    String title = web.path("title").asText("Web source");
                    if (!uri.isBlank() && (uri.startsWith("https://") || uri.startsWith("http://"))) {
                        groundedSources.add(new SourceReference(title, uri, "gemini_google_search"));
                    }
                }
            }
            return new AiResult(answer.toString().trim(), groundedSources);
        } catch (AiUnavailableException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AiUnavailableException("Live Gemini request failed: " + ex.getMessage(), ex);
        }
    }

    public record AiResult(String answer, List<SourceReference> sources) {}

    public static class AiUnavailableException extends RuntimeException {
        public AiUnavailableException(String message) { super(message); }
        public AiUnavailableException(String message, Throwable cause) { super(message, cause); }
    }
}
