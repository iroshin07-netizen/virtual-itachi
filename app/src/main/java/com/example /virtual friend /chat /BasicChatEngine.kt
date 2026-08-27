package com.example.virtualfriend.chat

class BasicChatEngine {
    fun reply(raw: String): String {
        val text = raw.trim().lowercase()
        return when {
            text.matches(Regex("(hi|hello|hey|hii|hola).*")) -> "Hey. I'm right here. 👋"
            text.contains("how are you") -> "I'm doing fine. Tiny, alert, and keeping you company."
            text.contains("nice to meet") -> "Nice to meet you too. I think we'll get along."
            text.contains("tired") -> "Sounds like a small reset could help. Take a minute."
            text.contains("stressed") -> "One thing at a time. Breathe, then pick the next tiny step."
            text.contains("water") -> "Hydration check accepted. Go grab some water. 💧"
            text.contains("break") -> "Yep. A short break sounds reasonable."
            text.contains("what are you doing") -> "Living rent-free on your screen. Obviously."
            text.contains("sleep") -> "If you're sleepy, don't fight your body forever. Rest when you can."
            text.contains("thank") -> "Anytime. That's what tiny companions are for."
            else -> "I'm listening. Tell me a little more."
        }
    }
}
