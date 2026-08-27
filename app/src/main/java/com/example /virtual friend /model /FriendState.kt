package com.example.virtualfriend.model

enum class FriendAnimationState { IDLE, BLINK, WALK_LEFT, WALK_RIGHT, TALK, HAPPY, SLEEPY, SURPRISED, ENTER, EXIT }
enum class FriendMood { HAPPY, NORMAL, SLEEPY, EXCITED, CURIOUS, SAD, SURPRISED }

data class FriendSettings(
    val enabled: Boolean = true,
    val friendSize: Float = 0.72f,
    val animationSpeed: Float = 1f,
    val visitMinutes: Int = 5,
    val waterEnabled: Boolean = true,
    val waterMinutes: Int = 60,
    val checkInsEnabled: Boolean = true,
    val soundEnabled: Boolean = false,
    val aiChatEnabled: Boolean = false,
    val summonButtonEnabled: Boolean = true,
    val quietStart: Int = 23 * 60,
    val quietEnd: Int = 7 * 60,
    val pauseUntil: Long = 0L,
    val petX: Int = 40,
    val petY: Int = 300,
    val summonX: Int = 0,
    val summonY: Int = 600
)
