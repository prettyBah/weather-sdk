package me.sukhov.weather.service.client.open.weather.map;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class OpenWeatherMapUtils {

    public static String queryParams(Map<String, String> params) {
        Objects.requireNonNull(params, "Params must not be null");
        if (params.isEmpty()) {
            throw new IllegalArgumentException("Params must not be empty");
        }
        return params.entrySet().stream()
                .map(e -> encode(e.getKey()) + "=" + encode(e.getValue()))
                .collect(Collectors.joining("&"));
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

}
