package me.sukhov.weather.service.client.open.weather.map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class OpenWeatherMapUtilsTest {

    private static Stream<Arguments> maps() {
        return Stream.of(
                Arguments.of(
                        Map.of("key", "value"),
                        "key=value"),

                Arguments.of(
                        Map.of("key", "value", "key2", "value2"),
                        "key=value&key2=value2"),

                Arguments.of(
                        Map.of("q", "London",
                                "appid", "test",
                                "lang", "en",
                                "units", "metric"),
                        "q=London&units=metric&lang=en&appid=test")
        );
    }

    @ParameterizedTest
    @MethodSource("maps")
    public void testQueryParamsSuccess(Map<String, String> map, String expected) {
        var result = OpenWeatherMapUtils.queryParams(map);
        for (String s : result.split("&")) {
            assertThat(expected, containsString(s));
        }
    }

    @Test
    public void testQueryParamsIsNull() {
        var result = assertThrows(
                NullPointerException.class,
                () -> OpenWeatherMapUtils.queryParams(null)
        );
        assertEquals("Params must not be null", result.getMessage());
    }

    @Test
    public void testQueryParamsIsEmpty() {
        var result = assertThrows(
                IllegalArgumentException.class,
                () -> OpenWeatherMapUtils.queryParams(Collections.emptyMap())
        );
        assertEquals("Params must not be empty", result.getMessage());
    }
}
