package com.realme.procam.ui.components

import android.graphics.Bitmap
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.realme.procam.theme.*

/**
 * Renders the semi-transparent center-cropped Ghost Overlay (Onion Skin)
 * from Shot 1 (1x Main Lens), scaled to match the 3x Telephoto FOV.
 * Guides the user in framing the distant subject in Step 2 & 3.
 */
@Composable
fun GhostOverlayView(
    ghostBitmap: Bitmap?,
    modifier: Modifier = Modifier
) {
    if (ghostBitmap == null) return

    val infiniteTransition = rememberInfiniteTransition(label = "GhostPulse")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BorderPulse"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize(0.92f)
                .clip(RoundedCornerShape(16.dp))
                .border(2.dp, AccentCyan.copy(alpha = borderAlpha), RoundedCornerShape(16.dp))
        ) {
            Image(
                bitmap = ghostBitmap.asImageBitmap(),
                contentDescription = "Shot 1 Ghost Guide (Onion Skin)",
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(0.38f),
                contentScale = ContentScale.Crop
            )

            // Guidance pill
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 16.dp)
            ) {
                Text(
                    text = "GHOST GUIDE ACTIVE (Shot 1 Alignment)",
                    color = AccentCyan,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
