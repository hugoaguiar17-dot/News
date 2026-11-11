package pt.hugo.noticias

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import pt.hugo.noticias.data.Article
import pt.hugo.noticias.data.Prefs
import pt.hugo.noticias.data.Region
import pt.hugo.noticias.data.Source
import pt.hugo.noticias.notify.Notifier
import pt.hugo.noticias.ui.FeedScreen
import pt.hugo.noticias.ui.NewsViewModel
import pt.hugo.noticias.ui.ReaderScreen
import pt.hugo.noticias.ui.SourcesScreen
import pt.hugo.noticias.ui.theme.NoticiasTheme

private sealed interface Screen {
    data object Feed : Screen
    data class Reader(val article: Article) : Screen
    data object Sources : Screen
}

class MainActivity : ComponentActivity() {

    /** Notícia vinda de uma notificação, à espera de ser aberta. */
    private var pendingArticle by mutableStateOf<Article?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Notifier.ensureChannel(this)
        if (savedInstanceState == null) handleIntent(intent)

        setContent {
            NoticiasTheme {
                val vm: NewsViewModel = viewModel()
                val listState = rememberLazyListState()
                var screen by remember { mutableStateOf<Screen>(Screen.Feed) }

                LifecycleStartEffect(Unit) {
                    vm.onForeground()
                    onStopOrDispose { vm.onBackground() }
                }

                LaunchedEffect(pendingArticle) {
                    pendingArticle?.let {
                        vm.open(it)
                        screen = Screen.Reader(it)
                        pendingArticle = null
                    }
                }

                when (val s = screen) {
                    Screen.Feed -> FeedScreen(
                        vm = vm,
                        listState = listState,
                        onOpen = {
                            vm.open(it)
                            screen = Screen.Reader(it)
                        },
                        onSources = { screen = Screen.Sources },
                    )

                    is Screen.Reader -> {
                        BackHandler { screen = Screen.Feed }
                        ReaderScreen(vm, s.article, onBack = { screen = Screen.Feed })
                    }

                    Screen.Sources -> {
                        val close = {
                            vm.onSourcesClosed()
                            screen = Screen.Feed
                        }
                        BackHandler(onBack = close)
                        SourcesScreen(vm, onBack = close)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val link = intent?.getStringExtra(Notifier.EXTRA_LINK) ?: return
        val sourceId = intent.getStringExtra(Notifier.EXTRA_SOURCE).orEmpty()
        val source = Prefs(this).allSources.firstOrNull { it.id == sourceId }
            ?: Source(sourceId, "Notícias", "", Region.PT)
        pendingArticle = Article(
            id = link,
            title = intent.getStringExtra(Notifier.EXTRA_TITLE).orEmpty(),
            link = link,
            summary = "",
            imageUrl = intent.getStringExtra(Notifier.EXTRA_IMAGE),
            publishedAt = intent.getLongExtra(Notifier.EXTRA_PUBLISHED, 0L),
            source = source,
        )
        intent.removeExtra(Notifier.EXTRA_LINK)
    }
}
