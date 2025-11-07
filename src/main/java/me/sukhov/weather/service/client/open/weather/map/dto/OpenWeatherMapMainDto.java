package me.sukhov.weather.service.client.open.weather.map.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenWeatherMapMainDto(
        @JsonProperty(value = "temp") Double temp,
        @JsonProperty(value = "feels_like") Double feelsLike
) {
}
