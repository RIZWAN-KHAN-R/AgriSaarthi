package com.agrisaarthi.agrichat_service;

import com.agrisaarthi.agrichat_service.service.LanguageService;
import com.agrisaarthi.agrichat_service.service.QueryClassifier;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AgrichatServiceApplicationTests {
    @Test
    void detectsTamilScript() {
        assertEquals("ta", new LanguageService().detect("தக்காளி பயிர்", null));
    }

    @Test
    void respectsRequestedEnglishLanguage() {
        assertEquals("en", new LanguageService().detect("வணக்கம்", "en"));
    }

    @Test
    void detectsDevanagariHindiScript() {
        assertEquals("hi", new LanguageService().detect("टमाटर में कीड़े कैसे रोकें?", null));
    }

    @Test
    void detectsTeluguScript() {
        assertEquals("te", new LanguageService().detect("టమాట తెగులు నివారణ ఎలా?", null));
    }

    @Test
    void respectsRequestedHindiLanguage() {
        assertEquals("hi", new LanguageService().detect("How to grow rice?", "hi"));
    }

    @Test
    void classifiesWeatherQuestion() {
        assertEquals("weather", new QueryClassifier().classify("Will it rain tomorrow in Madurai?"));
    }

    @Test
    void classifiesMarketPricesQuestion() {
        assertEquals("market_prices", new QueryClassifier().classify("What is the tomato mandi price today?"));
    }

    @Test
    void classifiesGovernmentSchemesQuestion() {
        assertEquals("government_schemes", new QueryClassifier().classify("How to apply for PM-Kisan subsidy scheme?"));
    }
}
