package com.example.nextstop_android.ui.permissions

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PermissionStepCard(
    pageIndex: Int,
    title: String,
    description: String,
    buttonText: String,
    onAction: () -> Unit
) {
    // 🔑 Theme-aware colors
    val purplePrimary = Color(0xFF6F66E3)
    val cardBackground = MaterialTheme.colorScheme.surfaceVariant // Adapts to light/dark
    val contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    val secondaryTextColor = contentColor.copy(alpha = 0.8f)

    AnimatedContent(
        targetState = pageIndex,
        transitionSpec = {
            (fadeIn(animationSpec = tween(800)) +
                    scaleIn(initialScale = 1.1f, animationSpec = tween(800)) +
                    slideInVertically(animationSpec = tween(800)) { -it / 4 })
                .togetherWith(
                    fadeOut(animationSpec = tween(600)) +
                            scaleOut(targetScale = 0.8f, animationSpec = tween(600)) +
                            slideOutVertically(animationSpec = tween(600)) { it / 2 }
                )
        },
        label = "CardStepTransition"
    ) { targetIndex ->
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            shape = RoundedCornerShape(28.dp),
            // 🔑 Changed from hardcoded #1A1A1A to theme surface
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
                                    if (index <= targetIndex) purplePrimary
                                    else contentColor.copy(alpha = 0.2f) // Adapts to light/dark
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (targetIndex == 0) {
                    WelcomeContent(
                        onAction,
                        buttonText,
                        purplePrimary,
                        contentColor,
                        secondaryTextColor
                    )
                } else {
                    StandardPermissionContent(
                        targetIndex,
                        title,
                        description,
                        buttonText,
                        onAction,
                        purplePrimary,
                        contentColor,
                        secondaryTextColor
                    )
                }
            }
        }
    }
}

@Composable
private fun WelcomeContent(
    onAction: () -> Unit,
    buttonText: String,
    purple: Color,
    mainText: Color,
    subText: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "👋", fontSize = 48.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Welcome to Next Stop",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = mainText
        )
        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Quick Setup Guide",
            style = MaterialTheme.typography.titleMedium,
            color = mainText.copy(alpha = 0.9f),
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "To alert you at just the right moment, Next Stop needs a few quick permissions. Setup only takes a minute — and then you're good to go.",
            style = MaterialTheme.typography.bodyMedium,
            color = subText,
            textAlign = TextAlign.Justify,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        val steps = listOf(
            "Choose your mode of travel.",
            "Pick your destination from the dropdown or the interactive map.",
            "Set your alarm.",
            "Relax — we'll alert you when you are near your destination station."
        )

        steps.forEachIndexed { index, step ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = "${index + 1}. ",
                    color = purple,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = step,
                    color = mainText.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 20.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
        ActionButton(buttonText, onAction, purple)
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
    mainText: Color,
    subText: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = when (index) {
                1 -> "📍"
                2 -> "🔔"
                3 -> "📱"
                else -> "🎉"
            },
            fontSize = 48.sp
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = mainText,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = subText,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )
        Spacer(modifier = Modifier.height(40.dp))
        ActionButton(buttonText, onAction, purple)
    }
}

@Composable
private fun ActionButton(text: String, onClick: () -> Unit, purple: Color) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = purple)
    ) {
        Text(text = text, fontWeight = FontWeight.Bold, color = Color.White)
    }
}