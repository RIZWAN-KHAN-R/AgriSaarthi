package com.Hack.AgriSaarthi.service;

import com.Hack.AgriSaarthi.model.WeatherInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Service
public class WeatherService {

    private final RestClient restClient;
    private final String weatherApiUrl;

    public WeatherService(
            @Value("${weather.api.url}") String weatherApiUrl) {

        this.restClient = RestClient.create();
        this.weatherApiUrl = weatherApiUrl;
    }

    public WeatherInfo getCurrentWeather(
            double latitude, double longitude) {

        try {
            String uri = UriComponentsBuilder
                    .fromUriString(weatherApiUrl)
                    .queryParam("latitude", latitude)
                    .queryParam("longitude", longitude)
                    .queryParam("current",
                            "temperature_2m,relative_humidity_2m,precipitation,wind_speed_10m")
                    .queryParam("timezone", "auto")
                    .build()
                    .encode()
                    .toUriString();

            Map<?, ?> response = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .body(Map.class);

            if (response == null ||
                    !(response.get("current") instanceof Map<?, ?> current)) {
                return unavailable();
            }

            return new WeatherInfo(
                    number(current.get("temperature_2m")),
                    integer(current.get("relative_humidity_2m")),
                    number(current.get("precipitation")),
                    number(current.get("wind_speed_10m")),
                    string(current.get("time")),
                    "AVAILABLE"
            );

        } catch (Exception exception) {
            return unavailable();
        }
    }

    private WeatherInfo unavailable() {
        return new WeatherInfo(
                null, null, null, null, null, "UNAVAILABLE"
        );
    }

    private Double number(Object value) {
        return value instanceof Number n ? n.doubleValue() : null;
    }

    private Integer integer(Object value) {
        return value instanceof Number n ? n.intValue() : null;
    }

    private String string(Object value) {
        return value == null ? null : value.toString();
    }
}