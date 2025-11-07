package me.sukhov.weather.exception;

public class WeatherApiKeyException extends WeatherClientException{
    public WeatherApiKeyException(String message) {
        super(401, message);
    }
}
