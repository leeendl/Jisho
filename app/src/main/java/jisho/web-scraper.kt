package jisho

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

const val JISHO_URL = "https://jisho.org"

data class JishoScraps(
    val furigana: List<String> = emptyList(),
    val text: String,
    val meanings: List<Meaning> = emptyList(),
    val isCommon: Boolean = false,
    val jlpt: String? = null,
    val wanikaniLevel: String? = null
)

data class Meaning(
    val definition: String,
    val partOfSpeech: String
)

fun Element.conceptLightRepresentation(): JishoScraps {
    val statusTags = select(".concept_light-status .concept_light-tag")

    return JishoScraps(
        furigana = select(".furigana .kanji").eachText(),
        text = selectFirst(".text")?.text().orEmpty(),
        meanings = select(".meanings-wrapper").firstOrNull()?.let { wrapper ->
            buildList {
                var currentPartOfSpeech = ""
                wrapper.children().forEach { child ->
                    when {
                        child.hasClass("meaning-tags") -> currentPartOfSpeech = child.text().trim()
                        child.hasClass("meaning-wrapper") -> child.selectFirst(".meaning-definition")?.let { def ->
                            val meaning = def.selectFirst(".meaning-meaning")?.text()?.trim().orEmpty()
                            if (meaning.isNotEmpty()) {
                                add(Meaning(meaning, currentPartOfSpeech))
                            }
                        }
                    }
                }
            }
        }.orEmpty(),
        isCommon = statusTags.any { it.hasClass("concept_light-common") },
        jlpt = statusTags.firstOrNull { "JLPT" in it.text() }?.text()?.trim(),
        wanikaniLevel = statusTags.firstOrNull { "Wanikani" in it.text() }?.text()?.trim()
    )
}

suspend fun scrapeJisho(query: String): List<JishoScraps> = withContext(Dispatchers.IO) {
    val document = Jsoup.connect("$JISHO_URL/search/$query").get()
    document.select(".concept_light.clearfix")
        .filter { it.select(".concept_light-representation").isNotEmpty() }
        .map { it.conceptLightRepresentation() }
}

fun scraperTester(query: String, onResult: (List<JishoScraps>) -> Unit) {
    CoroutineScope(Dispatchers.IO).launch {
        val result = scrapeJisho(query)
        withContext(Dispatchers.Main.immediate) {
            onResult(result)
        }
    }
}