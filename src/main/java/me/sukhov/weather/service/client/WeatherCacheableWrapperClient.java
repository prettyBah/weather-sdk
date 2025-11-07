package me.sukhov.weather.service.client;

import me.sukhov.weather.dto.request.WeatherDataRequest;
import me.sukhov.weather.dto.response.WeatherDataResponse;
import me.sukhov.weather.exception.WeatherException;
import me.sukhov.weather.service.WeatherApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Decorator for {@link WeatherApi} that adds weather data caching.
 * <p>
 * Caches request results for a specified time (TTL) and automatically removes expired entries.
 * Supports cache size limits and optional background refresh mechanism.
 * <p>
 * Implements {@link AutoCloseable} for proper cleanup of background tasks.
 */
public class WeatherCacheableWrapperClient implements WeatherApi, AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(WeatherCacheableWrapperClient.class);

    private final Config config;
    private final WeatherApi client;
    private ScheduledExecutorService refresherSchedule;
    private final ConcurrentHashMap<String, WeatherDataCacheEntry> cache;
    private final ReentrantLock lock;

    /**
     * Creates a wrapper with default settings.
     *
     * @param client base client for retrieving weather data
     */
    public WeatherCacheableWrapperClient(WeatherApi client) {
        this(client, new Config());
    }

    /**
     * Creates a wrapper with specified configuration.
     *
     * @param client base client for retrieving weather data
     * @param config cache configuration (TTL, limit, refresh settings)
     */
    public WeatherCacheableWrapperClient(WeatherApi client, Config config) {
        this.client = Objects.requireNonNull(client);
        this.config = Objects.requireNonNull(config);
        this.cache = new ConcurrentHashMap<>(config.limit);
        this.lock = new ReentrantLock();
        if (config.refresher) {
            initRefresherSchedule();
        }
    }

    private void initRefresherSchedule() {
        this.refresherSchedule = Executors.newSingleThreadScheduledExecutor();
        refresherSchedule.scheduleAtFixedRate(
                this::refresh,
                config.refresherTime.toMillis(),
                config.refresherTime.toMillis(),
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns data from cache if it's still valid, otherwise performs a request to the base client.
     * When cache limit is reached, the entry with the nearest expiration time is removed.
     *
     * @param request request containing the city name
     * @return weather data (from cache or new request)
     * @throws WeatherException if an error occurred while retrieving data
     */
    @Override
    public WeatherDataResponse getByCityName(WeatherDataRequest request) throws WeatherException {
        String cityName = request.cityName();

        WeatherDataCacheEntry cachedEntry = cache.get(cityName);
        if (cachedEntry != null && !cachedEntry.isExpired()) {
            return cachedEntry.value;
        }

        if (cache.size() >= config.limit) {
            evictOldestIfNeeded();
        }

        return cache.compute(cityName, (key, entry) -> {
            if (entry != null && !entry.isExpired()) {
                return entry;
            }
            return new WeatherDataCacheEntry(client.getByCityName(request), config.ttl);
        }).value;
    }

    private void evictOldestIfNeeded() {
        lock.lock();
        try {
            if (cache.size() >= config.limit) {
                cache.entrySet().stream()
                        .min(Comparator.comparing(e -> e.getValue().expireAt))
                        .ifPresent(e -> cache.remove(e.getKey()));
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void close() {
        if (refresherSchedule != null) {
            refresherSchedule.shutdown();
        }
        lock.lock();
        try {
            cache.clear();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Refreshes expired entries in the cache in the background.
     * Called periodically if refresh mode is enabled.
     */
    private void refresh() {
        log.trace("Refreshing weather data");
        try {
            cache.entrySet().stream()
                    .filter(e -> e.getValue().isExpired())
                    .map(Map.Entry::getKey)
                    .forEach(key -> {
                        var request = new WeatherDataRequest(key);
                        var entry = new WeatherDataCacheEntry(client.getByCityName(request), config.ttl);
                        cache.put(key, entry);
                    });
        } catch (Throwable t) {
            log.error("Failed cache refresh", t);
        }
    }

    /**
     * Cache entry containing weather data and expiration time.
     *
     * @param value    weather data
     * @param expireAt cache expiration time
     */
    private record WeatherDataCacheEntry(WeatherDataResponse value, LocalDateTime expireAt) {

        /**
         * Creates a cache entry with specified TTL.
         *
         * @param value    weather data
         * @param expireAt cache entry lifetime duration
         */
        private WeatherDataCacheEntry(WeatherDataResponse value, Duration expireAt) {
            this(value, LocalDateTime.now().plus(expireAt));
        }

        private boolean isExpired() {
            return LocalDateTime.now().isAfter(expireAt);
        }

    }

    /**
     * Cache configuration for {@link WeatherCacheableWrapperClient}.
     * <p>
     * Allows configuring entry lifetime (TTL), maximum cache size,
     * and enabling automatic background data refresh.
     */
    public static class Config {

        private final static Duration DEFAULT_MINUTES_TTL = Duration.ofMinutes(10);
        private final static Duration DEFAULT_MINUTES_REFRESHER_TIME = Duration.ofMinutes(5);
        private final static int DEFAULT_LIMIT = 10;

        private final Duration ttl;
        private final int limit;
        private final boolean refresher;
        private final Duration refresherTime;

        /**
         * Creates configuration with default settings:
         * TTL = 10 minutes, limit = 10 entries, refresh disabled.
         */
        public Config() {
            this(DEFAULT_MINUTES_TTL, DEFAULT_LIMIT, false, DEFAULT_MINUTES_REFRESHER_TIME);
        }

        /**
         * Creates configuration with specified TTL and limit.
         *
         * @param ttl   cache entry lifetime
         * @param limit maximum number of entries in cache
         */
        public Config(Duration ttl, int limit) {
            this(ttl, limit, false, DEFAULT_MINUTES_REFRESHER_TIME);
        }

        /**
         * Creates configuration with refresh settings.
         *
         * @param refresher     whether to enable background cache refresh
         * @param refresherTime cache refresh interval
         */
        public Config(boolean refresher, Duration refresherTime) {
            this(DEFAULT_MINUTES_TTL, DEFAULT_LIMIT, refresher, refresherTime);
        }

        /**
         * Creates configuration with all parameters.
         *
         * @param ttl           cache entry lifetime (null = default value)
         * @param limit         maximum number of entries (0 = default value)
         * @param refresher     whether to enable background cache refresh
         * @param refresherTime cache refresh interval (null = default value)
         */
        public Config(Duration ttl, int limit, boolean refresher, Duration refresherTime) {
            this.ttl = ttl == null ? DEFAULT_MINUTES_TTL : ttl;
            this.limit = limit == 0 ? DEFAULT_LIMIT : limit;
            this.refresher = refresher;
            this.refresherTime = refresherTime == null ? DEFAULT_MINUTES_REFRESHER_TIME : refresherTime;
        }

    }

}
