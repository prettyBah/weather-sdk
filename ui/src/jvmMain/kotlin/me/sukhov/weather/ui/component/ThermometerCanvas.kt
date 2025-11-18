package me.sukhov.weather.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun ThermometerCanvas(
    progress: Float,
    temperatureColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val centerX = width / 2

        val bulbRadius = width * 0.25f
        val stemWidth = width * 0.15f
        val stemHeight = height * 0.7f
        val stemTop = height * 0.15f
        val bulbCenterY = height - bulbRadius - 10.dp.toPx()

        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFF9FAFB),
                    Color(0xFFE5E7EB),
                    Color(0xFFD1D5DB)
                ),
                startY = stemTop,
                endY = stemTop + stemHeight
            ),
            topLeft = Offset(centerX - stemWidth / 2, stemTop),
            size = Size(stemWidth, stemHeight),
            cornerRadius = CornerRadius(stemWidth / 2, stemWidth / 2)
        )
        
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.Black.copy(alpha = 0.05f),
                    Color.Transparent
                ),
                startY = stemTop,
                endY = stemTop + stemHeight
            ),
            topLeft = Offset(centerX - stemWidth / 2 + 1.dp.toPx(), stemTop + 1.dp.toPx()),
            size = Size(stemWidth - 2.dp.toPx(), stemHeight - 2.dp.toPx()),
            cornerRadius = CornerRadius((stemWidth - 2.dp.toPx()) / 2, (stemWidth - 2.dp.toPx()) / 2)
        )

        val filledHeight = stemHeight * progress
        if (filledHeight > 0) {
            val fillTop = stemTop + stemHeight - filledHeight
            
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        temperatureColor.copy(alpha = 0.9f),
                        temperatureColor,
                        temperatureColor.copy(alpha = 0.95f)
                    ),
                    startY = fillTop,
                    endY = stemTop + stemHeight
                ),
                topLeft = Offset(centerX - stemWidth / 2, fillTop),
                size = Size(stemWidth, filledHeight),
                cornerRadius = CornerRadius(stemWidth / 2, stemWidth / 2)
            )
            
            if (filledHeight > stemWidth) {
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.3f),
                            Color.Transparent
                        ),
                        startY = fillTop,
                        endY = fillTop + stemWidth * 0.5f
                    ),
                    topLeft = Offset(centerX - stemWidth / 2 + 1.dp.toPx(), fillTop + 1.dp.toPx()),
                    size = Size(stemWidth - 2.dp.toPx(), (stemWidth * 0.5f).coerceAtMost(filledHeight - 2.dp.toPx())),
                    cornerRadius = CornerRadius((stemWidth - 2.dp.toPx()) / 2, (stemWidth - 2.dp.toPx()) / 2)
                )
            }
            
            val connectionRadius = stemWidth / 2
            val connectionY = stemTop + stemHeight
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        temperatureColor,
                        temperatureColor.copy(alpha = 0.8f)
                    ),
                    center = Offset(centerX, connectionY),
                    radius = connectionRadius
                ),
                radius = connectionRadius,
                center = Offset(centerX, connectionY)
            )
        }

        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF9CA3AF),
                    Color(0xFF6B7280),
                    Color(0xFF9CA3AF)
                ),
                startY = stemTop,
                endY = stemTop + stemHeight
            ),
            topLeft = Offset(centerX - stemWidth / 2, stemTop),
            size = Size(stemWidth, stemHeight),
            cornerRadius = CornerRadius(stemWidth / 2, stemWidth / 2),
            style = Stroke(width = 1.5.dp.toPx())
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFF9FAFB),
                    Color(0xFFF3F4F6),
                    Color(0xFFE5E7EB),
                    Color(0xFFD1D5DB)
                ),
                center = Offset(centerX - bulbRadius * 0.2f, bulbCenterY - bulbRadius * 0.2f),
                radius = bulbRadius * 1.3f
            ),
            radius = bulbRadius,
            center = Offset(centerX, bulbCenterY)
        )
        
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.Black.copy(alpha = 0.08f)
                ),
                center = Offset(centerX, bulbCenterY + bulbRadius * 0.3f),
                radius = bulbRadius * 0.7f
            ),
            radius = bulbRadius * 0.9f,
            center = Offset(centerX, bulbCenterY)
        )

        if (progress > 0) {
            val bulbFillRadius = bulbRadius * 0.92f
            val bulbCenter = Offset(centerX, bulbCenterY)
            
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        temperatureColor.copy(alpha = 0.95f),
                        temperatureColor,
                        temperatureColor.copy(alpha = 0.85f)
                    ),
                    center = Offset(centerX - bulbFillRadius * 0.25f, bulbCenterY - bulbFillRadius * 0.25f),
                    radius = bulbFillRadius * 1.1f
                ),
                radius = bulbFillRadius,
                center = bulbCenter
            )
            
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.5f),
                        Color.White.copy(alpha = 0.2f),
                        Color.Transparent
                    ),
                    center = Offset(centerX - bulbFillRadius * 0.35f, bulbCenterY - bulbFillRadius * 0.35f),
                    radius = bulbFillRadius * 0.6f
                ),
                radius = bulbFillRadius * 0.5f,
                center = Offset(centerX - bulbFillRadius * 0.25f, bulbCenterY - bulbFillRadius * 0.25f)
            )
            
            drawCircle(
                color = Color.White.copy(alpha = 0.4f),
                radius = bulbFillRadius * 0.15f,
                center = Offset(centerX - bulbFillRadius * 0.4f, bulbCenterY - bulbFillRadius * 0.4f)
            )
            
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    center = Offset(centerX, bulbCenterY + bulbFillRadius * 0.3f),
                    radius = bulbFillRadius * 0.5f
                ),
                radius = bulbFillRadius * 0.4f,
                center = Offset(centerX, bulbCenterY + bulbFillRadius * 0.2f)
            )
        }

        drawCircle(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF9CA3AF),
                    Color(0xFF6B7280),
                    Color(0xFF9CA3AF)
                ),
                start = Offset(centerX - bulbRadius, bulbCenterY - bulbRadius),
                end = Offset(centerX + bulbRadius, bulbCenterY + bulbRadius)
            ),
            radius = bulbRadius,
            center = Offset(centerX, bulbCenterY),
            style = Stroke(width = 2.5.dp.toPx())
        )
        
        drawCircle(
            color = Color.White.copy(alpha = 0.3f),
            radius = bulbRadius * 0.95f,
            center = Offset(centerX, bulbCenterY),
            style = Stroke(width = 1.dp.toPx())
        )

        val divisions = 10
        for (i in 1 until divisions) {
            val y = stemTop + (stemHeight / divisions) * i
            val markLength = if (i % 2 == 0) stemWidth * 0.5f else stemWidth * 0.2f

            drawLine(
                color = Color(0xFF9CA3AF),
                start = Offset(centerX - stemWidth / 2 - markLength, y),
                end = Offset(centerX - stemWidth / 2, y),
                strokeWidth = 2.dp.toPx()
            )
        }
    }
}

