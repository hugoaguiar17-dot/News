package pt.hugo.noticias.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import pt.hugo.noticias.data.Article
import pt.hugo.noticias.data.Block
import pt.hugo.noticias.data.FullArticle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(vm: NewsViewModel, article: Article, onBack: () -> Unit) {
    val state by vm.reader.collectAsStateWithLifecycle()
    val scale by vm.fontScale.collectAsStateWithLifecycle()
    var showWeb by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current

    BackHandler(enabled = showWeb) { showWeb = false }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(article.source.name, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = { if (showWeb) showWeb = false else onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar")
                    }
                },
                actions = {
                    if (!showWeb && state is ReaderState.Ready) {
                        TextButton(onClick = { vm.setFontScale(scale - 0.1f) }) { Text("A−") }
                        TextButton(onClick = { vm.setFontScale(scale + 0.1f) }) { Text("A+", fontSize = 18.sp) }
                    }
                    IconButton(onClick = { share(context, article) }) { Icon(Icons.Filled.Share, "Partilhar") }
                },
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            if (showWeb) {
                WebPage(article.link)
            } else {
                when (val s = state) {
                    ReaderState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                    is ReaderState.Failed -> Column(
                        Modifier.align(Alignment.Center).padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            "Não foi possível extrair o texto deste artigo.",
                            textAlign = TextAlign.Center,
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { showWeb = true }) { Text("Ver página original") }
                    }
                    is ReaderState.Ready -> ArticleBody(
                        article = article,
                        full = s.article,
                        scale = scale,
                        onWeb = { showWeb = true },
                        onBrowser = { openInBrowser(context, article.link) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ArticleBody(
    article: Article,
    full: FullArticle,
    scale: Float,
    onWeb: () -> Unit,
    onBrowser: () -> Unit,
) {
    val bodySize = (17 * scale).sp
    val bodyLine = (17 * scale * 1.55f).sp
    val hero = full.imageUrl ?: article.imageUrl
    val blocks = full.blocks.filterNot { it is Block.Image && it.url == hero }
    val meta = listOfNotNull(full.author, fullDate(article.publishedAt).takeIf { it.isNotEmpty() })
        .joinToString(" · ")

    LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
        if (hero != null) {
            item {
                AsyncImage(
                    model = hero,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                )
            }
        }
        item {
            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp)) {
                Text(
                    full.title,
                    fontSize = (24 * scale).sp,
                    lineHeight = (31 * scale).sp,
                    fontWeight = FontWeight.Bold,
                )
                if (meta.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(meta, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        items(blocks) { b ->
            when (b) {
                is Block.Paragraph -> Text(
                    b.text,
                    fontSize = bodySize,
                    lineHeight = bodyLine,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                )
                is Block.Heading -> Text(
                    b.text,
                    fontSize = (20 * scale).sp,
                    lineHeight = (26 * scale).sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 4.dp),
                )
                is Block.Quote -> Row(
                    Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .height(IntrinsicSize.Min),
                ) {
                    Box(
                        Modifier
                            .width(3.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.primary),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(b.text, fontSize = bodySize, lineHeight = bodyLine, fontStyle = FontStyle.Italic)
                }
                is Block.Bullet -> Text(
                    "•  ${b.text}",
                    fontSize = bodySize,
                    lineHeight = bodyLine,
                    modifier = Modifier.padding(start = 24.dp, end = 16.dp, top = 2.dp, bottom = 2.dp),
                )
                is Block.Image -> Column(Modifier.padding(vertical = 8.dp)) {
                    AsyncImage(
                        model = b.url,
                        contentDescription = b.caption,
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    b.caption?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                    }
                }
            }
        }
        item {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(onClick = onWeb, modifier = Modifier.weight(1f)) { Text("Página original") }
                OutlinedButton(onClick = onBrowser, modifier = Modifier.weight(1f)) { Text("Abrir no browser") }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun WebPage(url: String) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            WebView(ctx).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient = WebViewClient()
                loadUrl(url)
            }
        },
    )
}

private fun share(context: Context, article: Article) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, "${article.title}\n${article.link}")
    }
    context.startActivity(Intent.createChooser(send, "Partilhar"))
}

private fun openInBrowser(context: Context, url: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
}
