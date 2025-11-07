package me.sukhov.weather.exception;

public class WeatherServerException extends WeatherException {
    private final int statusCode;

    public WeatherServerException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public WeatherServerException(int statusCode, String message, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
    
}
