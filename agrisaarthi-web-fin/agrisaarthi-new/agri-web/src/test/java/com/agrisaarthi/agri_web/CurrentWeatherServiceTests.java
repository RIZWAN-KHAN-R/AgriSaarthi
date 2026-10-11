package com.agrisaarthi.agri_web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class CurrentWeatherServiceTests {

    @Test
    void resolvesPlaceFetchesCurrentWeatherAndBuildsRainChecklist() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        server.expect(requestTo(org.hamcrest.Matchers.containsString("geocoding-api.open-meteo.com/v1/search")))
                .andExpect(request -> assertTrue(request.getURI().getRawQuery().contains("name=Coimbatore")))
                .andRespond(withSuccess("""
                        {"results":[{"name":"Coimbatore","admin1":"Tamil Nadu","country":"India","latitude":11.0168,"longitude":76.9558}]}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(org.hamcrest.Matchers.containsString("api.open-meteo.com/v1/forecast")))
                .andExpect(request -> {
                    String query = request.getURI().getRawQuery();
                    assertTrue(query.contains("latitude=11.0168"));
                    assertTrue(query.contains("longitude=76.9558"));
                    assertTrue(query.contains("weather_code"));
                })
                .andRespond(withSuccess("""
                        {"timezone":"Asia/Kolkata","current":{"time":"2026-10-10T23:00","temperature_2m":26.4,"apparent_temperature":29.1,"relative_humidity_2m":82,"precipitation":1.2,"weather_code":61,"wind_speed_10m":12.4}}
                        """, MediaType.APPLICATION_JSON));

        CurrentWeatherService service = new CurrentWeatherService(restClientBuilder.build());
        CurrentWeatherService.CurrentWeatherResponse response = service.getCurrentWeather("Coimbatore");

        assertEquals("Coimbatore, Tamil Nadu, India", response.location());
        assertEquals("Rain", response.weatherDescription());
        assertEquals(26.4, response.temperatureC());
        assertTrue(response.observationTime().contains("Asia/Kolkata"));
        assertTrue(response.advisory().contains("pause irrigation"));
        assertFalse(response.advisory().contains("relatively settled"));
        assertEquals("Open-Meteo", response.source());
        server.verify();
    }

    @Test
    void addsHeatAndWindSafetyStepsToChecklist() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        server.expect(requestTo(org.hamcrest.Matchers.containsString("geocoding-api.open-meteo.com/v1/search")))
                .andRespond(withSuccess("""
                        {"results":[{"name":"Madurai","admin1":"Tamil Nadu","country":"India","latitude":9.9252,"longitude":78.1198}]}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(org.hamcrest.Matchers.containsString("api.open-meteo.com/v1/forecast")))
                .andRespond(withSuccess("""
                        {"timezone":"Asia/Kolkata","current":{"time":"2026-10-10T14:00","temperature_2m":38.0,"apparent_temperature":42.0,"relative_humidity_2m":32,"precipitation":0.0,"weather_code":0,"wind_speed_10m":34.0}}
                        """, MediaType.APPLICATION_JSON));

        CurrentWeatherService service = new CurrentWeatherService(restClientBuilder.build());
        CurrentWeatherService.CurrentWeatherResponse response = service.getCurrentWeather("Madurai");

        assertTrue(response.advisory().contains("cooler hours"));
        assertTrue(response.advisory().contains("postpone pesticide spraying"));
        server.verify();
    }

    @Test
    void geocodesTamilPlaceNamesUsingTamilLanguage() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        server.expect(requestTo(org.hamcrest.Matchers.containsString("geocoding-api.open-meteo.com/v1/search")))
                .andExpect(request -> {
                    String query = URLDecoder.decode(request.getURI().getRawQuery(), StandardCharsets.UTF_8);
                    assertTrue(query.contains("name=கோயம்புத்தூர்"));
                    assertTrue(query.contains("language=ta"));
                })
                .andRespond(withSuccess("""
                        {"results":[{"name":"கோயம்புத்தூர்","admin1":"தமிழ்நாடு","country":"இந்தியா","latitude":11.00555,"longitude":76.96612}]}
                        """, MediaType.APPLICATION_JSON));
        server.expect(requestTo(org.hamcrest.Matchers.containsString("api.open-meteo.com/v1/forecast")))
                .andRespond(withSuccess("""
                        {"timezone":"Asia/Kolkata","current":{"time":"2026-10-10T23:00","temperature_2m":26.0,"apparent_temperature":28.0,"relative_humidity_2m":75,"precipitation":0.0,"weather_code":1,"wind_speed_10m":8.0}}
                        """, MediaType.APPLICATION_JSON));

        CurrentWeatherService service = new CurrentWeatherService(restClientBuilder.build());
        CurrentWeatherService.CurrentWeatherResponse response = service.getCurrentWeather("கோயம்புத்தூர்");

        assertEquals("கோயம்புத்தூர், தமிழ்நாடு, இந்தியா", response.location());
        assertEquals("Mainly clear", response.weatherDescription());
        server.verify();
    }
}
