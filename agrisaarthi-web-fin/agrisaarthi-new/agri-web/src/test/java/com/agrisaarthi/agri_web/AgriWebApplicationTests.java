package com.agrisaarthi.agri_web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class AgriWebApplicationTests {

    @Autowired
    private WebApplicationContext applicationContext;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext).build();
    }

    @Test
    void allWorkspacePagesRender() throws Exception {
        for (String route : new String[] { "/", "/assistant", "/crops", "/diseases", "/weather", "/history" }) {
            mockMvc.perform(get(route))
                    .andExpect(status().isOk())
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"languageSelect\"")))
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("/js/language.js")));
        }
    }

    @Test
    void healthEndpointReportsApplicationStatus() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void marketRatesPageIsRemovedFromTheWebsite() throws Exception {
        mockMvc.perform(get("/market")).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/market/rates")).andExpect(status().isNotFound());
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Market rates"))));
    }

    @Test
    void assistantProvidesTextChatAndHasNoAudioControls() throws Exception {
        mockMvc.perform(get("/assistant"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"chatForm\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"messageInput\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"sendButton\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("chatVoiceButton"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("chatVoiceStatus"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Whisper"))));
    }

    @Test
    void chatAudioTranscriptionEndpointIsRemoved() throws Exception {
        mockMvc.perform(post("/api/chat/transcribe")).andExpect(status().isNotFound());
    }

    @Test
    void chatReturnsUsefulAdviceAndRejectsBlankQuestions() throws Exception {
        mockMvc.perform(post("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"message":"How should I manage irrigation during heavy rainfall?","language":"en"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("Water management"))
                .andExpect(jsonPath("$.answer").value(org.hamcrest.Matchers.containsString("soil moisture")));

        mockMvc.perform(post("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"message":"  ","language":"en"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        mockMvc.perform(post("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"message":"பாசனம் பற்றி சொல்லுங்கள்","language":"ta"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("நீர் மேலாண்மை"))
                .andExpect(jsonPath("$.answer").value(org.hamcrest.Matchers.containsString("நீர்ப்பாசனத்தை")));
    }

    @Test
    void cropScreeningReturnsMatchesAndValidatesMeasurements() throws Exception {
        mockMvc.perform(post("/api/crops/recommend")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nitrogen":90,"phosphorus":45,"potassium":45,"ph":6.5,"temperature":28,"humidity":70,"rainfall":180,"location":"Test district"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendations.length()").value(3))
                .andExpect(jsonPath("$.recommendations[0].crop").exists())
                .andExpect(jsonPath("$.note").value(org.hamcrest.Matchers.containsString("not a trained prediction")));

        mockMvc.perform(post("/api/crops/recommend")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"nitrogen":-1,"phosphorus":45,"potassium":45,"ph":6.5,"temperature":28,"humidity":70,"rainfall":180}
                        """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void symptomTriageReturnsHonestGuidance() throws Exception {
        mockMvc.perform(post("/api/diseases/check")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"crop":"Tomato","symptoms":"Brown spots on leaves"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nextSteps.length()").value(3))
                .andExpect(jsonPath("$.note").value(org.hamcrest.Matchers.containsString("not image analysis")));

    }

    @Test
    void currentWeatherRequiresAPlaceName() throws Exception {
        mockMvc.perform(get("/weather"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"weatherLocationForm\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("id=\"weatherResult\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("voice-weather-box"))))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("voiceWeatherForm"))));

        mockMvc.perform(get("/api/weather/current").param("location", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        mockMvc.perform(get("/api/weather/current").param("location", "a".repeat(121)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }
}
