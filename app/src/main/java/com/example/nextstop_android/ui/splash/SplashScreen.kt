//package com.example.nextstop_android.ui.splash
//
//import androidx.compose.animation.core.FastOutSlowInEasing
//import androidx.compose.animation.core.animateDpAsState
//import androidx.compose.animation.core.animateFloatAsState
//import androidx.compose.animation.core.tween
//import androidx.compose.foundation.Image
//import androidx.compose.foundation.background
//import androidx.compose.foundation.layout.Arrangement
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.Spacer
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.height
//import androidx.compose.foundation.layout.offset
//import androidx.compose.foundation.layout.size
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.LaunchedEffect
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.runtime.setValue
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.draw.alpha
//import androidx.compose.ui.draw.scale
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.layout.ContentScale
//import androidx.compose.ui.res.painterResource
//import androidx.compose.ui.unit.dp
//import com.example.nextstop_android.R
//import kotlinx.coroutines.delay
//
//@Composable
//fun SplashScreen(
//    onSplashFinished: () -> Unit
//) {
//    var textVisible by remember { mutableStateOf(false) }
//    var logoVisible by remember { mutableStateOf(false) }
//
//    // Text animation (appears first)
//    val textAlpha by animateFloatAsState(
//        targetValue = if (textVisible) 1f else 0f,
//        animationSpec = tween(
//            durationMillis = 600,
//            easing = FastOutSlowInEasing
//        ),
//        label = "textAlpha"
//    )
//
//    val textOffset by animateDpAsState(
//        targetValue = if (textVisible) 0.dp else (-20).dp,
//        animationSpec = tween(
//            durationMillis = 600,
//            easing = FastOutSlowInEasing
//        ),
//        label = "textOffset"
//    )
//
//    // Logo animation (appears second, slightly delayed)
//    val logoAlpha by animateFloatAsState(
//        targetValue = if (logoVisible) 1f else 0f,
//        animationSpec = tween(
//            durationMillis = 600,
//            easing = FastOutSlowInEasing
//        ),
//        label = "logoAlpha"
//    )
//
//    val logoScale by animateFloatAsState(
//        targetValue = if (logoVisible) 1f else 0.85f,
//        animationSpec = tween(
//            durationMillis = 600,
//            easing = FastOutSlowInEasing
//        ),
//        label = "logoScale"
//    )
//
//    // Animation sequence
//    LaunchedEffect(Unit) {
//        textVisible = true // Text appears first
//        delay(300) // Small delay before logo
//        logoVisible = true
//        delay(1900) // Total display time ~2.2 seconds
//        onSplashFinished()
//    }
//
//    Box(
//        modifier = Modifier
//            .fillMaxSize()
//            .background(Color.Black),
//        contentAlignment = Alignment.Center
//    ) {
//        Column(
//            horizontalAlignment = Alignment.CenterHorizontally,
//            verticalArrangement = Arrangement.Center
//        ) {
//            // Text logo (on top)
//            Image(
//                painter = painterResource(id = R.drawable.next_stop_text),
//                contentDescription = "NextStop Text",
//                modifier = Modifier
//                    .fillMaxWidth(0.7f)
//                    .offset(y = textOffset)
//                    .alpha(textAlpha),
//                contentScale = ContentScale.Fit
//            )
//
//            Spacer(modifier = Modifier.height(32.dp))
//
//            // Main logo (below text)
//            Image(
//                painter = painterResource(id = R.drawable.white_app_icon),
//                contentDescription = "NextStop Logo",
//                modifier = Modifier
//                    .size(160.dp)
//                    .scale(logoScale)
//                    .alpha(logoAlpha),
//                contentScale = ContentScale.Fit
//            )
//        }
//    }
//}