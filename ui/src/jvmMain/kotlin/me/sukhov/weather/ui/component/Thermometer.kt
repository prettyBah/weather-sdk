package me.sukhov.weather.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.sukhov.weather.dto.response.WeatherDataResponse
import kotlin.math.roundToInt

@Composable
fun Thermometer(
    weatherData: WeatherDataResponse?,
    modifier: Modifier = Modifier
) {
    val temperature = weatherData?.temperature()?.temp()
    val feelsLike = weatherData?.temperature()?.feelsLike()
    val cityName = weatherData?.name() ?: "—"

    val animatedProgress by animateFloatAsState(
        targetValue = temperature?.let { ((it.toFloat() + 20) / 70).coerceIn(0f, 1f) } ?: 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "thermometer_progress"
    )

    val animatedTemperature by animateFloatAsState(
        targetValue = temperature?.toFloat() ?: 0f,
        animationSpec = tween(
            durationMillis = 1000,
            easing = FastOutSlowInEasing
        ),
        label = "temperature_value"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = cityName,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Card(
            modifier = Modifier
                .size(120.dp, 360.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(24.dp),
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                ),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                ThermometerCanvas(
                    progress = animatedProgress,
                    temperatureColor = getTemperatureColor(animatedTemperature.toDouble()),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${animatedTemperature.roundToInt()}°",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = getTemperatureColor(animatedTemperature.toDouble()),
                fontSize = 30.sp
            )
        }

        feelsLike?.let {
            Text(
                text = "Feels like ${it.roundToInt()}°C",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
        }
        weatherData?.weather()?.let { weather ->
            Text(
                text = weather.description() ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        }
    }
}

private fun getTemperatureColor(celsius: Double): Color {
    return when {
        celsius < -10 -> Color(0xFF1E3A8A)
        celsius < 0 -> Color(0xFF3B82F6)
        celsius < 10 -> Color(0xFF60A5FA)
        celsius < 20 -> Color(0xFF34D399)
        celsius < 30 -> Color(0xFFFBBF24)
        celsius < 40 -> Color(0xFFF97316)
        else -> Color(0xFFDC2626)
    }
}

