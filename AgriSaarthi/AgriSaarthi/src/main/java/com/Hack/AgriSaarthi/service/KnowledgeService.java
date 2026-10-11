
package com.Hack.AgriSaarthi.service;

import com.Hack.AgriSaarthi.model.KnowledgeDocument;
import com.Hack.AgriSaarthi.model.KnowledgeRequest;
import com.Hack.AgriSaarthi.model.KnowledgeResponse;
import com.Hack.AgriSaarthi.model.SourceReference;
import com.Hack.AgriSaarthi.model.WeatherInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class KnowledgeService {

    private final ApplicationContext applicationContext;
    private final WeatherService weatherService;
    private final String documentPattern;
    private final int minimumScore;

    public KnowledgeService(ApplicationContext applicationContext, 
                            WeatherService weatherService,
                            @Value("${knowledge.documents.pattern}") String documentPattern,
                            @Value("${knowledge.retrieval.minimum-score:2}") int minimumScore) {
        this.applicationContext = applicationContext;
        this.weatherService = weatherService;
        this.documentPattern = documentPattern;
        this.minimumScore = minimumScore;
    }

    public KnowledgeResponse search(KnowledgeRequest request) {
        WeatherInfo weather = null;

        if (request.latitude() != null && request.longitude() != null) {
            weather = weatherService.getCurrentWeather(
                    request.latitude(), request.longitude());
        }

        List<KnowledgeDocument> documents;

        try {
            documents = loadDocuments();
        } catch (IOException exception) {
            return new KnowledgeResponse(
    List.of(), weather, List.of(), "KNOWLEDGE_UNAVAILABLE"
);
        }

        List<String> queryTerms = terms(request.query());

        List<Match> matches = documents.stream()
                .map(document -> new Match(
                        document, score(document, queryTerms, request)))
                .filter(match -> match.score() >= minimumScore)
                .sorted(Comparator.comparingInt(Match::score).reversed())
                .limit(3)
                .toList();

        if (matches.isEmpty()) {
            return new KnowledgeResponse(
    List.of(), weather, List.of(), "NO_MATCH"
);
        }

        List<KnowledgeResponse.EvidenceItem> evidence = matches.stream()
                .map(match -> new KnowledgeResponse.EvidenceItem(
                        match.document().content(),
                        match.document().title(),
                        match.document().url()))
                .toList();

        List<SourceReference> sources = matches.stream()
                .map(match -> new SourceReference(
                        match.document().title(),
                        match.document().url(),
                        match.document().type()))
                .distinct()
                .toList();

        return new KnowledgeResponse(
                evidence, weather, sources, "FOUND");
    }

    private List<KnowledgeDocument> loadDocuments() throws IOException {
        Resource[] resources = applicationContext.getResources(documentPattern);
        List<KnowledgeDocument> documents = new ArrayList<>();
        
        for (Resource resource : resources) {
            if (!resource.exists() || !resource.isReadable()) {
                continue;
            }
            String text;
            try (var input = resource.getInputStream()) {
                text = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            }
            KnowledgeDocument document = parseDocument(text);
            if (document != null && !document.content().isBlank()) {
                documents.add(document);
            }
        }
        return documents;
    }

    private KnowledgeDocument parseDocument(String text) {
        String[] parts = text.split("\\R\\s*\\R", 2);

        if (parts.length < 2) {
            return null;
        }

        Map<String, String> metadata = new HashMap<>();

        for (String line : parts[0].split("\\R")) {
            int separator = line.indexOf(':');

            if (separator > 0) {
                metadata.put(
                        line.substring(0, separator).trim().toLowerCase(Locale.ROOT),
                        line.substring(separator + 1).trim());
            }
        }

        String title = metadata.get("title");
        String url = metadata.get("url");
        String type = metadata.getOrDefault("type", "agriculture");
        String content = parts[1].trim();

        if (title == null || title.isBlank()
                || url == null || url.isBlank()
                || content.isBlank()) {
            return null;
        }

        return new KnowledgeDocument(title, url, type, content);
    }

    private int score(
            KnowledgeDocument document,
            List<String> queryTerms,
            KnowledgeRequest request) {

        String content = (document.title() + " " + document.content())
                .toLowerCase(Locale.ROOT);

        int score = 0;

        for (String term : queryTerms) {
            if (content.contains(term)) {
                score++;
            }
        }

        String crop = request.crop();
        if (crop != null && !crop.isBlank()
                && content.contains(crop.toLowerCase(Locale.ROOT))) {
            score += 2;
        }

        return score;
    }

    private List<String> terms(String text) {
        return Arrays.stream(text.toLowerCase(Locale.ROOT)
                        .split("[^\\p{L}\\p{N}]+"))
                .filter(term -> term.length() > 1)
                .distinct()
                .toList();
    }

    private record Match(KnowledgeDocument document, int score) {}
}
