package jisho

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import words.ui.theme.Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        getActionBar()?.hide()
        super.onCreate(savedInstanceState)
        val viewModel = Model()
        setContent {
            Theme(true) {
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
        searchQuery: String,
        onSearchQueryChange: (String) -> Unit,
        isLoading: Boolean
    ) {
        TextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
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
        LazyColumn {
            items(results) { word ->
                Text(
                    text = word.japanese.firstOrNull()?.let { japanese ->
                        japanese.word ?: japanese.reading
                    }.orEmpty(),
                    modifier = Modifier
                        .fillMaxWidth(),
                    textAlign = TextAlign.Start
                )
            }
        }
    }
}