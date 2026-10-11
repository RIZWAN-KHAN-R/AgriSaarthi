package com.Hack.AgriSaarthi.model;

public record WeatherInfo(
    Double temperatureCelsius,
    Integer relativeHumidityPercent,
    Double precipitationMm,
    Double windSpeedKmh,
    String time,
    String status
) {}