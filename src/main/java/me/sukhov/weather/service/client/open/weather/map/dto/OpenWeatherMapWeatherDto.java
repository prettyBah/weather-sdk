package me.sukhov.weather.service.client.open.weather.map.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenWeatherMapWeatherDto(
        @JsonProperty(value = "main") String main,
        @JsonProperty(value = "description") String description
) {
}
