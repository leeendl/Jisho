package jisho

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import jisho.ui.theme.Theme
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
                        val search by viewModel.search.observeAsState("")
                        val pending by viewModel.pending.observeAsState(false)
                        SearchBar(
                            search,
                            { viewModel.modelSearch(it) },
                            pending
                        )
                        val results by viewModel.results.observeAsState(emptyList())
                        Results(results, viewModel::modelSearch)
                    }
                }
            }
        }
    }

    class Model : ViewModel() {
        private val _search = MutableLiveData("")
        val search: LiveData<String> get() = _search

        private val _results = MutableLiveData<List<JishoData>>()
        val results: LiveData<List<JishoData>> get() = _results

        private val _pending = MutableLiveData(false)
        val pending: LiveData<Boolean> get() = _pending

        fun modelSearch(query: String, page: Int = 1) {
            _search.value = query
            if (query.isNotEmpty()) {
                _pending.value = true
                viewModelScope.launch {
                    search(query, page, { word ->
                        _results.postValue(word.data)
                        _pending.postValue(false)
                    }, {
                        // @todo further support dealing with API errors.
                        _pending.postValue(false)
                    })
                }
            } else {
                _results.postValue(emptyList())
            }
        }
    }

    @Composable
    fun SearchBar(
        search: String,
        onValueChange: (String) -> Unit,
        isLoading: Boolean
    ) {
        TextField(
            value = search,
            onValueChange = onValueChange,
            colors = TextFieldDefaults.colors(
                unfocusedIndicatorColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent
            ),
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_search),
                    contentDescription = "Search"
                )
            },
            trailingIcon = {
                if (isLoading) {
                    CircularProgressIndicator(Modifier.size(20.dp))
                }
            }
        )
    }

    @Composable
    fun Results(results: List<JishoData>, searchModel: (String) -> Unit) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            items(results) { word ->
                Row {
                    Text(
                        text = word.japanese.firstOrNull()?.let { japanese ->
                            japanese.word ?: japanese.reading
                        }.orEmpty(),
                        fontSize = 32.sp
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (word.isCommon) {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = Color(0xFF8abc83),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(5.dp, 0.dp, 3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "common word",
                                color = Color(0xFF222222),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (word.jlpt.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = Color(0xFF909dc0),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(5.dp, 0.dp, 3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = word.jlpt.firstOrNull().orEmpty(),
                                color = Color(0xFF222222),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (word.tags.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = Color(0xFF909dc0),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(5.dp, 0.dp, 3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = word.tags.firstOrNull().orEmpty(),
                                color = Color(0xFF222222),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
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
                        append(sense.englishDefinitions.firstOrNull().orEmpty())
                        if (sense.seeAlso.isNotEmpty()) {
                            withStyle(SpanStyle(color = Color(0xFFBBBBBB), fontSize = 16.sp)) {
                                append("  See also ")
                            }
                            pushStringAnnotation(
                                tag = "seeAlso",
                                annotation = sense.seeAlso.firstOrNull().orEmpty()
                            )
                            withStyle(
                                SpanStyle(
                                    color = Color(0xFFBBBBBB),
                                    fontSize = 16.sp,
                                    textDecoration = TextDecoration.Underline
                                )
                            ) {
                                append(sense.seeAlso.firstOrNull().orEmpty())
                            }
                            pop()
                        }
                        if (sense.info.isNotEmpty()) {
                            withStyle(SpanStyle(color = Color(0xFFBBBBBB), fontSize = 16.sp)) {
                                append(",  ${sense.info.firstOrNull().orEmpty()}")
                            }
                        }
                        append('\n')
                    }
                    var textLayoutResult: TextLayoutResult? = null
                    Text(
                        text = annotatedString,
                        modifier = Modifier
                            .pointerInput(annotatedString) {
                                detectTapGestures { offset ->
                                    val layoutResult = textLayoutResult
                                    if (layoutResult != null) {
                                        annotatedString.getStringAnnotations(
                                            "seeAlso",
                                            layoutResult.getOffsetForPosition(offset),
                                            layoutResult.getOffsetForPosition(offset)
                                        ).firstOrNull()
                                            ?.let { annotation -> searchModel(annotation.item) }
                                    }
                                }
                            },
                        onTextLayout = { textLayoutResult = it },
                        style = TextStyle(fontSize = 20.sp)
                    )
                }
                HorizontalDivider()
            }
        }
    }
}