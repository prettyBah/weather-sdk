package me.sukhov.weather.ui.platform

import me.sukhov.weather.service.WeatherApi
import me.sukhov.weather.service.client.WeatherCacheableWrapperClient
import me.sukhov.weather.service.client.WeatherJavaHttpClient
import me.sukhov.weather.service.client.config.WeatherApiConfig

object JVMClient {

    fun weatherApi(url: String, key: String): WeatherApi = WeatherCacheableWrapperClient(
        WeatherJavaHttpClient.createDefault(
            WeatherApiConfig.builder()
                .url(url)
                .apiKey(key)
                .units(WeatherApiConfig.WeatherUnits.METRIC)
                .build()
        )
    )

}