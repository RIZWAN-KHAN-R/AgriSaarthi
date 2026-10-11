package com.agrisaarthi.agri_web;

import java.util.Map;

import com.agrisaarthi.agri_web.CurrentWeatherService.CurrentWeatherResponse;
import com.agrisaarthi.agri_web.CurrentWeatherService.InvalidWeatherLocationException;
import com.agrisaarthi.agri_web.CurrentWeatherService.WeatherDataUnavailableException;
import com.agrisaarthi.agri_web.CurrentWeatherService.WeatherLocationNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;

@RestController
public class CurrentWeatherController {

    private static final Logger logger = LoggerFactory.getLogger(CurrentWeatherController.class);

    private final CurrentWeatherService currentWeatherService;

    public CurrentWeatherController(CurrentWeatherService currentWeatherService) {
        this.currentWeatherService = currentWeatherService;
    }

    @GetMapping("/api/weather/current")
    public ResponseEntity<?> getCurrentWeather(@RequestParam String location) {
        if (location == null || location.isBlank() || location.length() > 120) {
            return ResponseEntity.badRequest().body(Map.of("error", "Enter a place name up to 120 characters."));
        }

        try {
            CurrentWeatherResponse response = currentWeatherService.getCurrentWeather(location);
            return ResponseEntity.ok(response);
        } catch (InvalidWeatherLocationException exception) {
            return ResponseEntity.badRequest().body(Map.of("error", "Enter a place name up to 120 characters."));
        } catch (WeatherLocationNotFoundException exception) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Could not find that place in India. Try a nearby town or district name."));
        } catch (WeatherDataUnavailableException | RestClientException exception) {
            logger.warn("The current-weather provider could not complete the request.");
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("error", "Current weather is temporarily unavailable. Please try again later."));
        }
    }
}
