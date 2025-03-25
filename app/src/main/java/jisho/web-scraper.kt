package jisho

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

data class JishoScraps(
    val furigana: List<String> = emptyList(),
    val text: String
)

fun conceptLightRepresentation(concept: Element): JishoScraps = JishoScraps(
    concept.select(".furigana .kanji").map { it.text() },
    concept.select(".text").text()
)

fun scraperTester(query: String, onResult: (List<JishoScraps>) -> Unit) {
    CoroutineScope(Dispatchers.IO).launch {
        val document = Jsoup.connect("$JISHO_URL/search/$query").get()
        val concepts = withContext(Dispatchers.Default) {
            document.select(".concept_light.clearfix").filter {
                it.select(".furigana .kanji, .concept_light-representation").isNotEmpty()
            }
        }
        val scrapedData = concepts.asSequence().map(::conceptLightRepresentation).toList()
        withContext(Dispatchers.Main.immediate) {
            onResult(scrapedData)
        }
    }
}