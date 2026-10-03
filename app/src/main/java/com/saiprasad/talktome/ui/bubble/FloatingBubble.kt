package com.saiprasad.talktome.ui.bubble

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.saiprasad.talktome.service.BubblePhase

internal const val BUBBLE_SIZE_DP = 56
private const val RECORDING_WIDTH_DP = 200

/**
 * The floating mic pill (Wispr Flow style). Purely presentational: the window position lives in
 * [BubbleOverlay] and dictation state in DictationController.
 */
@Composable
fun FloatingBubble(
    visible: Boolean,
    phase: BubblePhase,
    onMicTap: () -> Unit,
    onCancel: () -> Unit,
    onAccept: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (dx: Float, dy: Float) -> Unit,
    onDragEnd: () -> Unit,
) {
    val view = LocalView.current
    val isRecording = phase == BubblePhase.RECORDING

    // pointerInput(Unit) captures lambdas once; keep them fresh.
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDrag by rememberUpdatedState(onDrag)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)

    val animatedWidth by animateDpAsState(
        targetValue = if (isRecording) RECORDING_WIDTH_DP.dp else BUBBLE_SIZE_DP.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "pillWidth",
    )
    val animatedColor by animateColorAsState(
        targetValue = if (isRecording) Color(0xFFE5E7EB) else Color(0xFF2563EB), // Light Gray or Blue
        label = "pillColor",
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.7f),
        exit = fadeOut() + scaleOut(targetScale = 0.7f),
    ) {
        Box(
            modifier = Modifier
                .padding(4.dp) // room for the shadow
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { currentOnDragStart() },
                        onDragEnd = { currentOnDragEnd() },
                        onDragCancel = { currentOnDragEnd() },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            currentOnDrag(dragAmount.x, dragAmount.y)
                        },
                    )
                },
        ) {
            // The morphing pill
            Box(
                modifier = Modifier
                    .width(animatedWidth)
                    .height(BUBBLE_SIZE_DP.dp)
                    .shadow(6.dp, CircleShape)
                    .clip(CircleShape)
                    .background(animatedColor)
                    .clickable(enabled = phase == BubblePhase.IDLE) {
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        onMicTap()
                    },
                contentAlignment = Alignment.Center,
            ) {
                when (phase) {
                    BubblePhase.TRANSCRIBING -> CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 3.dp,
                    )
                    BubblePhase.IDLE -> Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Start dictation",
                        tint = Color.White,
                    )
                    BubblePhase.RECORDING -> Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Reject
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFD1D5DB)) // Darker Gray
                                .clickable {
                                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                    onCancel()
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Cancel", tint = Color.Black)
                        }

                        AnimatedWaveform()

                        // Accept
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF5A55D6)) // Wispr Purple/Blue
                                .clickable {
                                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                    onAccept()
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = "Insert text", tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedWaveform() {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (i in 0 until 5) {
            val height by infiniteTransition.animateFloat(
                initialValue = 8f,
                targetValue = 24f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 500, delayMillis = i * 100, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "barHeight",
            )

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(height.dp)
                    .clip(CircleShape)
                    .background(Color.Gray),
            )
        }
    }
}
