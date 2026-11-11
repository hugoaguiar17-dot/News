package pt.hugo.noticias.data

enum class Region { PT, EN, SPORT }

data class Source(
    val id: String,
    val name: String,
    val url: String,
    val region: Region,
    val custom: Boolean = false,
)

data class Article(
    val id: String,
    val title: String,
    val link: String,
    val summary: String,
    val imageUrl: String?,
    val publishedAt: Long, // epoch ms; 0 = desconhecido
    val source: Source,
)

sealed interface Block {
    data class Paragraph(val text: String) : Block
    data class Heading(val text: String) : Block
    data class Quote(val text: String) : Block
    data class Bullet(val text: String) : Block
    data class Image(val url: String, val caption: String?) : Block
}

data class FullArticle(
    val title: String,
    val imageUrl: String?,
    val author: String?,
    val blocks: List<Block>,
)

/** Apenas fontes de acesso gratuito. Podes desligar/adicionar outras no ecrã "Fontes". */
object DefaultSources {
    val all = listOf(
        // Portugal
        Source("rtp", "RTP Notícias", "https://www.rtp.pt/noticias/rss", Region.PT),
        Source("nam", "Notícias ao Minuto", "https://www.noticiasaominuto.com/rss/ultima-hora", Region.PT),
        Source("sic", "SIC Notícias", "https://sicnoticias.pt/rss", Region.PT),
        Source("cnnpt", "CNN Portugal", "https://cnnportugal.iol.pt/rss", Region.PT),
        Source("tsf", "TSF", "https://www.tsf.pt/rss", Region.PT),
        Source("euronewspt", "Euronews PT", "https://pt.euronews.com/rss", Region.PT),
        // Internacional (inglês)
        Source("bbc", "BBC News", "https://feeds.bbci.co.uk/news/rss.xml", Region.EN),
        Source("guardian", "The Guardian", "https://www.theguardian.com/world/rss", Region.EN),
        // Desporto (Portugal)
        Source("bolanarede", "Bola na Rede", "https://www.bolanarede.pt/feed/", Region.SPORT),
        Source("maisfutebol", "Maisfutebol", "https://maisfutebol.iol.pt/rss", Region.SPORT),
        Source("zerozero", "zerozero", "https://www.zerozero.pt/rss/noticias.php", Region.SPORT),
        Source("ojogo", "O Jogo", "https://www.ojogo.pt/rss", Region.SPORT),
    )
}
