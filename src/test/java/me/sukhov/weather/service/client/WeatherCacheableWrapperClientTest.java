package me.sukhov.weather.service.client;

import me.sukhov.weather.dto.request.WeatherDataRequest;
import me.sukhov.weather.service.WeatherApi;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class WeatherCacheableWrapperClientTest {

    @Test
    @DisplayName("Should cache weather data and return cached value on second request")
    public void testCacheGetByCiteNameSuccess() {
        WeatherApi api = mock(WeatherApi.class);

        try (var client = new WeatherCacheableWrapperClient(api)) {
            client.getByCityName(WeatherDataRequest.builder().cityName("Zocca").build());
            client.getByCityName(WeatherDataRequest.builder().cityName("Zocca").build());
        }

        verify(api, times(1)).getByCityName(eq(WeatherDataRequest.builder().cityName("Zocca").build()));
    }

    @Test
    @DisplayName("Should evict oldest entry when cache limit is reached")
    public void testCacheGetByCiteNameWithLimitSuccess() {
        WeatherApi api = mock(WeatherApi.class);

        var config = new WeatherCacheableWrapperClient.Config(Duration.ofMinutes(1), 2);

        try (var client = new WeatherCacheableWrapperClient(api, config)) {
            client.getByCityName(WeatherDataRequest.builder().cityName("Zocca").build());
            client.getByCityName(WeatherDataRequest.builder().cityName("London").build());
            client.getByCityName(WeatherDataRequest.builder().cityName("Moscow").build());
            client.getByCityName(WeatherDataRequest.builder().cityName("Zocca").build());
        }

        verify(api, times(2)).getByCityName(eq(WeatherDataRequest.builder().cityName("Zocca").build()));
    }

    @Test
    @DisplayName("Should refresh cache entry when TTL expires")
    public void testCacheGetByCiteNameWithTtlSuccess() {
        WeatherApi api = mock(WeatherApi.class);

        var config = new WeatherCacheableWrapperClient.Config(Duration.ofMillis(10), 100);
        try (var client = new WeatherCacheableWrapperClient(api, config)) {
            client.getByCityName(WeatherDataRequest.builder().cityName("Zocca").build());
            Thread.sleep(100);
            client.getByCityName(WeatherDataRequest.builder().cityName("Zocca").build());
        } catch (InterruptedException e) {
            Assertions.fail(e);
        }

        verify(api, times(2)).getByCityName(eq(WeatherDataRequest.builder().cityName("Zocca").build()));
    }

    @Test
    @DisplayName("Should automatically update expired cache entries in background")
    public void testCacheGetByCiteNameWithRefreshUpdateSuccess() {
        WeatherApi api = mock(WeatherApi.class);

        var config = new WeatherCacheableWrapperClient.Config(
                Duration.ofMillis(50), 100, true, Duration.ofMillis(200)
        );

        try (var client = new WeatherCacheableWrapperClient(api, config)) {
            client.getByCityName(WeatherDataRequest.builder().cityName("Zocca").build());
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Assertions.fail(e);
        }

        verify(api, atLeast(2)).getByCityName(eq(WeatherDataRequest.builder().cityName("Zocca").build()));
    }

    @Test
    @DisplayName("Should automatically delete expired cache entries in background")
    public void testCacheGetByCiteNameWithRefreshDeleteSuccess() {
        WeatherApi api = mock(WeatherApi.class);

        var config = new WeatherCacheableWrapperClient.Config(
                Duration.ofMillis(50), 100, false, Duration.ofMillis(200)
        );

        try (var client = new WeatherCacheableWrapperClient(api, config)) {
            client.getByCityName(WeatherDataRequest.builder().cityName("Zocca").build());
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Assertions.fail(e);
        }

        verify(api, times(1)).getByCityName(eq(WeatherDataRequest.builder().cityName("Zocca").build()));
    }

}
