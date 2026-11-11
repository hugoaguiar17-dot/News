package pt.hugo.noticias.data

import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.net.URL

/**
 * Extrai o texto principal de uma página de notícia (estilo "modo de leitura").
 * Devolve null se não encontrar texto suficiente — nesse caso a app mostra a página original.
 */
object ArticleExtractor {

    private const val JUNK = "script, style, noscript, iframe, form, nav, footer, aside, button, svg, " +
        "input, select, textarea, [class*=related], [class*=Related], [class*=share], [class*=newsletter], " +
        "[class*=comment], [class*=advert], [id*=advert], [class*=banner], [class*=promo], [class*=social], " +
        "[class*=cookie], [id*=cookie], [class*=paywall]"

    fun extract(doc: Document, fallbackTitle: String): FullArticle? {
        val ogImage = doc.selectFirst("meta[property=og:image]")?.absUrl("content")?.takeIf { it.isNotBlank() }
        val ogTitle = doc.selectFirst("meta[property=og:title]")?.attr("content")?.trim()?.takeIf { it.isNotBlank() }
        val author = doc.selectFirst("meta[name=author]")?.attr("content")?.trim()?.takeIf { it.isNotBlank() }

        doc.select(JUNK).remove()
        val root = findContentRoot(doc) ?: return null

        val raw = mutableListOf<Block>()
        walk(root, raw)

        val title = ogTitle ?: fallbackTitle
        val blocks = mutableListOf<Block>()
        for (b in raw) {
            if (blocks.lastOrNull() == b) continue
            if (b is Block.Paragraph && (b.text == title || b.text == fallbackTitle)) continue
            if (b is Block.Heading && (b.text == title || b.text == fallbackTitle)) continue
            blocks += b
        }

        val textLen = blocks.sumOf { if (it is Block.Paragraph) it.text.length else 0 }
        if (textLen < 250) return null

        return FullArticle(title = title, imageUrl = ogImage, author = author, blocks = blocks)
    }

    private fun findContentRoot(doc: Document): Element? {
        doc.selectFirst("[itemprop=articleBody]")?.let { if (paragraphText(it) > 300) return it }

        val scores = HashMap<Element, Double>()
        for (p in doc.select("p")) {
            val text = p.text()
            if (text.length < 40) continue
            val score = 1.0 + text.count { it == ',' } + minOf(text.length / 100, 3)
            val parent = p.parent() ?: continue
            scores[parent] = (scores[parent] ?: 0.0) + score
            parent.parent()?.let { gp -> scores[gp] = (scores[gp] ?: 0.0) + score / 2 }
        }
        return scores.maxByOrNull { it.value }?.key
    }

    private fun paragraphText(el: Element) = el.select("p").sumOf { it.text().length }

    private fun linkDensity(el: Element): Double {
        val total = el.text().length
        if (total == 0) return 1.0
        return el.select("a").sumOf { it.text().length }.toDouble() / total
    }

    private fun walk(el: Element, out: MutableList<Block>) {
        for (c in el.children()) {
            when (c.normalName()) {
                "p" -> {
                    c.select("img").forEach { img -> imageUrl(img)?.let { out += Block.Image(it, null) } }
                    val t = c.text().trim()
                    if (t.isNotEmpty() && !(linkDensity(c) > 0.9 && t.length < 200)) out += Block.Paragraph(t)
                }
                "h1", "h2", "h3", "h4", "h5" -> c.text().trim().takeIf { it.isNotEmpty() }?.let { out += Block.Heading(it) }
                "blockquote" -> c.text().trim().takeIf { it.isNotEmpty() }?.let { out += Block.Quote(it) }
                "ul", "ol" -> if (linkDensity(c) < 0.8) {
                    c.children().filter { it.normalName() == "li" }.forEach { li ->
                        li.text().trim().takeIf { it.isNotEmpty() }?.let { out += Block.Bullet(it) }
                    }
                }
                "img" -> imageUrl(c)?.let { out += Block.Image(it, c.attr("alt").takeIf { a -> a.isNotBlank() }) }
                "figure", "picture" -> {
                    val img = c.selectFirst("img")
                    val cap = c.selectFirst("figcaption")?.text()?.trim()?.takeIf { it.isNotBlank() }
                    img?.let { imageUrl(it) }?.let { out += Block.Image(it, cap) }
                }
                "br", "hr" -> Unit
                else -> walk(c, out)
            }
        }
    }

    private fun imageUrl(img: Element): String? {
        val w = img.attr("width").toIntOrNull()
        if (w != null && w < 120) return null
        for (attr in listOf("data-src", "data-lazy-src", "data-original", "src")) {
            val u = img.absUrl(attr)
            if (u.startsWith("http") && !u.endsWith(".svg") && !u.contains("pixel")) return u
        }
        val srcset = img.attr("srcset").ifBlank { img.attr("data-srcset") }
        if (srcset.isNotBlank()) {
            val last = srcset.split(",").last().trim().split(" ").first()
            return runCatching { URL(URL(img.baseUri()), last).toString() }.getOrNull()
        }
        return null
    }
}
