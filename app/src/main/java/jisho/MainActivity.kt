package jisho

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

class MainActivity : ComponentActivity() {
    private val natsumeMoji = FontFamily(
        Font(R.font.natumemozi)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        getActionBar()?.hide()
        val searchModel: SearchModel by viewModels()
        setContent {
            val results by searchModel.results.collectAsState(emptyList())
            val listState = rememberLazyListState()
            MaterialTheme(
                darkColorScheme()
            ) {
                Surface {
                    LazyColumn(
                        Modifier
                            .fillMaxSize()
                            .systemBarsPadding(),
                        listState
                    ) {
                        item {
                            SearchBar(searchModel, listState)
                        }
                        items(results) {
                            ItemColumn(it)
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun SearchBar(
        searchModel: SearchModel,
        listState: LazyListState
    ) {
        val searchState by searchModel.searchState.collectAsState(TextFieldValue())
        TextField(
            value = searchState,
            onValueChange = { searchModel.search(it.text, listState) },
            modifier = Modifier
                .fillMaxWidth()
                .border(3.dp, Color(0xFF6E6E6E)),
            placeholder = { Text("Search") },
            leadingIcon = {
                // @todo add keyword filter
            },
            trailingIcon = {
                if (searchState.text.isNotEmpty()) {
                    IconButton(
                        onClick = { searchModel.resetField() }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_clear),
                            contentDescription = "Clear"
                        )
                    }
                }
            },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                unfocusedIndicatorColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent
            )
        )
    }

    @Composable
    fun ItemColumn(
        jishoScraps: JishoScraps
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (jishoScraps.furigana.isNotEmpty()) {
                Text(
                    text = jishoScraps.furigana.joinToString(" "),
                    fontFamily = natsumeMoji
                )
            }
            Text(
                text = jishoScraps.text,
                fontSize = 36.sp,
                fontFamily = natsumeMoji
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(4.dp)
        ) {
            if (jishoScraps.isCommon) {
                categoryTag("common word", Color(0xFF8abc83))
            }
            jishoScraps.jlpt?.let {
                categoryTag(it, Color(0xFF909dc0))
            }
            jishoScraps.wanikaniLevel?.let {
                categoryTag(it, Color(0xFF909dc0))
            }
        }

        for ((index, meaning) in jishoScraps.meanings.withIndex()) {
            val annotatedString = remember(meaning) {
                buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            color = Color(0xFFAAAAAA),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append("${meaning.partOfSpeech}\n")
                    }
                    withStyle(SpanStyle(color = Color(0xFF999999), fontSize = 18.sp)) {
                        append("${index + 1}.  ")
                    }
                    append(meaning.definition)
                }
            }
            Text(
                text = annotatedString,
                modifier = Modifier
                    .padding(10.dp),
                fontSize = 19.sp
            )
        }
    }

    @Composable
    private fun categoryTag(
        label: String,
        backgroundColor: Color
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = backgroundColor,
                    shape = RoundedCornerShape(2.dp)
                )
                .padding(start = 6.dp, end = 4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    color = Color(0xFF222222),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp,
                    fontSize = 12.sp
                )
            )
        }
    }

    class SearchModel : ViewModel() {
        private val _searchState = MutableStateFlow(TextFieldValue())
        val searchState: StateFlow<TextFieldValue> = _searchState

        private val _results = MutableStateFlow<List<JishoScraps>>(emptyList())
        val results: StateFlow<List<JishoScraps>> = _results

        private var job: Job? = null

        fun search(query: String, listState: LazyListState) {
            _searchState.update { it.copy(text = query, selection = TextRange(query.length)) }
            job?.cancel()

            if (query.isBlank()) {
                _results.update { emptyList() }
                return
            }

            job = viewModelScope.launch {
                runCatching {
                    val thisQuery = _searchState.value.text

                    scraperTester(thisQuery) { scraps ->
                        if (thisQuery != _searchState.value.text) throw CancellationException()
                        _results.update { scraps }
                    }

                    listState.scrollToItem(0)
                }.onFailure {
                    if (it is CancellationException) _results.update { emptyList() }
                }
            }
        }

        fun resetField() {
            _searchState.update { TextFieldValue() }
        }
    }
}