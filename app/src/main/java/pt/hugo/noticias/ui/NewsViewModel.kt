package pt.hugo.noticias.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import pt.hugo.noticias.data.Article
import pt.hugo.noticias.data.DefaultSources
import pt.hugo.noticias.data.FullArticle
import pt.hugo.noticias.data.NewsRepository
import pt.hugo.noticias.data.Prefs
import pt.hugo.noticias.data.Region
import pt.hugo.noticias.data.Source
import java.util.concurrent.TimeUnit

enum class FeedTab(val label: String) { ALL("Tudo"), PT("Portugal"), SPORT("Desporto"), EN("Mundo") }

data class SourceItem(val source: Source, val enabled: Boolean)

data class FeedState(
    val loading: Boolean = false,
    val articles: List<Article> = emptyList(),
    val failed: List<String> = emptyList(),
    val lastUpdated: Long = 0,
    val tab: FeedTab = FeedTab.ALL,
    val sourceFilter: String? = null,
)

sealed interface ReaderState {
    data object Loading : ReaderState
    data class Ready(val article: FullArticle) : ReaderState
    data class Failed(val message: String) : ReaderState
}

class NewsViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = Prefs(app)
    private val repo = NewsRepository(
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    )

    private val _sources = MutableStateFlow(loadSources())
    val sources: StateFlow<List<SourceItem>> = _sources.asStateFlow()

    private val _feed = MutableStateFlow(FeedState())
    val feed: StateFlow<FeedState> = _feed.asStateFlow()

    private val _reader = MutableStateFlow<ReaderState>(ReaderState.Loading)
    val reader: StateFlow<ReaderState> = _reader.asStateFlow()

    private val _fontScale = MutableStateFlow(prefs.fontScale)
    val fontScale: StateFlow<Float> = _fontScale.asStateFlow()

    private var refreshJob: Job? = null
    private var autoJob: Job? = null
    private var readerJob: Job? = null
    private var sourcesChanged = false

    private fun loadSources(): List<SourceItem> {
        val disabled = prefs.disabledIds
        return (DefaultSources.all + prefs.customSources).map { SourceItem(it, it.id !in disabled) }
    }

    fun refresh() {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            _feed.update { it.copy(loading = true) }
            val enabled = _sources.value.filter { it.enabled }.map { it.source }
            val result = repo.fetchAll(enabled)
            _feed.update {
                it.copy(
                    loading = false,
                    articles = result.articles,
                    failed = result.failed,
                    lastUpdated = System.currentTimeMillis(),
                )
            }
        }
    }

    /** Atualiza ao abrir a app e depois de 5 em 5 minutos enquanto está visível. */
    fun onForeground() {
        if (System.currentTimeMillis() - _feed.value.lastUpdated > 2 * 60_000) refresh()
        autoJob?.cancel()
        autoJob = viewModelScope.launch {
            while (isActive) {
                delay(5 * 60_000L)
                refresh()
            }
        }
    }

    fun onBackground() {
        autoJob?.cancel()
    }

    fun setTab(tab: FeedTab) = _feed.update { it.copy(tab = tab, sourceFilter = null) }

    fun setSourceFilter(id: String?) = _feed.update { it.copy(sourceFilter = id) }

    fun toggleSource(id: String, enabled: Boolean) {
        prefs.disabledIds = if (enabled) prefs.disabledIds - id else prefs.disabledIds + id
        _sources.value = loadSources()
        sourcesChanged = true
    }

    /** Devolve uma mensagem de erro, ou null se correu bem. */
    fun addCustomSource(name: String, url: String, region: Region): String? {
        val n = name.trim()
        val u = url.trim()
        if (n.isEmpty()) return "Dá um nome à fonte."
        if (!u.startsWith("http://") && !u.startsWith("https://")) return "O endereço tem de começar por http:// ou https://"
        if (_sources.value.any { it.source.url == u }) return "Essa fonte já existe."
        prefs.customSources = prefs.customSources + Source("custom_${System.currentTimeMillis()}", n, u, region, custom = true)
        _sources.value = loadSources()
        sourcesChanged = true
        return null
    }

    fun removeCustomSource(id: String) {
        prefs.customSources = prefs.customSources.filterNot { it.id == id }
        _sources.value = loadSources()
        sourcesChanged = true
    }

    fun onSourcesClosed() {
        if (sourcesChanged) {
            sourcesChanged = false
            refresh()
        }
    }

    fun open(article: Article) {
        readerJob?.cancel()
        _reader.value = ReaderState.Loading
        readerJob = viewModelScope.launch {
            _reader.value = runCatching { repo.fetchFullArticle(article) }.fold(
                onSuccess = { ReaderState.Ready(it) },
                onFailure = { ReaderState.Failed(it.message ?: "Erro desconhecido") },
            )
        }
    }

    fun setFontScale(value: Float) {
        val v = value.coerceIn(0.8f, 1.6f)
        prefs.fontScale = v
        _fontScale.value = v
    }
}
