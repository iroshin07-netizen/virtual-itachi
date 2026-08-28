package com.example.virtualfriend.chat

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

interface AIChatRepository {
    suspend fun sendMessage(message: String, conversation: List<Pair<String, String>>): Result<String>
}

class BackendAIChatRepository(private val context: Context) : AIChatRepository {
    
    override suspend fun sendMessage(message: String, conversation: List<Pair<String, String>>): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                // App ki internal memory se API key nikalna
                val sharedPrefs = context.getSharedPreferences("VirtualFriendPrefs", Context.MODE_PRIVATE)
                val apiKey = sharedPrefs.getString("GEMINI_API_KEY", "") ?: ""
                
                if (apiKey.isBlank()) {
                    return@withContext Result.failure(Exception("API Key missing! Please enter it in the app settings."))
                }

                // Direct connection to Gemini API
                val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                // Set Itachi's personality and the user's message
                val prompt = "Act as Itachi Uchiha from Naruto. Reply in 1 or 2 short sentences with his calm, wise, and slightly cold personality. User says: $message"
                
                val payload = JSONObject()
                val contentsArray = org.json.JSONArray()
                
                val userPart = JSONObject().apply { put("text", prompt) }
                val partsArray = org.json.JSONArray().apply { put(userPart) }
                
                val contentObj = JSONObject().apply { 
                    put("role", "user")
                    put("parts", partsArray)
                }
                
                contentsArray.put(contentObj)
                payload.put("contents", contentsArray)

                // Send the payload
                val writer = OutputStreamWriter(connection.outputStream)
                writer.write(payload.toString())
                writer.flush()
                writer.close()

                // Read and parse the response
                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val responseStr = connection.inputStream.bufferedReader().use { it.readText() }
                    val jsonResponse = JSONObject(responseStr)
                    val replyText = jsonResponse.getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text")
                    
                    Result.success(replyText)
                } else {
                    Result.failure(Exception("API Error: $responseCode"))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }
}
