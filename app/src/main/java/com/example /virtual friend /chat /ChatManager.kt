package com.example.virtualfriend.chat

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChatManager(context: Context) {
    private val basic: BasicChatEngine = BasicChatEngine()
    private val ai: AIChatRepository = BackendAIChatRepository(context)

    suspend fun reply(message: String, aiEnabled: Boolean, history: List<Pair<String, String>>): String {
        return withContext(Dispatchers.Default) {
            if (aiEnabled) {
                // Agar API fail hui, toh Itachi screen par ERROR reason dega, purana reply nahi.
                ai.sendMessage(message, history).getOrElse { error ->
                    "System Error: ${error.message ?: "Something went wrong. Check Internet/Key."}"
                }
            } else {
                basic.reply(message)
            }
        }
    }
}
