package com.example.util

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object RateListFileParser {
    suspend fun extractTextFromFile(context: Context, uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        try {
            val res = RateListDetector.detectRatesFromFile(context, uri)
            if (res.rawText.isNotBlank()) {
                Result.success(res.rawText)
            } else {
                Result.failure(Exception(res.error ?: "No legible text found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
