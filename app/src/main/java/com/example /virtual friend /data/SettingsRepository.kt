package com.example.virtualfriend.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.virtualfriend.model.FriendSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import androidx.datastore.preferences.core.edit

private val Context.friendDataStore by preferencesDataStore("friend_settings")

class SettingsRepository(private val context: Context) {
    private object K {
        val enabled = booleanPreferencesKey("enabled")
        val size = floatPreferencesKey("size")
        val speed = floatPreferencesKey("speed")
        val visit = intPreferencesKey("visit")
        val waterEnabled = booleanPreferencesKey("water_enabled")
        val water = intPreferencesKey("water")
        val checkIns = booleanPreferencesKey("check_ins")
        val sound = booleanPreferencesKey("sound")
        val ai = booleanPreferencesKey("ai")
        val summon = booleanPreferencesKey("summon")
        val quietStart = intPreferencesKey("quiet_start")
        val quietEnd = intPreferencesKey("quiet_end")
        val pauseUntil = longPreferencesKey("pause_until")
        val petX = intPreferencesKey("pet_x")
        val petY = intPreferencesKey("pet_y")
        val summonX = intPreferencesKey("summon_x")
        val summonY = intPreferencesKey("summon_y")
        val onboarding = booleanPreferencesKey("onboarding")
    }

    val settings: Flow<FriendSettings> = context.friendDataStore.data.map { p ->
        FriendSettings(
            enabled = p[K.enabled] ?: true,
            friendSize = p[K.size] ?: 0.72f,
            animationSpeed = p[K.speed] ?: 1f,
            visitMinutes = p[K.visit] ?: 5,
            waterEnabled = p[K.waterEnabled] ?: true,
            waterMinutes = p[K.water] ?: 60,
            checkInsEnabled = p[K.checkIns] ?: true,
            soundEnabled = p[K.sound] ?: false,
            aiChatEnabled = p[K.ai] ?: false,
            summonButtonEnabled = p[K.summon] ?: true,
            quietStart = p[K.quietStart] ?: 23 * 60,
            quietEnd = p[K.quietEnd] ?: 7 * 60,
            pauseUntil = p[K.pauseUntil] ?: 0L,
            petX = p[K.petX] ?: 40,
            petY = p[K.petY] ?: 300,
            summonX = p[K.summonX] ?: 0,
            summonY = p[K.summonY] ?: 600
        )
    }

    suspend fun update(transform: (FriendSettings) -> FriendSettings) {
        val current = settings.first()
        val next = transform(current)
        context.friendDataStore.edit { p ->
            p[K.enabled] = next.enabled
            p[K.size] = next.friendSize
            p[K.speed] = next.animationSpeed
            p[K.visit] = next.visitMinutes
            p[K.waterEnabled] = next.waterEnabled
            p[K.water] = next.waterMinutes
            p[K.checkIns] = next.checkInsEnabled
            p[K.sound] = next.soundEnabled
            p[K.ai] = next.aiChatEnabled
            p[K.summon] = next.summonButtonEnabled
            p[K.quietStart] = next.quietStart
            p[K.quietEnd] = next.quietEnd
            p[K.pauseUntil] = next.pauseUntil
            p[K.petX] = next.petX
            p[K.petY] = next.petY
            p[K.summonX] = next.summonX
            p[K.summonY] = next.summonY
        }
    }

    suspend fun completeOnboarding() = context.friendDataStore.edit { it[K.onboarding] = true }

    val onboardingComplete: Flow<Boolean> = context.friendDataStore.data.map { it[K.onboarding] ?: false }

    suspend fun resetAll() = context.friendDataStore.edit { it.clear() }
}
