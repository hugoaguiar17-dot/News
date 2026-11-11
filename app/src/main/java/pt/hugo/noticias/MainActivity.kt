package pt.hugo.noticias

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import pt.hugo.noticias.data.Article
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NoticiasTheme {
                val vm: NewsViewModel = viewModel()
                val listState = rememberLazyListState()
                var screen by remember { mutableStateOf<Screen>(Screen.Feed) }

                LifecycleStartEffect(Unit) {
                    vm.onForeground()
                    onStopOrDispose { vm.onBackground() }
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
}
