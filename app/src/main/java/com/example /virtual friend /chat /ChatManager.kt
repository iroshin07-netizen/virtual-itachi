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
                ai.sendMessage(message, history).getOrElse { basic.reply(message) }
            } else {
                basic.reply(message)
            }
        }
    }
}
