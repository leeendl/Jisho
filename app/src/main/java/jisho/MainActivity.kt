package jisho

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import jisho.ui.theme.Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        getActionBar()?.hide()
        super.onCreate(savedInstanceState)
        val viewModel = Model()
        setContent {
            Theme {
                Surface {
                    val search by viewModel.search.observeAsState("")
                    val results by viewModel.results.observeAsState(emptyList())
                    val pending by viewModel.pending.observeAsState(false)
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(WindowInsets.systemBars.asPaddingValues()), // @note on screen camera
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        SearchBar(
                            search,
                            { viewModel.modelSearch(it) },
                            pending
                        )
                        Results(results)
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
            if (query.isNotEmpty() && query.length > 1) { // @todo overload issues. fix later.
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
    fun Results(results: List<JishoData>) {
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
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = Color(0xAAAAAAAA), fontSize = 16.sp)) {
                                append("${sense.partsOfSpeech.firstOrNull().orEmpty()}\n")
                            }
                            withStyle(SpanStyle(color = Color(0x99999999), fontSize = 18.sp)) {
                                append("${index + 1}.  ")
                            }
                            withStyle(SpanStyle()) {
                                append(sense.englishDefinitions.firstOrNull().orEmpty())
                            }
                            // @todo redirect.
                            if (sense.seeAlso.isNotEmpty()) {
                                withStyle(SpanStyle(color = Color(0xBBBBBBBB), fontSize = 16.sp)) {
                                    append("  See also ${sense.seeAlso.firstOrNull().orEmpty()}")
                                    if (sense.info.isNotEmpty()) append(',')
                                }
                            }
                            if (sense.info.isNotEmpty()) {
                                withStyle(SpanStyle(color = Color(0xBBBBBBBB), fontSize = 16.sp)) {
                                    append("  ${sense.info.firstOrNull().orEmpty()}")
                                }
                            }
                            append('\n')
                        },
                        modifier = Modifier
                            .fillMaxWidth(),
                        fontSize = 20.sp
                    )
                }
                HorizontalDivider()
            }
        }
    }
}