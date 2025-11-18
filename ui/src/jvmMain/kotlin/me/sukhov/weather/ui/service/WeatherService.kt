package me.sukhov.weather.ui.service

import me.sukhov.weather.dto.request.WeatherDataRequest
import me.sukhov.weather.dto.response.WeatherDataResponse
import me.sukhov.weather.ui.platform.JVMClient

class WeatherService(url: String, key: String) {

    private val weatherApi = JVMClient.weatherApi(url, key)

    fun getWeatherByCity(cityName: String): WeatherDataResponse? =
        runCatching {
            weatherApi.getByCityName(
                WeatherDataRequest.builder()
                    .cityName(cityName)
                    .build()
            )
        }.onFailure { it.printStackTrace() }
            .getOrNull()

}