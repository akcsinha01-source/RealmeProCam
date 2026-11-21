package com.realme.procam.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterCenterFocus
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.realme.procam.processing.HybridFocusStacker
import com.realme.procam.processing.HybridFocusStacker.StackingMethod
import com.realme.procam.processing.HybridFocusStacker.StackingPhase
import com.realme.procam.theme.*

@Composable
fun DualLensProgressIndicator(
    phase: StackingPhase,
    statusMessage: String,
    stackingMethod: StackingMethod,
    onToggleMethod: (StackingMethod) -> Unit,
    modifier: Modifier = Modifier
) {
    if (phase == StackingPhase.IDLE) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(OledSurface.copy(alpha = 0.94f), RoundedCornerShape(16.dp))
            .border(1.5.dp, RealmeGold.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Stacking Method Toggle Pills
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(OledCard)
                .border(1.dp, OledBorder, CircleShape)
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (stackingMethod == StackingMethod.FULL_FRAME_1X) RealmeGold else Color.Transparent)
                    .clickable { onToggleMethod(StackingMethod.FULL_FRAME_1X) }
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "1x FULL FRAME (Edge-to-Edge)",
                    color = if (stackingMethod == StackingMethod.FULL_FRAME_1X) Black else TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (stackingMethod == StackingMethod.HYBRID_1X_3X) RealmeGold else Color.Transparent)
                    .clickable { onToggleMethod(StackingMethod.HYBRID_1X_3X) }
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "1x+3x HYBRID TELE",
                    color = if (stackingMethod == StackingMethod.HYBRID_1X_3X) Black else TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Step Icons Row
        if (stackingMethod == StackingMethod.FULL_FRAME_1X) {
            // Simplified, Clean Full-Frame 1x Steps
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StepNode(
                    step = 1,
                    title = "Near Tap",
                    icon = Icons.Default.FilterCenterFocus,
                    isCurrent = phase == StackingPhase.STEP1_NEAR_PROMPT || phase == StackingPhase.STEP1_CAPTURING,
                    isCompleted = phase > StackingPhase.STEP1_CAPTURING
                )

                StepConnector(isCompleted = phase > StackingPhase.STEP1_CAPTURING)

                StepNode(
                    step = 2,
                    title = "Far 1x (Auto)",
                    icon = Icons.Default.ZoomIn,
                    isCurrent = phase == StackingPhase.STEP3_CAPTURING,
                    isCompleted = phase > StackingPhase.STEP3_CAPTURING
                )

                StepConnector(isCompleted = phase >= StackingPhase.STEP4_BLENDING)

                StepNode(
                    step = 3,
                    title = "100% Sharp",
                    icon = Icons.Default.Layers,
                    isCurrent = phase == StackingPhase.STEP4_BLENDING,
                    isCompleted = phase == StackingPhase.COMPLETED
                )
            }
        } else {
            // Hybrid 1x + 3x Periscope Steps
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StepNode(
                    step = 1,
                    title = "Near (1x)",
                    icon = Icons.Default.FilterCenterFocus,
                    isCurrent = phase == StackingPhase.STEP1_NEAR_PROMPT || phase == StackingPhase.STEP1_CAPTURING,
                    isCompleted = phase > StackingPhase.STEP1_CAPTURING
                )

                StepConnector(isCompleted = phase > StackingPhase.STEP1_CAPTURING)

                StepNode(
                    step = 2,
                    title = "Ghost Guide",
                    icon = Icons.Default.ZoomIn,
                    isCurrent = phase == StackingPhase.STEP2_GUIDED_TRANSITION,
                    isCompleted = phase > StackingPhase.STEP2_GUIDED_TRANSITION
                )

                StepConnector(isCompleted = phase > StackingPhase.STEP2_GUIDED_TRANSITION)

                StepNode(
                    step = 3,
                    title = "Far (3x)",
                    icon = Icons.Default.ZoomIn,
                    isCurrent = phase == StackingPhase.STEP3_FAR_PROMPT || phase == StackingPhase.STEP3_CAPTURING,
                    isCompleted = phase > StackingPhase.STEP3_CAPTURING
                )

                StepConnector(isCompleted = phase > StackingPhase.STEP3_CAPTURING)

                StepNode(
                    step = 4,
                    title = "AI Merge",
                    icon = Icons.Default.Layers,
                    isCurrent = phase == StackingPhase.STEP4_BLENDING,
                    isCompleted = phase == StackingPhase.COMPLETED
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // On-Screen Hinglish Guidance Prompt
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (phase == StackingPhase.STEP1_CAPTURING || phase == StackingPhase.STEP3_CAPTURING || phase == StackingPhase.STEP4_BLENDING) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = RealmeGold,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            Text(
                text = statusMessage,
                color = if (phase == StackingPhase.STEP2_GUIDED_TRANSITION || phase == StackingPhase.STEP3_FAR_PROMPT) AccentCyan else RealmeGold,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun StepNode(
    step: Int,
    title: String,
    icon: ImageVector,
    isCurrent: Boolean,
    isCompleted: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isCompleted -> FocusGreen.copy(alpha = 0.2f)
                        isCurrent -> RealmeGold.copy(alpha = 0.25f)
                        else -> OledCard
                    }
                )
                .border(
                    1.5.dp,
                    when {
                        isCompleted -> FocusGreen
                        isCurrent -> RealmeGold
                        else -> OledBorder
                    },
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(Icons.Default.Check, contentDescription = null, tint = FocusGreen, modifier = Modifier.size(16.dp))
            } else {
                Text(
                    text = "$step",
                    color = if (isCurrent) RealmeGold else TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = title,
            color = if (isCurrent) RealmeGold else TextSecondary,
            fontSize = 9.sp
        )
    }
}

@Composable
private fun StepConnector(isCompleted: Boolean) {
    Box(
        modifier = Modifier
            .width(18.dp)
            .height(2.dp)
            .background(if (isCompleted) FocusGreen else OledBorder)
    )
}
