package pt.hugo.noticias.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.io.IOException

data class FetchResult(val articles: List<Article>, val failed: List<String>)

class NewsRepository(private val client: OkHttpClient) {

    private companion object {
        const val UA = "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36"
    }

    suspend fun fetchAll(sources: List<Source>): FetchResult = coroutineScope {
        val results = sources.map { s ->
            async(Dispatchers.IO) { s to runCatching { fetchFeed(s) } }
        }.awaitAll()

        val articles = results
            .flatMap { it.second.getOrDefault(emptyList()) }
            .distinctBy { it.id }
            .sortedByDescending { it.publishedAt }
        val failed = results.filter { it.second.isFailure }.map { it.first.name }
        FetchResult(articles, failed)
    }

    private fun fetchFeed(s: Source): List<Article> {
        val req = Request.Builder().url(s.url).header("User-Agent", UA).build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}")
            val bytes = resp.body?.bytes() ?: throw IOException("Resposta vazia")
            return FeedParser.parse(bytes, s)
        }
    }

    suspend fun fetchFullArticle(a: Article): FullArticle = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url(a.link)
            .header("User-Agent", UA)
            .header("Accept-Language", "pt-PT,pt;q=0.9,en;q=0.8")
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IOException("HTTP ${resp.code}")
            val body = resp.body ?: throw IOException("Resposta vazia")
            val charset = body.contentType()?.charset()?.name()
            val doc = Jsoup.parse(body.byteStream(), charset, resp.request.url.toString())
            ArticleExtractor.extract(doc, a.title) ?: throw IOException("Texto não encontrado")
        }
    }
}
