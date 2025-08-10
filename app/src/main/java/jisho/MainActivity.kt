package jisho

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
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
                            ItemColumn(it, searchModel, listState)
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
        jishoData: JishoData,
        searchModel: SearchModel,
        listState: LazyListState
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val japanese = jishoData.japanese.firstOrNull()
            Text(
                text = japanese?.let { if (it.word != null) it.reading else "" }.orEmpty(),
                fontFamily = natsumeMoji
            )
            Text(
                text = japanese?.let { it.word ?: it.reading }.orEmpty(),
                fontSize = 36.sp,
                fontFamily = natsumeMoji
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(4.dp)
        ) {
            if (jishoData.isCommon) {
                categoryTag("common word", Color(0xFF8abc83))
            }
            if (jishoData.jlpt.isNotEmpty()) {
                categoryTag(
                    jishoData.jlpt.firstOrNull().orEmpty().replace("-", " "),
                    Color(0xFF909dc0)
                )
            }
            if (jishoData.tags.isNotEmpty()) {
                categoryTag(
                    "wanikani level ${jishoData.tags.firstOrNull()?.lastOrNull().toString()}",
                    Color(0xFF909dc0)
                )
            }
        }
        for ((index, sense) in jishoData.senses.withIndex()) {
            val annotatedString = remember(sense, index) {
                buildAnnotatedString {
                    withStyle(
                        SpanStyle(
                            color = Color(0xFFAAAAAA),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append("${sense.partsOfSpeech.joinToString(", ") { it }}\n")
                    }
                    withStyle(SpanStyle(color = Color(0xFF999999), fontSize = 18.sp)) {
                        append("${index + 1}.  ")
                    }
                    sense.englishDefinitions.forEachIndexed { index, definition ->
                        append(definition)
                        if (index != sense.englishDefinitions.lastIndex) append("; ")
                    }
                    withStyle(SpanStyle(color = Color(0xFFBBBBBB), fontSize = 16.sp)) {
                        if (sense.seeAlso.isNotEmpty()) {
                            append("  See also ")
                            pushStringAnnotation(
                                tag = "clickSearch",
                                annotation = sense.seeAlso.firstOrNull().orEmpty()
                            )
                            withStyle(SpanStyle(textDecoration = TextDecoration.Underline, fontFamily = natsumeMoji)) {
                                append(sense.seeAlso.firstOrNull().orEmpty())
                            }
                            pop()
                        }
                        if (sense.info.isNotEmpty()) {
                            if (sense.seeAlso.isNotEmpty()) append(',')
                            append("  ${sense.info.firstOrNull().orEmpty()}")
                        }
                    }
                }
            }
            var textLayoutResult: TextLayoutResult? = null
            Text(
                text = annotatedString,
                modifier = Modifier
                    .pointerInput(Unit) {
                        tapGesture(searchModel, annotatedString, textLayoutResult, listState)
                    }
                    .padding(10.dp),
                onTextLayout = { textLayoutResult = it },
                style = TextStyle(fontSize = 20.sp)
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

    private suspend fun PointerInputScope.tapGesture(
        searchModel: SearchModel,
        annotatedString: AnnotatedString,
        textLayoutResult: TextLayoutResult?,
        listState: LazyListState
    ) {
        textLayoutResult ?: return
        this.detectTapGestures { offset ->
            val offsetPosition = textLayoutResult.getOffsetForPosition(offset)
            annotatedString.getStringAnnotations(
                "clickSearch",
                offsetPosition,
                offsetPosition
            ).firstOrNull()?.let {
                searchModel.apply {
                    search(it.item, listState)
                }
            }
        }
    }

    class SearchModel : ViewModel() {
        private val _searchState = MutableStateFlow(TextFieldValue())
        val searchState: StateFlow<TextFieldValue> = _searchState

        private val _results = MutableStateFlow<List<JishoData>>(emptyList())
        val results: StateFlow<List<JishoData>> = _results

        private var job: Job? = null
        fun search(query: String, listState: LazyListState, page: Int = 1) {
            _searchState.update { it.copy(text = query, selection = TextRange(query.length)) }
            job?.cancel()
            if (query.isBlank()) {
                _results.update { emptyList() }
                return
            }
            job = viewModelScope.launch {
                runCatching {
                    val thisQuery = _searchState.value.text
                    search(thisQuery, page, { word ->
                        if (thisQuery != _searchState.value.text) throw CancellationException()
                        _results.update { word.data }
                    })
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