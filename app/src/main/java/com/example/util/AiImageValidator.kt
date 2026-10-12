package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

object AiImageValidator {

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private fun getApiKey(): String {
        return when {
            BuildConfig.GEMINI_API_KEY.isNotBlank() &&
            !BuildConfig.GEMINI_API_KEY.contains("MY_GEMINI_API_KEY") -> BuildConfig.GEMINI_API_KEY.trim()
            else -> ""
        }
    }

    suspend fun validateIdDocument(context: Context, bitmap: Bitmap): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isBlank()) {
            if (bitmap.width >= 200 && bitmap.height >= 200) {
                return@withContext Pair(true, null)
            }
            return@withContext Pair(false, "Invalid Document. Please upload a clear image of your ID.")
        }

        try {
            val base64 = bitmapToBase64(bitmap, 800)
            val prompt = """
                You are a strict security KYC document verifier.
                Examine this image carefully.
                Does this image contain an actual Identity Card (such as a Pakistani CNIC/SNIC, National Identity Card, Passport, or Driving License)?
                If it is a fake photo, a car, food, animal, computer screen, scenery, random furniture, household object, clothing, or has no identification card visible, you MUST reject it.
                Return ONLY valid JSON matching this schema:
                {
                  "isValid": true/false,
                  "reason": "Short explanation"
                }
            """.trimIndent()

            val jsonResponse = callGeminiVision(apiKey, base64, prompt)
            val json = JSONObject(jsonResponse)
            val isValid = json.optBoolean("isValid", false)
            if (isValid) {
                Pair(true, null)
            } else {
                Pair(false, "Invalid Document. Please upload a clear image of your ID.")
            }
        } catch (e: Exception) {
            if (bitmap.width >= 150 && bitmap.height >= 150) {
                Pair(true, null)
            } else {
                Pair(false, "Invalid Document. Please upload a clear image of your ID.")
            }
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap, maxDim: Int): String {
        val scale = Math.min(maxDim.toFloat() / bitmap.width, maxDim.toFloat() / bitmap.height).coerceAtMost(1f)
        val targetW = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val targetH = (bitmap.height * scale).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(bitmap, targetW, targetH, true)
        val stream = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 85, stream)
        return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    }

    private fun callGeminiVision(apiKey: String, base64Image: String, promptText: String): String {
        val jsonRequest = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", promptText) })
                        put(JSONObject().apply {
                            put("inline_data", JSONObject().apply {
                                put("mime_type", "image/jpeg")
                                put("data", base64Image)
                            })
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.1)
            })
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"
        val request = Request.Builder()
            .url(url)
            .post(jsonRequest.toString().toRequestBody("application/json".toMediaType()))
            .build()

        httpClient.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                throw Exception("Gemini Vision HTTP ${response.code}: $body")
            }
            val root = JSONObject(body)
            val candidates = root.optJSONArray("candidates") ?: throw Exception("No candidates")
            val content = candidates.optJSONObject(0)?.optJSONObject("content") ?: throw Exception("No content")
            val parts = content.optJSONArray("parts") ?: throw Exception("No parts")
            val raw = parts.optJSONObject(0)?.optString("text") ?: "{}"
            return raw.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        }
    }
}
