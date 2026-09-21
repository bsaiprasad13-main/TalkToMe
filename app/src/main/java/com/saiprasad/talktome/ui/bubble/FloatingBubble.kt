package com.saiprasad.talktome.ui.bubble

import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.saiprasad.talktome.audio.TalkToMeAudioRecorder
import com.saiprasad.talktome.service.FocusEventBus
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.roundToInt

@Composable
fun FloatingBubble(
    audioRecorder: TalkToMeAudioRecorder,
    windowManager: WindowManager,
    layoutParams: WindowManager.LayoutParams,
    composeView: ComposeView,
    onAudioRecorded: suspend (File?) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var isRecording by remember { mutableStateOf(false) }
    var isTranslating by remember { mutableStateOf(false) }
    val isVisible by FocusEventBus.isEditableFocused.collectAsState()
    
    val screenWidth = LocalContext.current.resources.displayMetrics.widthPixels
    
    var offsetX by remember { mutableFloatStateOf(layoutParams.x.toFloat()) }
    var offsetY by remember { mutableFloatStateOf(layoutParams.y.toFloat()) }
    
    val isOnRightSide by derivedStateOf { offsetX > (screenWidth / 2) }
    var rowWidthPx by remember { mutableIntStateOf(0) }
    var isDragging by remember { mutableStateOf(false) }

    LaunchedEffect(rowWidthPx) {
        if (isOnRightSide && !isDragging) {
            offsetX = (screenWidth - rowWidthPx).toFloat()
            layoutParams.x = offsetX.roundToInt()
            windowManager.updateViewLayout(composeView, layoutParams)
        }
    }

    // Dynamic width for the pill
    val targetWidth = if (isRecording) 220.dp else 60.dp
    val animatedWidth by animateDpAsState(
        targetValue = targetWidth,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "pillWidth"
    )

    // Dynamic background color
    val targetColor = if (isRecording) Color(0xFFE5E7EB) else Color(0xFF2563EB) // Light Gray or Blue
    val animatedColor by animateColorAsState(targetValue = targetColor, label = "pillColor")

    AnimatedVisibility(
        visible = isVisible || isRecording || isTranslating,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .wrapContentSize()
                .onSizeChanged { rowWidthPx = it.width }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { isDragging = true },
                        onDragEnd = {
                            isDragging = false
                            val targetX = if (offsetX > screenWidth / 2) (screenWidth - rowWidthPx).toFloat() else 0f
                            coroutineScope.launch {
                                Animatable(offsetX).animateTo(
                                    targetValue = targetX,
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                ) {
                                    offsetX = value
                                    layoutParams.x = offsetX.roundToInt()
                                    windowManager.updateViewLayout(composeView, layoutParams)
                                }
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            offsetX += dragAmount.x
                            offsetY += dragAmount.y
                            
                            layoutParams.x = offsetX.roundToInt()
                            layoutParams.y = offsetY.roundToInt()
                            windowManager.updateViewLayout(composeView, layoutParams)
                        }
                    )
                }
        ) {
            // The Morphing Pill
            Box(
                modifier = Modifier
                    .width(animatedWidth)
                    .height(60.dp)
                    .clip(CircleShape)
                    .background(animatedColor)
                    .clickable(enabled = !isRecording && !isTranslating) {
                        // Start recording
                        isRecording = true
                        audioRecorder.startRecording { file ->
                            isRecording = false
                            isTranslating = true
                            coroutineScope.launch {
                                onAudioRecorded(file)
                                isTranslating = false
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isTranslating) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(30.dp),
                        strokeWidth = 3.dp
                    )
                } else if (!isRecording) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Mic",
                        tint = Color.White
                    )
                } else {
                    // Recording State UI (Wispr Flow style)
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Reject Button
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFD1D5DB)) // Darker Gray
                                .clickable {
                                    val file = audioRecorder.stopRecording()
                                    file?.delete()
                                    isRecording = false
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Cancel", tint = Color.Black)
                        }
                        
                        // Waveform Animation (Simulated)
                        AnimatedWaveform()
                        
                        // Accept Button
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF5A55D6)) // Wispr Purple/Blue
                                .clickable {
                                    val file = audioRecorder.stopRecording()
                                    isRecording = false
                                    isTranslating = true
                                    coroutineScope.launch {
                                        onAudioRecorded(file)
                                        isTranslating = false
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = "Accept", tint = Color.White)
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
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until 5) {
            val height by infiniteTransition.animateFloat(
                initialValue = 8f,
                targetValue = 24f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 500, delayMillis = i * 100, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "barHeight"
            )
            
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(height.dp)
                    .clip(CircleShape)
                    .background(Color.Gray)
            )
        }
    }
}
