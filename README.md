# OpenWeatherMap Client SDK

A Java SDK for retrieving weather data from the OpenWeatherMap API with built-in caching support.

## Requirements

- Java 17 or higher

## Installation

### Gradle

Add the following to your `build.gradle`:

```gradle
dependencies {
    implementation 'me.sukhov.weather:open-weather-map-client:1.0.0'
}
```

### Maven

```xml
<dependency>
    <groupId>me.sukhov.weather</groupId>
    <artifactId>open-weather-map-client</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Quick Start

### Basic Usage

```java
import me.sukhov.weather.dto.request.WeatherDataRequest;
import me.sukhov.weather.service.client.WeatherJavaHttpClient;
import me.sukhov.weather.service.client.config.WeatherApiConfig;

// Create API configuration
var config = WeatherApiConfig.builder()
    .url("https://api.openweathermap.org")
    .apiKey("your-api-key-here")
    .build();

// Create client
var client = WeatherJavaHttpClient.createDefault(config);

// Request weather data
var request = WeatherDataRequest.builder()
    .cityName("London")
    .build();

var response = client.getByCityName(request);
```

### With Caching

```java
import me.sukhov.weather.service.client.WeatherCacheableWrapperClient;
import java.time.Duration;

// Create base client
var baseClient = WeatherJavaHttpClient.createDefault(config);

// Configure cache: TTL = 10 minutes, max 50 entries
var cacheConfig = new WeatherCacheableWrapperClient.Config(
    Duration.ofMinutes(10), 
    50
);

// Create cached client
try (var cachedClient = new WeatherCacheableWrapperClient(baseClient, cacheConfig)) {
    var request = WeatherDataRequest.builder()
        .cityName("Moscow")
        .build();
    
    // First call - fetches from API
    var response1 = cachedClient.getByCityName(request);
    
    // Second call - returns cached data (if within TTL)
    var response2 = cachedClient.getByCityName(request);
}
```

## Configuration

### API Configuration

The `WeatherApiConfig` allows you to customize API connection settings:

```java
var config = WeatherApiConfig.builder()
    .url("https://api.openweathermap.org")           // API base URL (required)
    .apiKey("your-api-key")                          // API key (required)
    .lang("en")                                      // Response language (default: "en")
    .units(WeatherApiConfig.WeatherUnits.METRIC)     // Temperature units (default: STANDARD)
    .requestTimeout(10000)                           // Request timeout in milliseconds (default: 10000)
    .build();
```

### Cache Configuration

The `WeatherCacheableWrapperClient.Config` provides flexible caching options:

```java
// Default configuration (TTL: 10 min, limit: 10 entries, refresh: disabled)
var defaultConfig = new WeatherCacheableWrapperClient.Config();

// Custom TTL and limit
var customConfig = new WeatherCacheableWrapperClient.Config(
    Duration.ofMinutes(15),  // Cache entry lifetime
    100                      // Maximum cache entries
);

// With background refresh
var refreshConfig = new WeatherCacheableWrapperClient.Config(
    Duration.ofMinutes(10),     // TTL
    50,                          // Limit
    true,                        // Enable refresh
    Duration.ofMinutes(5)        // Refresh interval
);
```

**Cache Features:**
- **TTL (Time To Live)**: How long cached entries remain valid
- **Limit**: Maximum number of entries in cache (LRU eviction when limit reached)
- **Background Refresh**: Automatically refreshes expired entries in the background

## Examples

### Example 1: Simple Weather Check

```java
var config = OpenWeatherMapApiConfig.builder()
    .url("https://api.openweathermap.org")
    .apiKey("your-api-key")
    .units(OpenWeatherMapApiConfig.WeatherUnits.METRIC)
    .build();

var client = WeatherJavaHttpClient.createDefault(config);
var request = WeatherDataRequest.builder().cityName("Paris").build();

var weather = client.getByCityName(request);
System.out.println(weather.name() + ": " + 
                   weather.temperature().temp() + "°C");
```

### Example 2: Multiple Cities with Caching

```java
var baseClient = WeatherJavaHttpClient.createDefault(config);
var cacheConfig = new WeatherCacheableWrapperClient.Config(
    Duration.ofMinutes(10), 
    20
);

try (var cachedClient = new WeatherCacheableWrapperClient(baseClient, cacheConfig)) {
    String[] cities = {"London", "New York", "Tokyo", "Moscow"};
    
    for (String city : cities) {
        var request = WeatherDataRequest.builder().cityName(city).build();
        var response = cachedClient.getByCityName(request);
        System.out.println(city + ": " + response.temperature().temp() + "°C");
    }
    
    // Second iteration uses cache
    for (String city : cities) {
        var request = WeatherDataRequest.builder().cityName(city).build();
        var response = cachedClient.getByCityName(request); // From cache
        System.out.println(city + ": " + response.temperature().temp() + "°C");
    }
}
```

### Example 3: With Background Refresh

```java
var cacheConfig = new WeatherCacheableWrapperClient.Config(
    Duration.ofMinutes(10),  // Cache for 10 minutes
    50,                       // Max 50 entries
    true,                     // Enable refresh
    Duration.ofMinutes(5)      // Refresh every 5 minutes
);

try (var cachedClient = new WeatherCacheableWrapperClient(baseClient, cacheConfig)) {
    // Cache will automatically refresh expired entries in the background
    var request = WeatherDataRequest.builder().cityName("Berlin").build();
    var response = cachedClient.getByCityName(request);
}
```

