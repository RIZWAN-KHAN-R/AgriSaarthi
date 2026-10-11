package com.agrisaarthi.agrichat_service.service;

import org.springframework.stereotype.Service;

@Service
public class LanguageService {
    public String detect(String text, String requestedLanguage) {
        if (requestedLanguage != null && !requestedLanguage.isBlank()) {
            String lang = requestedLanguage.trim().toLowerCase();
            return switch (lang) {
                case "ta", "tamil" -> "ta";
                case "hi", "hindi" -> "hi";
                case "te", "telugu" -> "te";
                case "kn", "kannada" -> "kn";
                case "ml", "malayalam" -> "ml";
                case "mr", "marathi" -> "mr";
                case "bn", "bengali" -> "bn";
                case "gu", "gujarati" -> "gu";
                case "pa", "punjabi" -> "pa";
                case "or", "odia" -> "or";
                case "en", "english" -> "en";
                default -> lang;
            };
        }
        // Tamil Unicode block: U+0B80–U+0BFF. Devanagari block: U+0900–U+097F (Hindi/Marathi).
        // Telugu: U+0C00–U+0C7F. Kannada: U+0C80–U+0CFF. Malayalam: U+0D00–U+0D7F.
        if (text != null) {
            if (text.codePoints().anyMatch(c -> c >= 0x0B80 && c <= 0x0BFF)) return "ta";
            if (text.codePoints().anyMatch(c -> c >= 0x0900 && c <= 0x097F)) return "hi";
            if (text.codePoints().anyMatch(c -> c >= 0x0C00 && c <= 0x0C7F)) return "te";
            if (text.codePoints().anyMatch(c -> c >= 0x0C80 && c <= 0x0CFF)) return "kn";
            if (text.codePoints().anyMatch(c -> c >= 0x0D00 && c <= 0x0D7F)) return "ml";
        }
        return "en";
    }
}
