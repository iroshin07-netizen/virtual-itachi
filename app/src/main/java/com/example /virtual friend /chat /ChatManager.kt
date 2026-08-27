package com.example.virtualfriend.chat

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ChatManager(
    private val basic: BasicChatEngine = BasicChatEngine(),
    private val ai: AIChatRepository = BackendAIChatRepository()
) {
    suspend fun reply(message: String, aiEnabled: Boolean, history: List<Pair<String, String>>): String =
        withContext(Dispatchers.Default) {
            if (aiEnabled) ai.sendMessage(message, history).getOrElse { basic.reply(message) }
            else basic.reply(message)
        }
}
