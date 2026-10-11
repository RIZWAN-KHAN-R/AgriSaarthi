package com.agrisaarthi.agri_web;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class CurrentWeatherService {

    private static final String GEOCODING_URL = "https://geocoding-api.open-meteo.com/v1/search";
    private static final String FORECAST_URL = "https://api.open-meteo.com/v1/forecast";
    private static final String SOURCE = "Open-Meteo";

    private final RestClient restClient;

    public CurrentWeatherService() {
        this(createRestClient());
    }

    CurrentWeatherService(RestClient restClient) {
        this.restClient = restClient;
    }

    private static RestClient createRestClient() {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        return RestClient.builder().requestFactory(requestFactory).build();
    }

    public CurrentWeatherResponse getCurrentWeather(String place) {
        String query = place == null ? "" : place.trim();
        if (query.isBlank() || query.length() > 120) {
            throw new InvalidWeatherLocationException();
        }

        String language = containsTamilCharacters(query) ? "ta" : "en";
        GeocodingResponse geocoding = restClient.get()
                .uri(GEOCODING_URL + "?name={name}&count=1&language={language}&format=json&countryCode=IN", query, language)
                .retrieve()
                .body(GeocodingResponse.class);
        if (geocoding == null || geocoding.results() == null || geocoding.results().isEmpty()) {
            throw new WeatherLocationNotFoundException(query);
        }

        PlaceResult resolved = geocoding.results().get(0);
        ForecastResponse forecast = restClient.get()
                .uri(FORECAST_URL
                        + "?latitude={latitude}&longitude={longitude}"
                        + "&current=temperature_2m,apparent_temperature,relative_humidity_2m,precipitation,weather_code,wind_speed_10m"
                        + "&timezone=auto", resolved.latitude(), resolved.longitude())
                .retrieve()
                .body(ForecastResponse.class);
        if (forecast == null || forecast.current() == null) {
            throw new WeatherDataUnavailableException();
        }

        CurrentConditions current = forecast.current();
        String location = formatLocation(resolved);
        return new CurrentWeatherResponse(
                location,
                describeWeatherCode(current.weatherCode()),
                current.temperatureC(),
                current.apparentTemperatureC(),
                current.relativeHumidityPercent(),
                current.precipitationMm(),
                current.windSpeedKmh(),
                formatObservationTime(current.observationTime(), forecast.timezone()),
                createChecklist(current),
                "These are current conditions, not a forecast. Check official local alerts before safety-critical work.",
                SOURCE);
    }

    private String formatLocation(PlaceResult place) {
        List<String> parts = new ArrayList<>();
        parts.add(place.name());
        if (place.admin1() != null && !place.admin1().isBlank() && !place.admin1().equalsIgnoreCase(place.name())) {
            parts.add(place.admin1());
        }
        if (place.country() != null && !place.country().isBlank()) {
            parts.add(place.country());
        }
        return String.join(", ", parts);
    }

    private boolean containsTamilCharacters(String value) {
        return value.codePoints()
                .anyMatch(codePoint -> Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.TAMIL);
    }

    private String formatObservationTime(String observationTime, String timezone) {
        if (timezone == null || timezone.isBlank()) {
            return observationTime;
        }
        return observationTime + " (" + timezone + ")";
    }

    private String createChecklist(CurrentConditions current) {
        List<String> steps = new ArrayList<>();
        int weatherCode = current.weatherCode() == null ? -1 : current.weatherCode();
        boolean wetConditions = isWetWeather(weatherCode)
                || (current.precipitationMm() != null && current.precipitationMm() >= 0.5);
        boolean lightPrecipitation = !wetConditions
                && current.precipitationMm() != null
                && current.precipitationMm() > 0;
        boolean highHeat = current.temperatureC() != null && current.temperatureC() >= 35;
        boolean cold = current.temperatureC() != null && current.temperatureC() <= 5;
        boolean strongWind = current.windSpeedKmh() != null && current.windSpeedKmh() >= 30;
        boolean thunderstorm = weatherCode >= 95;

        if (wetConditions) {
            steps.add("Rain or wet conditions are being observed: pause irrigation, keep field drains clear, and avoid machinery or spraying on wet crops.");
        } else if (lightPrecipitation) {
            steps.add("Light precipitation is being observed: check root-zone moisture before irrigating and avoid spraying until foliage is dry.");
        } else {
            steps.add("No precipitation is being observed now: check root-zone moisture and crop stage before deciding whether to irrigate.");
        }

        if (highHeat) {
            steps.add("High heat is being observed: schedule strenuous field work for cooler hours, provide drinking water, and monitor crops for heat stress.");
        } else if (cold) {
            steps.add("Cold conditions are being observed: protect sensitive seedlings and check local crop-specific cold guidance.");
        }

        if (strongWind) {
            steps.add("Strong wind is being observed: postpone pesticide spraying and secure lightweight materials in the field.");
        }
        if (thunderstorm) {
            steps.add("Thunderstorm conditions are being observed: stop exposed field work and follow official local safety alerts.");
        }
        if (!wetConditions && !lightPrecipitation && !highHeat && !cold && !strongWind && !thunderstorm) {
            steps.add("Conditions appear relatively settled at this observation time; keep monitoring local changes and follow any official weather warning.");
        }
        return String.join(" ", steps);
    }

    private boolean isWetWeather(int code) {
        return (code >= 51 && code <= 67)
                || (code >= 80 && code <= 82)
                || code == 95
                || code == 96
                || code == 99;
    }

    private String describeWeatherCode(Integer code) {
        if (code == null) {
            return "Current weather";
        }
        return switch (code) {
            case 0 -> "Clear sky";
            case 1 -> "Mainly clear";
            case 2 -> "Partly cloudy";
            case 3 -> "Overcast";
            case 45, 48 -> "Fog";
            case 51, 53, 55 -> "Drizzle";
            case 56, 57 -> "Freezing drizzle";
            case 61, 63, 65 -> "Rain";
            case 66, 67 -> "Freezing rain";
            case 71, 73, 75, 77 -> "Snow";
            case 80, 81, 82 -> "Rain showers";
            case 85, 86 -> "Snow showers";
            case 95 -> "Thunderstorm";
            case 96, 99 -> "Thunderstorm with hail";
            default -> String.format(Locale.ROOT, "Weather code %d", code);
        };
    }

    public record CurrentWeatherResponse(
            String location,
            String weatherDescription,
            Double temperatureC,
            Double apparentTemperatureC,
            Integer relativeHumidityPercent,
            Double precipitationMm,
            Double windSpeedKmh,
            String observationTime,
            String advisory,
            String note,
            String source) {
    }

    private record GeocodingResponse(List<PlaceResult> results) {
    }

    private record PlaceResult(String name, String admin1, String country, Double latitude, Double longitude) {
    }

    private record ForecastResponse(String timezone, CurrentConditions current) {
    }

    private record CurrentConditions(
            @JsonProperty("time") String observationTime,
            @JsonProperty("temperature_2m") Double temperatureC,
            @JsonProperty("apparent_temperature") Double apparentTemperatureC,
            @JsonProperty("relative_humidity_2m") Integer relativeHumidityPercent,
            @JsonProperty("precipitation") Double precipitationMm,
            @JsonProperty("weather_code") Integer weatherCode,
            @JsonProperty("wind_speed_10m") Double windSpeedKmh) {
    }

    public static class InvalidWeatherLocationException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    public static class WeatherLocationNotFoundException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public WeatherLocationNotFoundException(String query) {
            super(query);
        }
    }

    public static class WeatherDataUnavailableException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
