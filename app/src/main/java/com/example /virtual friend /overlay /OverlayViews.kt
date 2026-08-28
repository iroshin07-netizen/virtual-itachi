package com.example.virtualfriend.overlay

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.virtualfriend.R
import com.example.virtualfriend.chat.ChatManager
import com.example.virtualfriend.model.FriendAnimationState
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun PetOverlayContent(
    state: FriendAnimationState,
    sizeScale: Float,
    message: String?,
    bubbleOnLeft: Boolean,
    onDrag: (Float, Float) -> Unit,
    onClick: () -> Unit
) {
    val infinite = rememberInfiniteTransition(label = "pet")
    val bob by infinite.animateFloat(
        initialValue = -3f, targetValue = 3f,
        animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bob"
    )
    val breath by infinite.animateFloat(
        initialValue = 0.985f, targetValue = 1.015f,
        animationSpec = infiniteRepeatable(tween(1700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breath"
    )
    val enterExit by animateFloatAsState(
        targetValue = if (state == FriendAnimationState.ENTER) 0f else if (state == FriendAnimationState.EXIT) 0f else 1f,
        animationSpec = tween(450, easing = FastOutSlowInEasing), label = "enter"
    )
    val rotation by animateFloatAsState(
        targetValue = when (state) {
            FriendAnimationState.WALK_LEFT -> -3f
            FriendAnimationState.WALK_RIGHT -> 3f
            FriendAnimationState.SURPRISED -> 2f
            else -> 0f
        }, animationSpec = tween(220), label = "rotation"
    )
    val squash by animateFloatAsState(
        targetValue = when (state) {
            FriendAnimationState.HAPPY -> 1.035f
            FriendAnimationState.SLEEPY -> 0.97f
            else -> 1f
        }, animationSpec = tween(260), label = "squash"
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onClick() })
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount.x, dragAmount.y)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(if (state == FriendAnimationState.WALK_LEFT || state == FriendAnimationState.WALK_RIGHT) R.drawable.itachi_side else R.drawable.itachi_front),
            contentDescription = "Itachi virtual friend",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .align(if (bubbleOnLeft) Alignment.CenterEnd else Alignment.Center)
                .offset { IntOffset(0, bob.roundToInt()) }
                .scale(
                    scaleX = (if (state == FriendAnimationState.WALK_RIGHT) -1f else 1f) * sizeScale * breath * squash,
                    scaleY = sizeScale * breath * squash
                )
                .rotate(rotation)
                .alpha(enterExit)
        )
        if (!message.isNullOrBlank()) {
            Surface(
                color = Color.White,
                contentColor = Color(0xFF17151B),
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 7.dp,
                modifier = Modifier
                    .widthIn(max = 190.dp)
                    .padding(8.dp)
                    .align(if (bubbleOnLeft) Alignment.CenterStart else Alignment.TopCenter)
                    .offset { if (bubbleOnLeft) IntOffset(4, -70) else IntOffset(0, 4) }
            ) {
                Text(message, modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun SummonButtonContent(onDrag: (Float, Float) -> Unit, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onClick() })
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDrag = { change, dragAmount -> 
                        change.consume()
                        onDrag(dragAmount.x, dragAmount.y) 
                    }
                )
            }, 
        contentAlignment = Alignment.Center
    ) {
        Surface(shape = RoundedCornerShape(50), tonalElevation = 6.dp, shadowElevation = 8.dp) {
            Image(
                painter = painterResource(R.drawable.itachi_front), 
                contentDescription = "Summon Itachi", 
                modifier = Modifier.size(58.dp).padding(4.dp), 
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
fun ChatPanelContent(
    messages: List<Pair<Boolean, String>>,
    onSend: suspend (String) -> Unit,
    onClose: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf(TextFieldValue()) }
    Surface(shape = RoundedCornerShape(22.dp), tonalElevation = 8.dp, shadowElevation = 16.dp) {
        Column(Modifier.fillMaxSize().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.itachi_front), 
                    contentDescription = "Itachi", 
                    modifier = Modifier.size(44.dp), 
                    contentScale = ContentScale.Fit
                )
                Spacer(Modifier.width(8.dp))
                Text("Itachi", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onClose) { Text("Close") }
            }
            HorizontalDivider()
            Column(Modifier.weight(1f).fillMaxWidth().padding(vertical = 8.dp)) {
                messages.forEach { (mine, text) ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
                        Surface(shape = RoundedCornerShape(14.dp), color = if (mine) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant) {
                            Text(text, Modifier.padding(9.dp).widthIn(max = 230.dp))
                        }
                    }
                    Spacer(Modifier.height(5.dp))
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextField(input, { input = it }, Modifier.weight(1f), placeholder = { Text("Say something…") }, singleLine = true)
                Spacer(Modifier.width(6.dp))
                Button(onClick = {
                    val text = input.text.trim()
                    if (text.isNotEmpty()) { input = TextFieldValue(); scope.launch { onSend(text) } }
                }) { Text("Send") }
            }
        }
    }
}
