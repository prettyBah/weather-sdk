package me.sukhov.weather.service.client;

import me.sukhov.weather.dto.request.WeatherDataRequest;
import me.sukhov.weather.exception.*;
import me.sukhov.weather.service.client.config.WeatherApiConfig;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static me.sukhov.weather.service.client.WeatherClientTestExamples.*;
import static me.sukhov.weather.service.client.WeatherClientTestExamples.EXAMPLE_SERVER_ERROR;
import static me.sukhov.weather.service.client.WeatherClientTestExamples.EXAMPLE_TOO_MANY_REQUESTS;
import static me.sukhov.weather.service.client.WeatherClientTestExamples.EXAMPLE_UNAUTHORIZED_API_KEY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class WeatherJavaHttpClientTest {

    @Test
    @DisplayName("Should successfully retrieve weather data by city name")
    public void testGetByCityNameSuccess() {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(
                    new MockResponse()
                            .setBody(EXAMPLE_SUCCESS)
                            .setHeader("Content-Type", "application/json")
                            .setResponseCode(200)
            );
            server.start();

            var client = WeatherJavaHttpClient.createDefault(
                    WeatherApiConfig.builder()
                            .url(server.url("/test").toString())
                            .apiKey("test")
                            .build()
            );

            String cityName = "Zocca";
            var result = client.getByCityName(
                    WeatherDataRequest.builder().cityName(cityName).build()
            );

            assertAll(
                    (() -> assertEquals(cityName, result.name())),
                    (() -> assertEquals("Rain", result.weather().main())),
                    (() -> assertEquals("moderate rain", result.weather().description())),
                    (() -> assertEquals(298.48, result.temperature().temp())),
                    (() -> assertEquals(298.74, result.temperature().feelsLike())),
                    (() -> assertEquals(0.62, result.wind().speed())),
                    (() -> assertEquals(1661834187, result.sys().sunrise())),
                    (() -> assertEquals(1661882248, result.sys().sunset())),
                    (() -> assertEquals(10000, result.visibility())),
                    (() -> assertEquals(1661870592, result.datetime())),
                    (() -> assertEquals(7200, result.timezone()))
            );

            RecordedRequest recordedRequest = server.takeRequest();
            assertRequest(recordedRequest, cityName);
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    @DisplayName("Should throw WeatherNotFoundException when city is not found (404)")
    public void testGetByCityNameNotFound() {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(
                    new MockResponse()
                            .setBody(EXAMPLE_NOT_FOUND)
                            .setHeader("Content-Type", "application/json")
                            .setResponseCode(404)
            );
            server.start();

            var client = WeatherJavaHttpClient.createDefault(
                    WeatherApiConfig.builder()
                            .url(server.url("/test").toString())
                            .apiKey("test")
                            .build()
            );

            String cityName = "L";
            var exc = assertThrows(
                    WeatherNotFoundException.class,
                    () -> client.getByCityName(
                            WeatherDataRequest.builder().cityName(cityName).build()
                    )
            );

            assertEquals("Weather info not found by L", exc.getMessage());

            RecordedRequest recordedRequest = server.takeRequest();
            assertRequest(recordedRequest, cityName);
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    @DisplayName("Should throw WeatherClientException when request parameters are invalid (400)")
    public void testGetByCityNameInvalidParameter() {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(
                    new MockResponse()
                            .setBody(EXAMPLE_INVALID_PARAMETER)
                            .setHeader("Content-Type", "application/json")
                            .setResponseCode(400)
            );
            server.start();

            var client = WeatherJavaHttpClient.createDefault(
                    WeatherApiConfig.builder()
                            .url(server.url("/test").toString())
                            .apiKey("test")
                            .lang("10220")
                            .units(WeatherApiConfig.WeatherUnits.IMPERIAL)
                            .build()
            );

            String cityName = "Zocca";
            var exc = assertThrows(
                    WeatherClientException.class,
                    () -> client.getByCityName(
                            WeatherDataRequest.builder().cityName(cityName).build()
                    )
            );

            assertEquals("Invalid parameters", exc.getMessage());
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    @DisplayName("Should throw WeatherApiKeyException when API key is invalid (401)")
    public void testGetByCityApiKeyUnauthorized() {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(
                    new MockResponse()
                            .setBody(EXAMPLE_UNAUTHORIZED_API_KEY)
                            .setHeader("Content-Type", "application/json")
                            .setResponseCode(401)
            );
            server.start();

            var client = WeatherJavaHttpClient.createDefault(
                    WeatherApiConfig.builder()
                            .url(server.url("/test").toString())
                            .apiKey("test")
                            .build()
            );

            String cityName = "Zocca";
            var exc = assertThrows(
                    WeatherApiKeyException.class,
                    () -> client.getByCityName(
                            WeatherDataRequest.builder().cityName(cityName).build()
                    )
            );

            assertEquals("Incorrect API Key", exc.getMessage());

            RecordedRequest recordedRequest = server.takeRequest();
            assertRequest(recordedRequest, cityName);
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    @DisplayName("Should throw WeatherToManyRequestException when request limit is exceeded (429)")
    public void testGetByCityToManyRequest() {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(
                    new MockResponse()
                            .setBody(EXAMPLE_TOO_MANY_REQUESTS)
                            .setHeader("Content-Type", "application/json")
                            .setResponseCode(429)
            );
            server.start();

            var client = WeatherJavaHttpClient.createDefault(
                    WeatherApiConfig.builder()
                            .url(server.url("/test").toString())
                            .apiKey("test")
                            .build()
            );

            String cityName = "Zocca";
            var exc = assertThrows(
                    WeatherToManyRequestException.class,
                    () -> client.getByCityName(
                            WeatherDataRequest.builder().cityName(cityName).build()
                    )
            );

            assertEquals("Too many request", exc.getMessage());

            RecordedRequest recordedRequest = server.takeRequest();
            assertRequest(recordedRequest, cityName);
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    @DisplayName("Should throw WeatherServerException when server returns error (500)")
    public void testGetByCityServerError() {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(
                    new MockResponse()
                            .setBody(EXAMPLE_SERVER_ERROR)
                            .setHeader("Content-Type", "application/json")
                            .setResponseCode(500)
            );
            server.start();

            var client = WeatherJavaHttpClient.createDefault(
                    WeatherApiConfig.builder()
                            .url(server.url("/test").toString())
                            .apiKey("test")
                            .build()
            );

            String cityName = "Zocca";
            var exc = assertThrows(
                    WeatherServerException.class,
                    () -> client.getByCityName(
                            WeatherDataRequest.builder().cityName(cityName).build()
                    )
            );

            assertEquals("Server error", exc.getMessage());

            RecordedRequest recordedRequest = server.takeRequest();
            assertRequest(recordedRequest, cityName);
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    @DisplayName("Should throw WeatherClientException for unexpected client error status codes (4xx)")
    public void testGetByCityUnexpectedClientError() {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(
                    new MockResponse()
                            .setBody("{\"message\":\"Forbidden\"}")
                            .setHeader("Content-Type", "application/json")
                            .setResponseCode(403)
            );
            server.start();

            var client = WeatherJavaHttpClient.createDefault(
                    WeatherApiConfig.builder()
                            .url(server.url("/test").toString())
                            .apiKey("test")
                            .build()
            );

            String cityName = "Zocca";
            var exc = assertThrows(
                    WeatherClientException.class,
                    () -> client.getByCityName(
                            WeatherDataRequest.builder().cityName(cityName).build()
                    )
            );

            assertEquals("Unexpected HTTP status", exc.getMessage());
            assertEquals(403, exc.getStatusCode());

            RecordedRequest recordedRequest = server.takeRequest();
            assertRequest(recordedRequest, cityName);
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    @DisplayName("Should throw WeatherServerException when request timeout occurs")
    public void testGetByCityTimeout() {
        try (MockWebServer server = new MockWebServer()) {
            server.start();

            var client = WeatherJavaHttpClient.createDefault(
                    WeatherApiConfig.builder()
                            .url(server.url("/test").toString())
                            .apiKey("test")
                            .requestTimeout(20)
                            .build()
            );

            String cityName = "Zocca";
            var exc = assertThrows(
                    WeatherServerException.class,
                    () -> client.getByCityName(
                            WeatherDataRequest.builder().cityName(cityName).build()
                    )
            );

            assertEquals("Timeout exception", exc.getMessage());
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    @DisplayName("Should throw WeatherException when response JSON cannot be parsed")
    public void testGetByCityUnexpectedResponse() {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(
                    new MockResponse()
                            .setResponseCode(200)
                            .setHeader("Content-Type", "application/json")
            );
            server.start();

            var client = WeatherJavaHttpClient.createDefault(
                    WeatherApiConfig.builder()
                            .url(server.url("/test").toString())
                            .apiKey("test")
                            .build()
            );

            String cityName = "Zocca";
            var exc = assertThrows(
                    WeatherException.class,
                    () -> client.getByCityName(
                            WeatherDataRequest.builder().cityName(cityName).build()
                    )
            );

            assertEquals("Json parsing error", exc.getMessage());

            RecordedRequest recordedRequest = server.takeRequest();
            assertRequest(recordedRequest, cityName);
        } catch (Exception e) {
            fail(e);
        }
    }

    private static void assertRequest(RecordedRequest recordedRequest, String name) {
        assertAll(
                (() -> assertEquals("GET", recordedRequest.getMethod())),
                (() -> assertThat(recordedRequest.getPath(), CoreMatchers.startsWith("/test/data/2.5/weather?"))),
                (() -> assertThat(recordedRequest.getPath(), CoreMatchers.containsString("q=" + name))),
                (() -> assertThat(recordedRequest.getPath(), CoreMatchers.containsString("appid=test"))),
                (() -> assertThat(recordedRequest.getPath(), CoreMatchers.containsString("lang=en"))),
                (() -> assertThat(recordedRequest.getPath(), CoreMatchers.containsString("units=standard")))
        );
    }

}
