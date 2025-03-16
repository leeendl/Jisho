package jisho

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import jisho.ui.theme.Theme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        getActionBar()?.hide()
        super.onCreate(savedInstanceState)
        val viewModel = Model()
        setContent {
            Theme {
                Surface {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(WindowInsets.systemBars.asPaddingValues()), // @note on screen camera
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        SearchBar(
                            viewModel
                        ) { viewModel.modelSearch(it) }
                        val results by viewModel.results.collectAsState(emptyList())
                        if (results.isNotEmpty()) Results(results, viewModel)
                    }
                }
            }
        }
    }

    class Model : ViewModel() {
        private val _search = MutableStateFlow("")
        val search: StateFlow<String> get() = _search

        private val _results = MutableStateFlow<List<JishoData>>(emptyList())
        val results: StateFlow<List<JishoData>> get() = _results

        val iPos = MutableStateFlow(0)

        fun modelSearch(query: String, page: Int = 1) {
            _search.value = query
            iPos.value = query.length
            if (query.isNotEmpty()) {
                viewModelScope.launch {
                    val thisQuery = _search.value
                    search(query, page, { word ->
                        if (thisQuery == _search.value) {
                            _results.value = word.data
                        }
                    })
                }
            } else {
                _results.value = emptyList()
            }
        }
    }

    @Composable
    private fun SearchBar(
        viewModel: Model,
        onValueChange: (String) -> Unit
    ) {
        val search by viewModel.search.collectAsState("")
        val iPos by viewModel.iPos.collectAsState(0)
        TextField(
            value = TextFieldValue(text = search, selection = TextRange(iPos)),
            onValueChange = { newValue ->
                onValueChange(newValue.text)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 8.dp)
                .border(3.dp, Color(0xFF6E6E6E), RoundedCornerShape(6.dp)),
            placeholder = { Text("Search") },
            colors = TextFieldDefaults.colors(
                unfocusedIndicatorColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent
            ),
            leadingIcon = {
                // @todo add keyword filter
            },
            trailingIcon = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy((-6).dp),
                    verticalAlignment = Alignment.CenterVertically // @note adapt with ic_search padding
                ) {
                    if (search.isNotEmpty()) {
                        IconButton(
                            onClick = { onValueChange("") }
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_clear),
                                contentDescription = "Clear"
                            )
                        }
                    }
                    IconButton(
                        onClick = { }, // @todo
                        modifier = Modifier
                            .padding(6.dp)
                            .background(color = Color(0xFF6A6A6A))
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_search),
                            contentDescription = "Search"
                        )
                    }
                }
            }
        )
    }

    @Composable
    fun Results(results: List<JishoData>, viewModel: Model) {
        val search by viewModel.search.collectAsState()
        if (search.isEmpty()) return
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            item {
                if (search.all { it in 'a'..'z' || it in 'A'..'Z' } &&
                    search.canEtoH()) {
                    val annotatedString = buildAnnotatedString {
                        append("Searched for ${replaceEtoH(search)}. You can also try a search for ")
                        pushStringAnnotation(
                            tag = "clickSearch",
                            annotation = "\"$search\""
                        )
                        withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                            append("\"$search\"")
                        }
                        pop()
                        append(".")
                    }
                    var textLayoutResult: TextLayoutResult? = null
                    Text(
                        text = annotatedString,
                        modifier = Modifier
                            .pointerInput(annotatedString) {
                                tapGesture(viewModel, annotatedString, textLayoutResult)
                            },
                        onTextLayout = { textLayoutResult = it }
                    )
                }
            }
            items(results) { word ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    var japanese = word.japanese.firstOrNull()
                    Text(
                        text = japanese?.let { if (it.word != null) it.reading else "" }.orEmpty(),
                        fontSize = 16.sp
                    )
                    Text(
                        text = japanese?.let { it.word ?: it.reading }.orEmpty(),
                        fontSize = 32.sp
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (word.isCommon) {
                        wordTag("common word", Color(0xFF8abc83))
                    }
                    if (word.jlpt.isNotEmpty()) {
                        wordTag(word.jlpt.firstOrNull().orEmpty(), Color(0xFF909dc0))
                    }
                    if (word.tags.isNotEmpty()) {
                        wordTag(word.tags.firstOrNull().orEmpty(), Color(0xFF909dc0))
                    }
                }
                for ((index, sense) in word.senses.withIndex()) {
                    val annotatedString = buildAnnotatedString {
                        withStyle(SpanStyle(color = Color(0xFFAAAAAA), fontSize = 16.sp)) {
                            append("${sense.partsOfSpeech.firstOrNull().orEmpty()}\n")
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
                                withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) {
                                    append(sense.seeAlso.firstOrNull().orEmpty())
                                }
                                pop()
                            }
                            if (sense.info.isNotEmpty()) {
                                if (sense.seeAlso.isNotEmpty()) append(',')
                                append("  ${sense.info.firstOrNull().orEmpty()}")
                            }
                        }
                        append('\n')
                    }
                    var textLayoutResult: TextLayoutResult? = null
                    Text(
                        text = annotatedString,
                        modifier = Modifier
                            .pointerInput(annotatedString) {
                                tapGesture(viewModel, annotatedString, textLayoutResult)
                            },
                        onTextLayout = { textLayoutResult = it },
                        style = TextStyle(fontSize = 20.sp)
                    )
                }
                HorizontalDivider()
            }
        }
    }

    @Composable
    private fun wordTag(
        label: String,
        backgroundColor: Color
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = backgroundColor,
                    shape = RoundedCornerShape(4.dp)
                )
                .padding(start = 5.dp, end = 3.dp)
        ) {
            Text(
                text = label,
                color = Color(0xFF222222),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    private suspend fun PointerInputScope.tapGesture(
        viewModel: Model,
        annotatedString: AnnotatedString,
        textLayoutResult: TextLayoutResult?
    ) {
        this.detectTapGestures { offset ->
            textLayoutResult?.let {
                val offsetPosition = it.getOffsetForPosition(offset)
                annotatedString.getStringAnnotations(
                    "clickSearch",
                    offsetPosition,
                    offsetPosition
                ).firstOrNull()?.let { annotation ->
                    viewModel.modelSearch(annotation.item)
                    viewModel.iPos.value = annotation.item.length
                }
            }
        }
    }
}