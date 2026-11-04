package com.apexfission.android.carddetector.ui.overlays

import androidx.compose.material3.MaterialTheme
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp

@Composable
fun CaptureShutterButton(
    isCaptureEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedShutterAlpha by animateFloatAsState(
        targetValue = if (isCaptureEnabled) 1.0f else 0.4f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "shutterAlpha"
    )

    val animatedInnerSize by animateDpAsState(
        targetValue = if (isCaptureEnabled) 64.dp else 52.dp,
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
            dampingRatio = Spring.DampingRatioMediumBouncy
        ),
        label = "shutterInnerSize"
    )

    val animatedBorderAlpha by animateFloatAsState(
        targetValue = if (isCaptureEnabled) 1.0f else 0.5f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "shutterBorderAlpha"
    )

    Box(
        modifier = modifier
            .size(80.dp)
            .alpha(animatedShutterAlpha)
            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.small)
            .clickable(
                enabled = isCaptureEnabled,
                role = Role.Button,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .border(4.dp, MaterialTheme.colorScheme.primary.copy(alpha = animatedBorderAlpha), MaterialTheme.shapes.small),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(animatedInnerSize)
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = animatedShutterAlpha),
                    shape = MaterialTheme.shapes.small
                )
        )
    }
}
