package com.agrisaarthi.agrichat_service.service;

import org.springframework.stereotype.Service;
import java.util.Locale;
import java.util.Map;

@Service
public class QueryClassifier {
    private static final Map<String, String[]> KEYWORDS = Map.of(
        "pest_disease", new String[]{"pest", "disease", "insect", "fungus", "fungal", "leaf spot", "பூச்சி", "நோய்", "இலை"},
        "weather", new String[]{"weather", "rain", "rainfall", "temperature", "forecast", "வானிலை", "மழை", "வெப்பநிலை"},
        "market_prices", new String[]{"market", "price", "mandi", "sell", "rate", "விலை", "சந்தை", "மார்க்கெட்"},
        "government_schemes", new String[]{"scheme", "subsidy", "government", "insurance", "pm-kisan", "திட்டம்", "மானியம்", "காப்பீடு"},
        "cultivation", new String[]{"cultivation", "fertilizer", "irrigation", "sowing", "soil", "seed", "நடவு", "உரம்", "நீர்ப்பாசனம்", "விதை"},
        "crop_selection", new String[]{"crop", "which crop", "grow", "plant", "விவசாயம்", "பயிர்", "சாகுபடி"}
    );

    public String classify(String query) {
        String q = query.toLowerCase(Locale.ROOT);
        for (var entry : KEYWORDS.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (q.contains(keyword)) return entry.getKey();
            }
        }
        return "general_agriculture";
    }
}
