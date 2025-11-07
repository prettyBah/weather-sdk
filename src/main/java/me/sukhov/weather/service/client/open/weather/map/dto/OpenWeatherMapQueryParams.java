package me.sukhov.weather.service.client.open.weather.map.dto;

import java.util.Map;

import static me.sukhov.weather.service.client.open.weather.map.OpenWeatherMapUtils.queryParams;

public record OpenWeatherMapQueryParams(
        String cityName,
        String appid,
        String lang,
        String units
) {

    @Override
    public String toString() {
        return queryParams(
                Map.of(
                        "q", cityName(),
                        "appid", appid(),
                        "lang", lang(),
                        "units", units()
                )
        );
    }

}
