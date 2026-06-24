package com.digitaltwin.assistant.ai

import com.digitaltwin.assistant.ai.model.GeminiRequest
import com.digitaltwin.assistant.ai.model.GeminiResponse
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

interface GeminiApi {

    @POST("v1beta/models/gemini-1.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest,
    ): GeminiResponse

    companion object {
        const val BASE_URL = "https://generativelanguage.googleapis.com/"
    }
}
