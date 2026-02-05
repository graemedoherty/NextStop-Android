package com.example.nextstop_android.ui.splash

import android.graphics.BlurMaskFilter
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Devices
import com.example.nextstop_android.R
import kotlinx.coroutines.delay

private val fontPatterns = mapOf(
    'N' to listOf(listOf(1,0,0,0,1), listOf(1,1,0,0,1), listOf(1,0,1,0,1), listOf(1,0,0,1,1), listOf(1,0,0,0,1), listOf(1,0,0,0,1), listOf(1,0,0,0,1)),
    'E' to listOf(listOf(1,1,1,1,1), listOf(1,0,0,0,0), listOf(1,0,0,0,0), listOf(1,1,1,1,0), listOf(1,0,0,0,0), listOf(1,0,0,0,0), listOf(1,1,1,1,1)),
    'X' to listOf(listOf(1,0,0,0,1), listOf(1,0,0,0,1), listOf(0,1,0,1,0), listOf(0,0,1,0,0), listOf(0,1,0,1,0), listOf(1,0,0,0,1), listOf(1,0,0,0,1)),
    'T' to listOf(listOf(1,1,1,1,1), listOf(0,0,1,0,0), listOf(0,0,1,0,0), listOf(0,0,1,0,0), listOf(0,0,1,0,0), listOf(0,0,1,0,0), listOf(0,0,1,0,0)),
    'S' to listOf(listOf(0,1,1,1,0), listOf(1,0,0,0,1), listOf(1,0,0,0,0), listOf(0,1,1,1,0), listOf(0,0,0,0,1), listOf(1,0,0,0,1), listOf(0,1,1,1,0)),
    'O' to listOf(listOf(0,1,1,1,0), listOf(1,0,0,0,1), listOf(1,0,0,0,1), listOf(1,0,0,0,1), listOf(1,0,0,0,1), listOf(1,0,0,0,1), listOf(0,1,1,1,0)),
    'P' to listOf(listOf(1,1,1,1,0), listOf(1,0,0,0,1), listOf(1,0,0,0,1), listOf(1,1,1,1,0), listOf(1,0,0,0,0), listOf(1,0,0,0,0), listOf(1,0,0,0,0)),
    ' ' to listOf(listOf(0,0,0,0,0), listOf(0,0,0,0,0), listOf(0,0,0,0,0), listOf(0,0,0,0,0), listOf(0,0,0,0,0), listOf(0,0,0,0,0), listOf(0,0,0,0,0))
)

@Composable
fun LEDChar(char: Char, color: Color, dotSize: Float, dotGap: Float) {
    val pattern = fontPatterns[char.uppercaseChar()] ?: fontPatterns[' ']!!
    val glowColor = color.copy(alpha = 0.4f)

    Column(verticalArrangement = Arrangement.spacedBy(dotGap.dp)) {
        pattern.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(dotGap.dp)) {
                row.forEach { dot ->
                    val isActive = dot == 1
                    Box(
                        modifier = Modifier
                            .size(dotSize.dp)
                            .then(
                                if (isActive) {
                                    Modifier.drawBehind {
                                        drawIntoCanvas { canvas ->
                                            val paint = Paint().asFrameworkPaint()
                                            paint.color = glowColor.toArgb()
                                            paint.maskFilter = BlurMaskFilter(12f, BlurMaskFilter.Blur.NORMAL)
                                            canvas.nativeCanvas.drawCircle(
                                                size.width / 2,
                                                size.height / 2,
                                                size.width / 1.1f,
                                                paint
                                            )
                                        }
                                    }
                                } else Modifier
                            )
                            .background(if (isActive) color else Color(0xFF0F0F0F), CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
fun LEDMatrixSplashScreen(onTimeout: () -> Unit) {
    // 🔥 OPTIMIZED: Simpler, faster animation
    // - Removed scrolling text (was 3.8s)
    // - Just fade in logo (0.8s)
    // - Show for 1.2s total
    // - Exit animation handled by MainActivity (0.6s)
    // Total: ~2 seconds instead of 7+ seconds

    val fadeAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Fade in quickly
        fadeAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(800, easing = FastOutSlowInEasing)
        )
        // Show splash for just 1.2 seconds
        delay(1200)
        // Trigger exit (MainActivity handles the slide-out animation)
        onTimeout()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // Bottom: App icon and version
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp)
                .graphicsLayer { alpha = fadeAlpha.value },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.white_app_icon),
                contentDescription = null,
                modifier = Modifier.size(54.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "v1.0.0",
                color = Color.DarkGray,
                fontSize = 11.sp,
                letterSpacing = 1.sp
            )
        }

        // Center: LED Matrix "NEXT STOP" logo
        Box(
            modifier = Modifier.graphicsLayer { alpha = fadeAlpha.value },
            contentAlignment = Alignment.Center
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                "NEXT STOP".forEach { char ->
                    LEDChar(char, Color.White, 3.5f, 1.5f)
                }
            }
        }
    }
}

@Preview(name = "Optimized Splash Screen", device = Devices.PIXEL_4, showBackground = true)
@Composable
fun PreviewSplashScreen() {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        LEDMatrixSplashScreen(onTimeout = {})
    }
}