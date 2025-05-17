package com.example.wordlearn.data.api

import com.example.wordlearn.data.model.ChatRequest
import com.example.wordlearn.data.model.ChatResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface ChatApi {
    @POST("api/v1/services/aigc/text-generation/generation")
    suspend fun chat(
        @Header("Authorization") auth: String,
        @Body request: ChatRequest
    ): ChatResponse
}

object ChatService {
    private const val BASE_URL = "https://dashscope.aliyuncs.com/"
    private const val API_KEY = "sk-5d7fdd9007dd45a58da86c26463d81fe" //

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    public fun  getApiKey():String {
        return API_KEY
    }
    val api: ChatApi = retrofit.create(ChatApi::class.java)
} 