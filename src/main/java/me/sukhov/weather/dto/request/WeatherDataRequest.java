package me.sukhov.weather.dto.request;

public record WeatherDataRequest(String cityName) {

    public static WeatherDataRequestBuilder builder() {
        return new WeatherDataRequestBuilder();
    }

    public static class WeatherDataRequestBuilder {
        private String cityName;

        public WeatherDataRequestBuilder cityName(String cityName) {
            this.cityName = cityName;
            return this;
        }

        public WeatherDataRequest build() {
            return new WeatherDataRequest(cityName);
        }
    }

}
