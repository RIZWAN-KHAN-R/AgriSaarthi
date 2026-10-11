package com.agrisaarthi.agri_web;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

@RestController
@RequestMapping("/api")
public class AdvisoryApiController {

    private final RestClient restClient;
    private final String chatServiceUrl;

    public AdvisoryApiController(
            @org.springframework.beans.factory.annotation.Autowired(required = false) RestClient.Builder builder,
            @Value("${agrisaarthi.chat-service-url:http://localhost:8081}") String chatServiceUrl) {
        this.restClient = (builder != null ? builder : RestClient.builder()).build();
        this.chatServiceUrl = chatServiceUrl;
    }

    @PostMapping("/chat")
    public ResponseEntity<?> chat(@RequestBody(required = false) ChatRequest request) {
        if (request == null || request.message() == null || request.message().isBlank()) {
            return badRequest("Enter a farming question before sending it.");
        }
        if (request.message().length() > 2000) {
            return badRequest("Questions must be 2,000 characters or fewer.");
        }

        try {
            // Forward question to Member 2 (agrichat_service on port 8081)
            Map<?, ?> response = restClient.post()
                    .uri(chatServiceUrl + "/api/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "message", request.message(),
                            "language", request.language() != null ? request.language() : "en"
                    ))
                    .retrieve()
                    .body(Map.class);

            return ResponseEntity.ok(response);

        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(Map.of("error", "AgriChat service is offline or unreachable on port 8081."));
        }
    }

    @PostMapping("/crops/recommend")
    public ResponseEntity<?> recommendCrops(@RequestBody(required = false) CropRequest request) {
        if (request == null) {
            return badRequest("Enter the soil and climate measurements first.");
        }
        if (!within(request.nitrogen(), 0, 200)
                || !within(request.phosphorus(), 0, 200)
                || !within(request.potassium(), 0, 300)
                || !within(request.ph(), 0, 14)
                || !within(request.temperature(), -10, 60)
                || !within(request.humidity(), 0, 100)
                || !within(request.rainfall(), 0, 5000)) {
            return badRequest("Check that all measurements are present and within the ranges shown on the form.");
        }

        List<CropProfile> profiles = List.of(
                new CropProfile("Rice", 80, 150, 30, 80, 30, 100, 5.0, 7.5, 20, 35, 60, 100, 100, 400),
                new CropProfile("Maize", 60, 150, 25, 70, 25, 80, 5.5, 7.5, 18, 32, 40, 85, 50, 200),
                new CropProfile("Sorghum", 40, 100, 20, 50, 20, 60, 5.5, 8.0, 25, 38, 25, 75, 30, 120),
                new CropProfile("Cotton", 60, 120, 25, 65, 25, 80, 5.5, 8.0, 21, 35, 40, 85, 50, 180),
                new CropProfile("Chickpea", 20, 60, 20, 55, 20, 60, 6.0, 8.0, 15, 30, 25, 75, 30, 120),
                new CropProfile("Groundnut", 20, 60, 25, 60, 25, 70, 5.5, 7.5, 20, 32, 40, 80, 50, 180));

        List<CropRecommendation> matches = profiles.stream()
                .map(profile -> new CropMatch(profile, score(profile, request)))
                .sorted(Comparator.comparingInt(CropMatch::score).reversed())
                .limit(3)
                .map(match -> new CropRecommendation(
                        match.profile().name(),
                        match.score() >= 75 ? "Closer broad-range match"
                                : match.score() >= 50 ? "Some broad-range overlap" : "Check local suitability",
                        "Screened against broad soil and climate ranges; confirm season, water availability, soil test, and local guidance."))
                .toList();

        return ResponseEntity.ok(new CropResponse(
                matches,
                "This is a simple range-based screening, not a trained prediction. It does not account for season, variety, drainage, local pests, or current market conditions."));
    }

    @PostMapping("/diseases/check")
    public ResponseEntity<?> checkSymptoms(@RequestBody(required = false) DiseaseRequest request) {
        if (request == null || request.symptoms() == null || request.symptoms().isBlank()) {
            return badRequest("Describe the symptoms you can see.");
        }
        if (request.symptoms().length() > 1000) {
            return badRequest("Keep the symptom description to 1,000 characters or fewer.");
        }

        String symptoms = request.symptoms().toLowerCase(Locale.ROOT);
        String issue;
        List<String> steps;
        if (containsAny(symptoms, "powder", "white coating", "white growth")) {
            issue = "A white surface growth can be consistent with powdery mildew, but other causes are possible.";
            steps = List.of("Check several leaves, including their undersides, and note whether the growth wipes off.", "Avoid overhead watering late in the day and improve airflow where practical.", "Ask a local plant-health adviser to confirm the cause before choosing a treatment.");
        } else if (containsAny(symptoms, "spot", "brown mark", "black mark", "lesion")) {
            issue = "Leaf spots have many possible causes; this description alone cannot distinguish infection from other damage.";
            steps = List.of("Photograph affected and healthy leaves in good light and note whether spots are spreading.", "Avoid handling wet plants and remove severely affected material only if local guidance recommends it.", "Share the crop, recent weather, and a clear image with an agricultural extension adviser.");
        } else if (containsAny(symptoms, "wilt", "droop", "collapse")) {
            issue = "Wilting can result from dry soil, waterlogging, root injury, or disease; it is not specific to one cause.";
            steps = List.of("Check moisture below the surface and inspect whether nearby plants are also affected.", "Check for standing water or root/stem damage before changing irrigation.", "Seek local diagnosis if plants remain wilted despite suitable soil moisture.");
        } else if (containsAny(symptoms, "yellow", "yellowing", "pale")) {
            issue = "Yellowing can be linked to nutrient stress, water problems, normal leaf ageing, or disease.";
            steps = List.of("Note whether older or newer leaves are affected and whether the pattern is uniform or patchy.", "Check soil moisture and review a recent soil test before applying fertilizer.", "Get local advice if symptoms are spreading or accompanied by spots or wilting.");
        } else {
            issue = "These symptoms do not clearly point to a single common category.";
            steps = List.of("Record the crop, plant part affected, and when symptoms began.", "Check whether only one plant or many plants across the field are affected.", "Consult a local agricultural extension office with photos or a fresh sample.");
        }

        return ResponseEntity.ok(new DiseaseResponse(issue, steps));
    }

    private ResponseEntity<?> badRequest(String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", message));
    }

    private static boolean containsAny(String text, String... words) {
        for (String word : words) {
            if (text.contains(word)) return true;
        }
        return false;
    }

    private static boolean within(Double val, double min, double max) {
        return val != null && val >= min && val <= max;
    }

    private static int score(CropProfile profile, CropRequest req) {
        int score = 0;
        if (within(req.nitrogen(), profile.nMin(), profile.nMax())) score += 15;
        if (within(req.phosphorus(), profile.pMin(), profile.pMax())) score += 15;
        if (within(req.potassium(), profile.kMin(), profile.kMax())) score += 15;
        if (within(req.ph(), profile.phMin(), profile.phMax())) score += 15;
        if (within(req.temperature(), profile.tMin(), profile.tMax())) score += 15;
        if (within(req.humidity(), profile.hMin(), profile.hMax())) score += 15;
        if (within(req.rainfall(), profile.rMin(), profile.rMax())) score += 10;
        return score;
    }

    public record ChatRequest(String message, String language) {}
    public record CropRequest(Double nitrogen, Double phosphorus, Double potassium, Double ph, Double temperature, Double humidity, Double rainfall) {}
    public record CropProfile(String name, double nMin,