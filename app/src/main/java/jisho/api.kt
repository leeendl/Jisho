package jisho

import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

data class JishoData(
    @Json(name = "slug") val slug: String = "",
    @Json(name = "is_common") val isCommon: Boolean = false,
    @Json(name = "tags") val tags: List<String> = emptyList(),
    @Json(name = "jlpt") val jlpt: List<String> = emptyList(),
    @Json(name = "japanese") val japanese: List<JishoJapanese> = emptyList(),
    @Json(name = "senses") val senses: List<JishoSenses> = emptyList()
)

/**
 * "japanese":
 **/
data class JishoJapanese(
    @Json(name = "word") val word: String? = null, // for kana-cases
    @Json(name = "reading") val reading: String = ""
)

/**
 * "senses":
 **/
data class JishoSenses(
    @Json(name = "english_definitions") val englishDefinitions: List<String> = emptyList(),
    @Json(name = "parts_of_speech") val partsOfSpeech: List<String> = emptyList(),
    // @todo links
    @Json(name = "tags") val tags: List<String> = emptyList(),
    // @todo restrictions
    @Json(name = "see_also") val seeAlso: List<String> = emptyList(),
    // @todo antonyms
    // @todo source
    @Json(name = "info") val info: List<String> = emptyList()
)

data class JishoSearch(
    @Json(name = "data") val data: List<JishoData> = emptyList()
)

interface JishoApi {
    @GET("api/v1/search/words")
    suspend fun keyword(
        @Query("keyword") keyword: String,
        @Query("page") page: Int
    ): JishoSearch
}

object JishoClient {
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .readTimeout(24000, TimeUnit.MILLISECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://jisho.org/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val jishoApi: JishoApi by lazy { retrofit.create(JishoApi::class.java) }
}

suspend fun search(
    keyword: String,
    page: Int,
    onSuccess: (JishoSearch) -> Unit,
    onFailure: (String) -> Unit = {}
) {
    withContext(Dispatchers.IO) {
        runCatching {
            JishoClient.jishoApi.keyword(keyword, page)
        }.onSuccess { response ->
            onSuccess(response)
        }.onFailure { e ->
            onFailure(e.message ?: "Unknown error")
        }
    }
}