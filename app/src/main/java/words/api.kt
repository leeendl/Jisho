package words

import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

data class JishoData(
    @SerializedName("slug") val slug: String = "",
    @SerializedName("is_common") val isCommon: Boolean = false,
    @SerializedName("tags") val tags: List<String> = emptyList(),
    @SerializedName("jlpt") val jlpt: List<String> = emptyList(),
    @SerializedName("japanese") val japanese: List<JishoJapanese> = emptyList(),
    @SerializedName("senses") val senses: List<JishoSenses> = emptyList()
)

/**
 * "japanese":
 **/
data class JishoJapanese(
    @SerializedName("word") val word: String? = null, // for kana-cases
    @SerializedName("reading") val reading: String = ""
)

/**
 * "senses":
 **/
data class JishoSenses(
    @SerializedName("english_definitions") val englishDefinitions: List<String> = emptyList(),
    @SerializedName("parts_of_speech") val partsOfSpeech: List<String> = emptyList(),
    // @todo links
    @SerializedName("tags") val tags: List<String> = emptyList(),
    // @todo restrictions
    @SerializedName("see_also") val seeAlso: List<String> = emptyList(),
    // @todo antonyms
    // @todo source
    @SerializedName("info") val info: List<String> = emptyList()
)

data class JishoSearch(
    @SerializedName("data") val data: List<JishoData> = emptyList()
)

interface JishoApi {
    @GET("api/v1/search/words")
    suspend fun keyword(
        @Query("keyword") keyword: String
    ): JishoSearch
}

fun search(
    query: String,
    onSuccess: (JishoSearch) -> Unit,
    onFailure: (String) -> Unit = {}
) {
    val retrofit = Retrofit.Builder()
        .baseUrl("https://jisho.org/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    val jishoApi = retrofit.create(JishoApi::class.java)

    CoroutineScope(Dispatchers.IO).launch {
        runCatching {
            jishoApi.keyword(query)
        }.onSuccess { response ->
            withContext(Dispatchers.Main) { onSuccess(response) }
        }.onFailure { e ->
            withContext(Dispatchers.Main) { onFailure(e.message ?: "Unknown error") }
        }
    }
}
