package com.example.virtualfriend.chat

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

interface AIChatRepository {
    suspend fun sendMessage(message: String, conversation: List<Pair<String, String>>): Result<String>
}

class BackendAIChatRepository(private val context: Context) : AIChatRepository {
    
    override suspend fun sendMessage(message: String, conversation: List<Pair<String, String>>): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                // Get the key and trim any accidental spaces
                val sharedPrefs = context.getSharedPreferences("VirtualFriendPrefs", Context.MODE_PRIVATE)
                val rawKey = sharedPrefs.getString("GEMINI_API_KEY", "")?.trim() ?: ""
                
                if (rawKey.isBlank()) {
                    return@withContext Result.failure(Exception("API Key is missing! Please paste it in settings."))
                }

                // URL-Encode the key to protect special characters like '.' and '_'
                val encodedKey = URLEncoder.encode(rawKey, "UTF-8")
                
                // Using the ultra-stable gemini-pro model
                val urlString = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent?key=$encodedKey"
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                // Itachi personality prompt
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

                // Send request
                val writer = OutputStreamWriter(connection.outputStream)
                writer.write(payload.toString())
                writer.flush()
                writer.close()

                // Check Response
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
                    // Extract the exact error message from Google's server
                    val errorStream = connection.errorStream
                    val errorResponse = errorStream?.bufferedReader()?.use { it.readText() } ?: "No details"
                    var cleanErrorMsg = errorResponse
                    try {
                        val errJson = JSONObject(errorResponse)
                        cleanErrorMsg = errJson.getJSONObject("error").getString("message")
                    } catch (e: Exception) {}
                    
                    Result.failure(Exception("Google API Error: $cleanErrorMsg"))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Result.failure(Exception("Network Error: ${e.message}"))
            }
        }
    }
}
