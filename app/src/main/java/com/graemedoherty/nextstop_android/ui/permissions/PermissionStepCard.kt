package com.graemedoherty.nextstop_android.ui.permissions

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateValue
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.graemedoherty.nextstop_android.R

@Composable
fun PermissionStepCard(
    pageIndex: Int,
    title: String,
    description: String,
    buttonText: String,
    onAction: () -> Unit
) {
    val purplePrimary = Color(0xFF6F66E3)
    val cardBackground = MaterialTheme.colorScheme.surfaceVariant
    val contentColor = MaterialTheme.colorScheme.onSurfaceVariant

    AnimatedContent(
        targetState = pageIndex,
        transitionSpec = {
            (fadeIn(animationSpec = tween(800)) + scaleIn(initialScale = 1.1f)).togetherWith(fadeOut())
        },
        label = "CardStepTransition"
    ) { targetIndex ->
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = cardBackground)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Progress Bar
                Row(
                    modifier = Modifier.width(120.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    repeat(5) { index ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(
                                    if (index <= targetIndex) purplePrimary else contentColor.copy(
                                        alpha = 0.2f
                                    )
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                StandardPermissionContent(
                    targetIndex,
                    title,
                    description,
                    buttonText,
                    onAction,
                    purplePrimary,
                    contentColor
                )
            }
        }
    }
}

@Composable
private fun StandardPermissionContent(
    index: Int,
    title: String,
    description: String,
    buttonText: String,
    onAction: () -> Unit,
    purple: Color,
    mainText: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (index == 3) {
            OverlayInstructionAnimation(purple, mainText)
        } else {
            Text(
                text = when (index) {
                    0 -> "👋"
                    1 -> "📍"
                    2 -> "🔔"
                    else -> "🎉"
                },
                fontSize = 48.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = mainText
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = mainText.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
        Spacer(modifier = Modifier.height(32.dp))
        ActionButton(buttonText, onAction, purple)
    }
}

@Composable
fun OverlayInstructionAnimation(purple: Color, mainText: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "toggle")

    // Animation math for perfect toggle travel
    val toggleOffset by infiniteTransition.animateValue(
        initialValue = 0.dp,
        targetValue = 24.dp,
        typeConverter = Dp.VectorConverter,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "thumb"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(mainText.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "SYSTEM SETTINGS PREVIEW",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = mainText.copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 🔑 FIX: Swapped R.mipmap.ic_launcher for R.drawable.next_stop_logo
                // painterResource cannot load Adaptive Icons (XML) directly.
                Image(
                    painter = painterResource(id = R.drawable.next_stop_logo),
                    contentDescription = "App Logo",
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Fit
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Next Stop",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = mainText
                )
            }

            // Toggle Track
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(26.dp)
                    .clip(CircleShape)
                    .background(if (toggleOffset > 12.dp) purple else mainText.copy(alpha = 0.2f))
                    .padding(3.dp)
            ) {
                // Toggle Thumb
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .offset(x = toggleOffset)
                        .background(Color.White, CircleShape)
                )
            }
        }
    }
}

@Composable
private fun ActionButton(text: String, onClick: () -> Unit, purple: Color) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = purple)
    ) {
        Text(text = text, fontWeight = FontWeight.Bold, color = Color.White)
    }
}
