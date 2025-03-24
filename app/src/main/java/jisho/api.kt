package jisho

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

const val JISHO_URL = "https://jisho.org"
private const val JISHO_API = "api/v1/search/words"

@Serializable
data class JishoData(
    @SerialName("slug") val slug: String = "",
    @SerialName("is_common") val isCommon: Boolean = false,
    @SerialName("tags") val tags: List<String> = emptyList(),
    @SerialName("jlpt") val jlpt: List<String> = emptyList(),
    @SerialName("japanese") val japanese: List<JishoJapanese> = emptyList(),
    @SerialName("senses") val senses: List<JishoSenses> = emptyList()
)

@Serializable
data class JishoJapanese(
    @SerialName("word") val word: String? = null, // for kana-cases
    @SerialName("reading") val reading: String = ""
)

@Serializable
data class JishoSenses(
    @SerialName("english_definitions") val englishDefinitions: List<String> = emptyList(),
    @SerialName("parts_of_speech") val partsOfSpeech: List<String> = emptyList(),
    // @todo links
    @SerialName("tags") val tags: List<String> = emptyList(),
    // @todo restrictions
    @SerialName("see_also") val seeAlso: List<String> = emptyList(),
    // @todo antonyms
    // @todo source
    @SerialName("info") val info: List<String> = emptyList()
)

@Serializable
data class JishoSearch(
    @SerialName("data") val data: List<JishoData> = emptyList()
)

interface JishoApi {
    @GET(JISHO_API)
    suspend fun keyword(
        @Query("keyword") keyword: String,
        @Query("page") page: Int
    ): JishoSearch
}

object JishoClient {
    private val json = Json {
        ignoreUnknownKeys = true
    }
    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(JISHO_URL)
            .client(
                OkHttpClient.Builder()
                    .readTimeout(24000L, TimeUnit.MILLISECONDS)
                    .build()
            )
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }
    val jishoApi: JishoApi by lazy { retrofit.create(JishoApi::class.java) }
}

suspend fun search(
    keyword: String,
    page: Int,
    onSuccess: (JishoSearch) -> Unit,
    onFailure: (String) -> Unit = {}
) = withContext(Dispatchers.IO) {
    runCatching { JishoClient.jishoApi.keyword(keyword, page) }
        .onSuccess { onSuccess(it) }
        .onFailure { onFailure(it.message ?: "Unknown error") }
}