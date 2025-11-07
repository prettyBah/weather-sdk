package me.sukhov.weather.service.client;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.sukhov.weather.dto.request.WeatherDataRequest;
import me.sukhov.weather.dto.response.*;
import me.sukhov.weather.exception.*;
import me.sukhov.weather.service.WeatherApi;
import me.sukhov.weather.service.client.config.WeatherApiConfig;
import me.sukhov.weather.service.client.open.weather.map.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Implementation of {@link WeatherApi} for working with OpenWeatherMap API via Java HTTP Client.
 * Performs HTTP requests to the API and converts responses into domain model objects.
 */
public class WeatherJavaHttpClient implements WeatherApi {

    private static final Logger log = LoggerFactory.getLogger(WeatherJavaHttpClient.class);

    private static final String DATA_WEATHER_PATH = "/data/2.5/weather";

    private final WeatherApiConfig apiConfig;
    private final HttpClient client;
    private final ObjectMapper objectMapper;

    /**
     * Creates a new client instance.
     *
     * @param config OpenWeatherMap API configuration
     * @param client HTTP client for executing requests
     * @param objectMapper mapper for JSON conversion
     */
    public WeatherJavaHttpClient(WeatherApiConfig config, HttpClient client, ObjectMapper objectMapper) {
        this.apiConfig = Objects.requireNonNull(config, "apiConfig cannot be null");
        this.client = Objects.requireNonNull(client, "client cannot be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper cannot be null");
    }

    /**
     * Creates a client with default settings.
     *
     * @param config OpenWeatherMap API configuration
     * @return new client instance
     */
    public static WeatherJavaHttpClient createDefault(WeatherApiConfig config) {
        return createDefault(config, new ObjectMapper());
    }

    /**
     * Creates a client with default settings and specified mapper.
     *
     * @param config OpenWeatherMap API configuration
     * @param objectMapper mapper for JSON conversion
     * @return new client instance
     */
    public static WeatherJavaHttpClient createDefault(WeatherApiConfig config, ObjectMapper objectMapper) {
        return new WeatherJavaHttpClient(config, HttpClient.newHttpClient(), objectMapper);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Performs HTTP GET request to OpenWeatherMap API and handles various HTTP status codes:
     * <ul>
     *   <li>200 - successful response, data is converted to {@link WeatherDataResponse}</li>
     *   <li>400 - invalid request parameters</li>
     *   <li>401 - invalid API key</li>
     *   <li>404 - city not found</li>
     *   <li>429 - request limit exceeded</li>
     *   <li>others - handled based on status code range</li>
     * </ul>
     *
     * @param request request containing the city name
     * @return weather data
     * @throws WeatherClientException for client errors (4xx)
     * @throws WeatherServerException for server errors (5xx)
     * @throws WeatherException for other errors (unexpected)
     */
    @Override
    public WeatherDataResponse getByCityName(WeatherDataRequest request) throws WeatherException {
        var params = new OpenWeatherMapQueryParams(
                request.cityName(),
                apiConfig.getApiKey(),
                apiConfig.getLang(),
                apiConfig.getUnits().toString().toLowerCase()
        );
        var uri = URI.create(apiConfig.getUrl() + DATA_WEATHER_PATH + "?" + params);
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .GET().uri(uri)
                .timeout(Duration.ofMillis(apiConfig.getRequestTimeout()))
                .build();

        HttpResponse<String> response;
        try {
            log.debug("Sending GET request: {}", httpRequest.toString().replace(apiConfig.getApiKey(), "*****"));
            response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        } catch (HttpTimeoutException hte) {
            log.error("Timeout exception: {}", hte.getMessage(), hte);
            throw new WeatherServerException(503, "Timeout exception", hte);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new WeatherException("Unexpected error", e);
        }

        var status = response.statusCode();
        return switch (status) {
            case 200 -> {
                try {
                    yield convert(objectMapper.readValue(response.body(), OpenWeatherMapResponse.class));
                } catch (JacksonException je) {
                    log.error("Json parsing error: {}", je.getMessage(), je);
                    throw new WeatherException("Json parsing error", je);
                }
            }
            case 400 -> {
                log.error("Incorrect parameters: {}", response.body());
                throw new WeatherClientException(400, "Invalid parameters");
            }
            case 401 -> {
                log.error("Incorrect API Key: {}", response.body());
                throw new WeatherApiKeyException("Incorrect API Key");
            }
            case 404 -> {
                log.error("Resource Not Found: {}", response.body());
                throw new WeatherNotFoundException(String.format("Weather info not found by %s", request.cityName()));
            }
            case 429 -> {
                log.error("To many request: {}", response.body());
                throw new WeatherToManyRequestException("Too many request");
            }
            default -> {
                log.error("Unexpected HTTP status: {}; {}", status, response.body());
                if (status >= 400 && status <= 499) {
                    throw new WeatherClientException(status, "Unexpected HTTP status");
                } else if (status >= 500 && status <= 599) {
                    throw new WeatherServerException(status, "Server error");
                } else {
                    throw new WeatherException("Unexpected HTTP status");
                }
            }
        };
    }

    /**
     * Converts OpenWeatherMap API response DTO to domain model.
     *
     * @param dto API response DTO
     * @return domain model object with weather data
     */
    private static WeatherDataResponse convert(OpenWeatherMapResponse dto) {
        var weather = Optional.ofNullable(dto.weather()).map(List::getFirst);
        var main = Optional.ofNullable(dto.main());
        var wind = Optional.ofNullable(dto.wind());
        var sys = Optional.ofNullable(dto.sys());

        return new WeatherDataResponse(
                dto.name(),
                weather.map(w -> new WeatherDto(w.main(), w.description())).orElse(null),
                main.map(m -> new TemperatureDto(m.temp(), m.feelsLike())).orElse(null),
                wind.map(w -> new WindDto(w.speed())).orElse(null),
                sys.map(s -> new SystemDto(s.sunrise(), s.sunset())).orElse(null),
                dto.visibility(),
                dto.dt(),
                dto.timezone()
        );
    }

}
