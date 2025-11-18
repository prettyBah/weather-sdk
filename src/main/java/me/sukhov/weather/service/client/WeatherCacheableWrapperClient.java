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
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Decorator for {@link WeatherApi} that adds weather data caching.
 * <p>
 * Caches request results for a specified time (TTL) and automatically manages expired entries.
 * Supports cache size limits with eviction of entries with nearest expiration time when limit is reached.
 * Provides two background refresh modes:
 * <ul>
 *   <li>Update mode: automatically refreshes expired entries in the background</li>
 *   <li>Delete mode: automatically removes expired entries from cache</li>
 * </ul>
 * <p>
 * Thread-safe implementation using {@link ConcurrentHashMap} with atomic operations for optimal performance.
 * Lock is only used for eviction operations that require finding and removing the oldest entry atomically.
 * Background refresh operations use per-entry atomic operations, allowing concurrent access to other entries.
 * <p>
 * Implements {@link AutoCloseable} for proper cleanup of background tasks and cache resources.
 * Always use try-with-resources or explicitly call {@link #close()} to prevent resource leaks.
 */
public class WeatherCacheableWrapperClient implements WeatherApi, AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(WeatherCacheableWrapperClient.class);

    private final Config config;
    private final WeatherApi client;
    private final ScheduledExecutorService refresherSchedule;

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
        this.refresherSchedule = Executors.newSingleThreadScheduledExecutor();
        initRefresherSchedule(config.refresherUpdate);
    }

    private void initRefresherSchedule(boolean refresherUpdate) {
        refresherSchedule.scheduleAtFixedRate(
                refresherUpdate ? this::refresherUpdate : this::refresherDelete,
                config.refresherTime.toMillis(),
                config.refresherTime.toMillis(),
                TimeUnit.MILLISECONDS
        );
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns weather data from cache if it's still valid (not expired), otherwise performs
     * a request to the base client and caches the result.
     * <p>
     * When cache limit is reached, the entry with the nearest expiration time is evicted
     * before adding a new entry. This ensures that the most recently used entries with
     * longer remaining TTL are preserved.
     *
     * @param request request containing the city name
     * @return weather data (from cache if valid, otherwise from new request)
     * @throws WeatherException if an error occurred while retrieving data from the base client
     */
    @Override
    public WeatherDataResponse getByCityName(WeatherDataRequest request) throws WeatherException {
        return getEntryByKey(request.cityName(), () -> client.getByCityName(request)).value;
    }

    private WeatherDataCacheEntry getEntryByKey(String key, Supplier<WeatherDataResponse> supplierResponse) {
        var cachedEntry = cache.get(key);
        if (cachedEntry != null && !cachedEntry.isExpired()) {
            return cachedEntry;
        }

        if (cache.size() >= config.limit) {
            evictOldestIfNeeded();
        }

        return cache.compute(key, (k, entry) -> {
            if (entry != null && !entry.isExpired()) {
                return entry;
            }
            return new WeatherDataCacheEntry(supplierResponse.get(), config.ttl, supplierResponse);
        });
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

    /**
     * Closes this cacheable wrapper client.
     * <p>
     * Shuts down the background refresh scheduler and clears all cached entries.
     * This method should be called to properly release resources, preferably using
     * try-with-resources statement.
     */
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
     * <p>
     * Called periodically by the scheduled executor if refresh update mode is enabled.
     * For each expired entry, performs a new request using the stored supplier
     * and updates the cache entry with fresh data and new expiration time.
     * <p>
     * Uses atomic operations per entry, allowing concurrent access to other cache entries.
     * Errors during refresh are logged but do not interrupt the refresh process.
     */
    private void refresherUpdate() {
        if (cache.isEmpty()) {
            return;
        }

        log.trace("Refreshing update weather data");
        try {
            cache.forEach((key, entry) -> {
                if (entry.isExpired()) {
                    cache.computeIfPresent(key, (k, oldEntry) -> {
                        if (oldEntry.isExpired()) {
                            try {
                                var supplierResponse = oldEntry.supplierResponse;
                                var response = supplierResponse.get();
                                return new WeatherDataCacheEntry(response, config.ttl, supplierResponse);
                            } catch (Throwable t) {
                                log.error("Failed to refresh entry for key: {}", key, t);
                                return oldEntry;
                            }
                        }
                        return oldEntry;
                    });
                }
            });
        } catch (Throwable t) {
            log.error("Failed cache update", t);
        }
    }

    /**
     * Removes expired entries from the cache in the background.
     * <p>
     * Called periodically by the scheduled executor if refresh delete mode is enabled
     * (default mode when refresh update is disabled).
     * Removes all entries that have expired their TTL to free up cache space.
     * <p>
     * Uses atomic operations per entry, allowing concurrent access to other cache entries.
     * Errors during deletion are logged but do not interrupt the cleanup process.
     */
    private void refresherDelete() {
        if (cache.isEmpty()) {
            return;
        }

        log.trace("Refreshing delete weather data");
        try {
            cache.forEach((key, entry) -> {
                if (entry.isExpired()) {
                    cache.remove(key, entry);
                }
            });
        } catch (Throwable t) {
            log.error("Failed cache delete", t);
        }
    }

    /**
     * Cache entry containing weather data, expiration time, and supplier for refresh.
     *
     * @param value            weather data response
     * @param expireAt         cache expiration time (when this entry becomes invalid)
     * @param supplierResponse supplier function for refreshing this entry when expired
     */
    private record WeatherDataCacheEntry(
            WeatherDataResponse value,
            LocalDateTime expireAt,
            Supplier<WeatherDataResponse> supplierResponse
    ) {

        /**
         * Creates a cache entry with specified TTL.
         * <p>
         * Calculates expiration time as current time plus the provided TTL duration.
         *
         * @param value             weather data response
         * @param expireAt         cache entry lifetime duration (TTL)
         * @param supplierResponse supplier function for refreshing this entry
         */
        private WeatherDataCacheEntry(WeatherDataResponse value, Duration expireAt, Supplier<WeatherDataResponse> supplierResponse) {
            this(value, LocalDateTime.now().plus(expireAt), supplierResponse);
        }

       
        private boolean isExpired() {
            return LocalDateTime.now().isAfter(expireAt);
        }

    }

    /**
     * Cache configuration for {@link WeatherCacheableWrapperClient}.
     * <p>
     * Allows configuring entry lifetime (TTL), maximum cache size,
     * and enabling automatic background data refresh or cleanup.
     * <p>
     * Default values:
     * <ul>
     *   <li>TTL: 10 minutes</li>
     *   <li>Limit: 10 entries</li>
     *   <li>Refresh update: disabled (delete mode enabled)</li>
     *   <li>Refresher time: 5 minutes</li>
     * </ul>
     */
    public static class Config {

        private final static Duration DEFAULT_MINUTES_TTL = Duration.ofMinutes(10);
        private final static Duration DEFAULT_MINUTES_REFRESHER_TIME = Duration.ofMinutes(5);
        private final static int DEFAULT_LIMIT = 10;

        private final Duration ttl;
        private final int limit;
        private final boolean refresherUpdate;
        private final Duration refresherTime;

        /**
         * Creates configuration with default settings:
         * TTL = 10 minutes, limit = 10 entries, refresh update disabled (delete mode enabled).
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
         * <p>
         * Uses default TTL (10 minutes) and limit (10 entries).
         *
         * @param refresherUpdate whether to enable background cache refresh (true) or delete mode (false)
         * @param refresherTime   background refresh/cleanup interval
         */
        public Config(boolean refresherUpdate, Duration refresherTime) {
            this(DEFAULT_MINUTES_TTL, DEFAULT_LIMIT, refresherUpdate, refresherTime);
        }

        /**
         * Creates configuration with all parameters.
         * <p>
         * Allows fine-grained control over all cache settings. Null and zero values
         * are replaced with defaults for convenience.
         *
         * @param ttl             cache entry lifetime (null = default 10 minutes)
         * @param limit           maximum number of entries in cache (0 = default 10 entries)
         * @param refresherUpdate whether to enable background cache refresh (true) or delete mode (false)
         * @param refresherTime   background refresh/cleanup interval (null = default 5 minutes)
         */
        public Config(Duration ttl, int limit, boolean refresherUpdate, Duration refresherTime) {
            this.ttl = ttl == null ? DEFAULT_MINUTES_TTL : ttl;
            this.limit = limit == 0 ? DEFAULT_LIMIT : limit;
            this.refresherUpdate = refresherUpdate;
            this.refresherTime = refresherTime == null ? DEFAULT_MINUTES_REFRESHER_TIME : refresherTime;
        }

    }

}
