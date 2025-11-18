package me.sukhov.weather.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.sukhov.weather.dto.response.WeatherDataResponse
import me.sukhov.weather.ui.service.WeatherService
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    MaterialTheme {
        var apiUrl by remember { mutableStateOf("https://api.openweathermap.org") }
        var apiKey by remember { mutableStateOf("your-api-key") }
        val weatherService = remember(apiUrl, apiKey) { 
            WeatherService(apiUrl, apiKey) 
        }
        val coroutineScope = rememberCoroutineScope()
        var weatherData by remember { mutableStateOf<WeatherDataResponse?>(null) }
        var cityName by remember { mutableStateOf("Moscow") }
        var isLoading by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf<String?>(null) }

        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .padding(top = 5.dp)
                .safeContentPadding()
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ApiSettings(
                apiUrl = apiUrl,
                onApiUrlChange = { apiUrl = it },
                apiKey = apiKey,
                onApiKeyChange = { apiKey = it }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = cityName,
                    onValueChange = { cityName = it },
                    label = { Text("City") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )

                Button(
                    onClick = {
                        isLoading = true
                        errorMessage = null
                        coroutineScope.launch {
                            try {
                                val data = withContext(Dispatchers.IO) {
                                    weatherService.getWeatherByCity(cityName)
                                }
                                weatherData = data
                                isLoading = false
                                if (data == null) {
                                    errorMessage = "Failed load weather data"
                                }
                            } catch (e: Exception) {
                                isLoading = false
                                errorMessage = "Error: ${e.message}"
                            }
                        }
                    },
                    enabled = !isLoading && cityName.isNotBlank() && apiUrl.isNotBlank() && apiKey.isNotBlank()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Load")
                    }
                }
            }

            errorMessage?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            Thermometer(
                weatherData = weatherData,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}