package me.sukhov.weather.service.client.open.weather.map.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenWeatherMapResponse(
        @JsonProperty(value = "name") String name,
        @JsonProperty(value = "weather") List<OpenWeatherMapWeatherDto> weather,
        @JsonProperty(value = "main") OpenWeatherMapMainDto main,
        @JsonProperty(value = "wind") OpenWeatherMapWindDto wind,
        @JsonProperty(value = "sys") OpenWeatherMapSysDto sys,
        @JsonProperty(value = "visibility") Long visibility,
        @JsonProperty(value = "dt") Long dt,
        @JsonProperty(value = "timezone") Long timezone
) {
}