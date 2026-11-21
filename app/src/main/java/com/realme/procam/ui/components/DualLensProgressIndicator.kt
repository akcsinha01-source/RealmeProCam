package com.realme.procam.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.realme.procam.processing.HybridFocusStacker.StackingMethod
import com.realme.procam.processing.HybridFocusStacker.StackingPhase
import com.realme.procam.theme.*

/**
 * Ultra-compact, non-intrusive floating status pill for Focus Stacking.
 * Leaves the camera viewfinder completely clean and unobstructed.
 */
@Composable
fun DualLensProgressIndicator(
    phase: StackingPhase,
    statusMessage: String,
    stackingMethod: StackingMethod,
    onToggleMethod: (StackingMethod) -> Unit,
    modifier: Modifier = Modifier
) {
    if (phase == StackingPhase.IDLE) return

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.Black.copy(alpha = 0.65f))
            .border(1.dp, RealmeGold.copy(alpha = 0.7f), RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (phase == StackingPhase.STEP1_CAPTURING || phase == StackingPhase.STEP3_CAPTURING || phase == StackingPhase.STEP4_BLENDING) {
                CircularProgressIndicator(
                    modifier = Modifier.size(13.dp),
                    color = RealmeGold,
                    strokeWidth = 2.dp
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (phase == StackingPhase.COMPLETED) FocusGreen else RealmeGold)
                )
            }

            Text(
                text = statusMessage.ifEmpty { "Same-Zoom Focus Stacking" },
                color = if (phase == StackingPhase.COMPLETED) FocusGreen else Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
