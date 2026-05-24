package com.example.data

import com.example.BuildConfig
import com.example.ui.TrialBalanceRow
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content? = null
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-1.5-pro:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val moshi = Moshi.Builder()
        .build()

    val service: GeminiApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
        retrofit.create(GeminiApiService::class.java)
    }
}

class GeminiService {
    suspend fun analyzeFinancials(incomeStatement: List<Pair<AccountEntity, Double>>, trialBalance: List<TrialBalanceRow>): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY_DEFAULT_VALUE") {
             return@withContext "تحذير: مفتاح Gemini API غير متوفر. يرجى إضافته في إعدادات الأسرار (Secrets)."
        }

        val promptBuilder = StringBuilder()
        promptBuilder.append("بصفتك خبير محاسبي ومالي، قم بتحليل البيانات المالية التالية وإعطاء تقرير مختصر وبناء عن وضعیت الشركة، والتوصيات.\n\n")
        
        promptBuilder.append("قائمة الدخل:\n")
        incomeStatement.forEach { (acc, bal) ->
            promptBuilder.append("- ${acc.nameAr}: $bal\n")
        }
        
        promptBuilder.append("\nميزان المراجعة (مختصر):\n")
        trialBalance.take(10).forEach { row ->
            promptBuilder.append("- ${row.account.nameAr}: مدين=${row.totalDebit}, دائن=${row.totalCredit}\n")
        }

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = promptBuilder.toString()))))
        )

        try {
            val response = RetrofitClient.service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "لم يتم الحصول على تحليل."
        } catch (e: Throwable) {
            "حدث خطأ أثناء إعداد التحليل: ${e.message}"
        }
    }
}
