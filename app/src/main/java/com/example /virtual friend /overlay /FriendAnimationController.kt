package com.example.virtualfriend.overlay

import com.example.virtualfriend.model.FriendAnimationState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

class FriendAnimationController(
    private val scope: CoroutineScope,
    private val speedProvider: () -> Float,
    private val onState: (FriendAnimationState) -> Unit,
    private val onMessage: (String?) -> Unit,
    private val onMove: (dx: Int) -> Unit,
    private val canMove: () -> Boolean
) {
    private var job: Job? = null

    fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            onState(FriendAnimationState.IDLE)
            while (isActive) {
                delay((2500L / speedProvider().coerceIn(0.5f, 2f)).toLong())
                when (Random.nextInt(100)) {
                    in 0..58 -> { onState(FriendAnimationState.IDLE); onMessage(null) }
                    in 59..68 -> { onState(FriendAnimationState.BLINK); delay(220); onState(FriendAnimationState.IDLE) }
                    in 69..78 -> { onState(if (Random.nextBoolean()) FriendAnimationState.WALK_LEFT else FriendAnimationState.WALK_RIGHT); wander() }
                    in 79..88 -> { onState(FriendAnimationState.HAPPY); onMessage("Hi. Just checking in."); delay(2500); onMessage(null) }
                    in 89..95 -> { onState(FriendAnimationState.SLEEPY); onMessage("Need a little break?"); delay(2600); onMessage(null) }
                    else -> { onState(FriendAnimationState.SURPRISED); delay(700); onState(FriendAnimationState.IDLE) }
                }
            }
        }
    }

    private suspend fun wander() {
        val dir = if (Random.nextBoolean()) 1 else -1
        repeat(Random.nextInt(10, 26)) {
            if (!canMove()) return
            onMove(dir * 5)
            delay((55L / speedProvider().coerceIn(0.5f, 2f)).toLong())
        }
        onState(FriendAnimationState.IDLE)
    }

    fun summon() {
        scope.launch {
            onState(FriendAnimationState.ENTER)
            delay(500)
            onState(FriendAnimationState.HAPPY)
            onMessage("I'm here. 👋")
            delay(2500)
            onMessage(null)
            onState(FriendAnimationState.IDLE)
        }
    }

    fun speak(message: String) {
        scope.launch {
            onState(FriendAnimationState.TALK)
            onMessage(message)
            delay(3500)
            onMessage(null)
            onState(FriendAnimationState.IDLE)
        }
    }

    fun exit() {
        scope.launch {
            onState(FriendAnimationState.EXIT)
            delay(550)
            onState(FriendAnimationState.IDLE)
        }
    }

    fun stop() { job?.cancel(); job = null }
}
