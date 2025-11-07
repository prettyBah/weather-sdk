package me.sukhov.weather.exception;

public class WeatherClientException extends WeatherException {
    private final int statusCode;

    public WeatherClientException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
