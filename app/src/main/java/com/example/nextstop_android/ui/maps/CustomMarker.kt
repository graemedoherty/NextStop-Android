package com.example.nextstop_android.ui.maps

import android.content.Context
import android.graphics.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.nextstop_android.R
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.MarkerInfoWindow
import com.google.maps.android.compose.rememberMarkerState

// Helper to load the icon ONCE
fun getSharedPinIcon(context: Context): BitmapDescriptor {
    val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.pin)
    val aspectRatio = bitmap.height.toFloat() / bitmap.width.toFloat()
    val scaledWidth = 100 // Slightly smaller for better performance
    val scaledHeight = (scaledWidth * aspectRatio).toInt()
    val scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
    return BitmapDescriptorFactory.fromBitmap(scaledBitmap)
}

private fun createInfoWindowBitmap(title: String, isAlarmArmed: Boolean): Bitmap {
    val width = 700
    val height = if (isAlarmArmed) 280 else 340
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val cornerRadius = 40f

    // Background & Shadow
    val shadow = Paint().apply {
        color = 0x80000000.toInt()
        isAntiAlias = true
        maskFilter = BlurMaskFilter(15f, BlurMaskFilter.Blur.NORMAL)
    }
    canvas.drawRoundRect(RectF(30f, 30f, width - 30f, height - 30f), cornerRadius, cornerRadius, shadow)

    val bgPaint = Paint().apply {
        color = 0xFF1A1A1A.toInt()
        isAntiAlias = true
    }
    canvas.drawRoundRect(RectF(20f, 20f, width - 20f, height - 20f), cornerRadius, cornerRadius, bgPaint)

    // Border
    val border = Paint().apply {
        color = 0xFF6F66E3.toInt()
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }
    canvas.drawRoundRect(RectF(20f, 20f, width - 20f, height - 20f), cornerRadius, cornerRadius, border)

    // Text
    canvas.drawText(title, 60f, 110f, Paint().apply {
        color = Color.WHITE
        isAntiAlias = true
        textSize = 48f
        typeface = Typeface.DEFAULT_BOLD
    })

    canvas.drawText(if (isAlarmArmed) "Alarm Active" else "Tap to Select", 60f, 185f, Paint().apply {
        color = if (isAlarmArmed) 0xFF4CAF50.toInt() else 0xFF8F87EB.toInt()
        isAntiAlias = true
        textSize = 40f
    })

    return bitmap
}

@Composable
fun CustomMarker(
    position: LatLng,
    title: String,
    isAlarmArmed: Boolean,
    icon: BitmapDescriptor, // Pass the shared icon here
    onSelect: () -> Unit
) {
    val markerState = rememberMarkerState(position = position)

    MarkerInfoWindow(
        state = markerState,
        icon = icon,
        onInfoWindowClick = { if (!isAlarmArmed) onSelect() }
    ) {
        val infoBitmap = remember(title, isAlarmArmed) {
            createInfoWindowBitmap(title, isAlarmArmed)
        }
        Image(
            bitmap = infoBitmap.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.width(300.dp)
        )
    }
}