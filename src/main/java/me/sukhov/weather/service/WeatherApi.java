package me.sukhov.weather.service;

import me.sukhov.weather.dto.request.WeatherDataRequest;
import me.sukhov.weather.dto.response.WeatherDataResponse;
import me.sukhov.weather.exception.WeatherException;

/**
 * Interface for retrieving weather data.
 */
public interface WeatherApi {

    /**
     * Retrieves weather data for the specified city.
     *
     * @param request request containing the city name
     * @return weather data
     * @throws WeatherException if an error occurred while retrieving data
     */
    WeatherDataResponse getByCityName(WeatherDataRequest request) throws WeatherException;

}
