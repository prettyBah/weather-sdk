package me.sukhov.weather.service.client.config;

import java.util.Objects;

public class WeatherApiConfig {

    private final String url;
    private final String lang;
    private final String apiKey;
    private final WeatherUnits units;

    private final long requestTimeout;

    public WeatherApiConfig(String url, String apiKey, String lang, WeatherUnits units, long requestTimeout) {
        this.url = url;
        this.apiKey = apiKey;
        this.lang = lang;
        this.units = units;
        this.requestTimeout = requestTimeout;
    }

    public String getUrl() {
        return url;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getLang() {
        return lang;
    }

    public WeatherUnits getUnits() {
        return units;
    }

    public long getRequestTimeout() {
        return requestTimeout;
    }

    public enum WeatherUnits {
        METRIC, STANDARD, IMPERIAL
    }

    public static WeatherApiConfigBuilder builder() {
        return new WeatherApiConfigBuilder();
    }

    public static class WeatherApiConfigBuilder {

        private String url;
        private String apiKey;
        private String lang = "en";
        private WeatherUnits units = WeatherUnits.STANDARD;

        private long requestTimeout = 10 * 1000;

        public WeatherApiConfigBuilder url(String url) {
            this.url = Objects.requireNonNull(url, "url cannot be null");
            return this;
        }

        public WeatherApiConfigBuilder apiKey(String apiKey) {
            this.apiKey = Objects.requireNonNull(apiKey, "apiKey cannot be null");
            return this;
        }

        public WeatherApiConfigBuilder lang(String lang) {
            this.lang = lang;
            return this;
        }

        public WeatherApiConfigBuilder units(WeatherUnits units) {
            this.units = units;
            return this;
        }

        public WeatherApiConfigBuilder requestTimeout(long requestTimeout) {
            this.requestTimeout = requestTimeout;
            return this;
        }

        public WeatherApiConfig build() {
            Objects.requireNonNull(apiKey, "apiKey cannot be null");
            Objects.requireNonNull(url, "url cannot be null");
            return new WeatherApiConfig(url, apiKey, lang, units, requestTimeout);
        }

    }

}
