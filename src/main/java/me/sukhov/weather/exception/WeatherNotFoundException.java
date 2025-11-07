package me.sukhov.weather.exception;

public class WeatherNotFoundException extends WeatherClientException {
    public WeatherNotFoundException(String message) {
        super(404, message);
    }
}
