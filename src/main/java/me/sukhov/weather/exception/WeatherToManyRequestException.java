package me.sukhov.weather.exception;

public class WeatherToManyRequestException extends WeatherClientException {
    public WeatherToManyRequestException(String message) {
        super(429, message);
    }
}
