package pt.hugo.noticias.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import pt.hugo.noticias.data.Article
import pt.hugo.noticias.data.Region
import pt.hugo.noticias.ui.theme.BreakingRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    vm: NewsViewModel,
    listState: LazyListState,
    onOpen: (Article) -> Unit,
    onSources: () -> Unit,
) {
    val state by vm.feed.collectAsStateWithLifecycle()
    val sources by vm.sources.collectAsStateWithLifecycle()

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            now = System.currentTimeMillis()
        }
    }

    fun inTab(region: Region) = when (state.tab) {
        FeedTab.ALL -> true
        FeedTab.PT -> region == Region.PT
        FeedTab.SPORT -> region == Region.SPORT
        FeedTab.EN -> region == Region.EN
    }

    val enabledIds = remember(sources) { sources.filter { it.enabled }.map { it.source.id }.toSet() }
    val visible = remember(state, enabledIds) {
        state.articles.filter {
            it.source.id in enabledIds && inTab(it.source.region) &&
                (state.sourceFilter == null || it.source.id == state.sourceFilter)
        }
    }
    val chipSources = sources.filter { it.enabled && inTab(it.source.region) }.map { it.source }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Notícias PT", fontWeight = FontWeight.Bold)
                        if (state.lastUpdated > 0) {
                            Text(
                                "Atualizado ${relativeTime(state.lastUpdated, now)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = vm::refresh) { Icon(Icons.Filled.Refresh, "Atualizar") }
                    IconButton(onClick = onSources) { Icon(Icons.Filled.Settings, "Fontes") }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = state.tab.ordinal) {
                FeedTab.entries.forEach { t ->
                    Tab(selected = state.tab == t, onClick = { vm.setTab(t) }, text = { Text(t.label) })
                }
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    FilterChip(
                        selected = state.sourceFilter == null,
                        onClick = { vm.setSourceFilter(null) },
                        label = { Text("Todas") },
                    )
                }
                items(chipSources, key = { it.id }) { s ->
                    FilterChip(
                        selected = state.sourceFilter == s.id,
                        onClick = { vm.setSourceFilter(s.id) },
                        label = { Text(s.name) },
                    )
                }
            }

            if (state.failed.isNotEmpty()) {
                Text(
                    "Sem resposta de: ${state.failed.joinToString()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }

            PullToRefreshBox(
                isRefreshing = state.loading,
                onRefresh = vm::refresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                    if (visible.isEmpty() && !state.loading) {
                        item {
                            Text(
                                "Sem notícias. Puxa para baixo para atualizar.",
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                            )
                        }
                    }
                    items(visible, key = { it.id }) { a ->
                        ArticleRow(a, now) { onOpen(a) }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun ArticleRow(a: Article, now: Long, onClick: () -> Unit) {
    val ageMin = if (a.publishedAt > 0) (now - a.publishedAt) / 60_000 else Long.MAX_VALUE
    val time = relativeTime(a.publishedAt, now)

    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (ageMin < 60) {
                    Text(
                        "ÚLTIMA HORA",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(BreakingRed, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    if (time.isEmpty()) a.source.name else "${a.source.name} · $time",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                a.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
            )
            if (a.summary.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    a.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (a.imageUrl != null) {
            Spacer(Modifier.width(12.dp))
            AsyncImage(
                model = a.imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(88.dp).clip(RoundedCornerShape(8.dp)),
            )
        }
    }
}
