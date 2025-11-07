package me.sukhov.weather.dto.response;

public record WeatherDataResponse(
        String name,
        WeatherDto weather,
        TemperatureDto temperature,
        WindDto wind,
        SystemDto sys,
        Long visibility,
        Long datetime,
        Long timezone
) {
}