package com.example.virtualfriend.chat

/**
 * Secure AI boundary. The Android app never stores a production AI provider key.
 * Replace this implementation with your HTTPS backend client later.
 */
interface AIChatRepository {
    suspend fun sendMessage(message: String, conversation: List<Pair<String, String>>): Result<String>
}

class BackendAIChatRepository : AIChatRepository {
    override suspend fun sendMessage(message: String, conversation: List<Pair<String, String>>): Result<String> =
        Result.failure(UnsupportedOperationException("Connect this repository to your secure backend."))
}
