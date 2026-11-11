package pt.hugo.noticias.data

import android.util.Xml
import org.jsoup.Jsoup
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Lê feeds RSS 2.0 e Atom. */
object FeedParser {

    private const val MAX_ITEMS = 50

    fun parse(bytes: ByteArray, source: Source): List<Article> {
        val head = String(bytes, 0, minOf(bytes.size, 300), Charsets.ISO_8859_1)
        val enc = Regex("encoding=[\"']([A-Za-z0-9_\\-]+)[\"']").find(head)?.groupValues?.get(1)
        val charset = runCatching { Charset.forName(enc ?: "UTF-8") }.getOrDefault(Charsets.UTF_8)
        val text = String(bytes, charset).trimStart('﻿', ' ', '\n', '\r', '\t')

        val parser = Xml.newPullParser()
        runCatching { parser.setFeature("http://xmlpull.org/v1/doc/features.html#relaxed", true) }
        parser.setInput(StringReader(text))

        val out = mutableListOf<Article>()
        var inItem = false
        var title = ""
        var link = ""
        var desc = ""
        var content = ""
        var date = ""
        var image: String? = null

        try {
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT && out.size < MAX_ITEMS) {
                when (event) {
                    XmlPullParser.START_TAG -> {
                        val name = parser.name.lowercase(Locale.ROOT)
                        if (name == "item" || name == "entry") {
                            inItem = true
                            title = ""; link = ""; desc = ""; content = ""; date = ""; image = null
                        } else if (inItem) {
                            when (name) {
                                "title" -> title = readText(parser)
                                "link" -> {
                                    val href = parser.getAttributeValue(null, "href")
                                    val rel = parser.getAttributeValue(null, "rel")
                                    val type = parser.getAttributeValue(null, "type")
                                    if (href != null) {
                                        if ((rel == null || rel == "alternate") && link.isEmpty()) link = href
                                        if (rel == "enclosure" && image == null && type?.startsWith("image") == true) image = href
                                    } else {
                                        link = readText(parser)
                                    }
                                }
                                "guid" -> {
                                    val g = readText(parser)
                                    if (link.isEmpty() && g.startsWith("http")) link = g
                                }
                                "description", "summary" -> desc = readText(parser)
                                "content:encoded", "content" -> content = readText(parser)
                                "pubdate", "published", "updated", "dc:date" ->
                                    if (date.isEmpty()) date = readText(parser) else readText(parser)
                                "media:content", "media:thumbnail", "enclosure" -> {
                                    val url = parser.getAttributeValue(null, "url")
                                    val type = parser.getAttributeValue(null, "type")
                                    val medium = parser.getAttributeValue(null, "medium")
                                    val isImage = type?.startsWith("image") == true || medium == "image" ||
                                        (type == null && medium == null)
                                    if (image == null && url != null && url.startsWith("http") && isImage) image = url
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        val name = parser.name.lowercase(Locale.ROOT)
                        if ((name == "item" || name == "entry") && inItem) {
                            inItem = false
                            build(source, title, link, desc, content, date, image)?.let { out += it }
                        }
                    }
                }
                event = parser.next()
            }
        } catch (e: Exception) {
            if (out.isEmpty()) throw e
        }
        return out
    }

    private fun build(
        source: Source, rawTitle: String, rawLink: String, desc: String,
        content: String, date: String, image: String?,
    ): Article? {
        val link = rawLink.trim()
        if (!link.startsWith("http")) return null
        val title = Jsoup.parse(rawTitle).text().trim()
        if (title.isEmpty()) return null

        val htmlForImage = desc.ifBlank { content }
        val descDoc = if (htmlForImage.isNotBlank()) Jsoup.parse(htmlForImage) else null
        var summary = (if (desc.isNotBlank()) Jsoup.parse(desc).text() else descDoc?.text().orEmpty()).trim()
        if (summary == title) summary = ""
        if (summary.length > 300) summary = summary.take(297).trimEnd() + "…"

        val img = image ?: descDoc?.selectFirst("img[src]")?.attr("src")?.takeIf { it.startsWith("http") }

        return Article(
            id = link,
            title = title,
            link = link,
            summary = summary,
            imageUrl = img,
            publishedAt = parseDate(date),
            source = source,
        )
    }

    private fun readText(p: XmlPullParser): String {
        val sb = StringBuilder()
        val depth = p.depth
        var ev = p.next()
        while (!(ev == XmlPullParser.END_TAG && p.depth == depth) && ev != XmlPullParser.END_DOCUMENT) {
            if (ev == XmlPullParser.TEXT) sb.append(p.text)
            ev = p.next()
        }
        return sb.toString().trim()
    }

    private val patterns = listOf(
        "EEE, dd MMM yyyy HH:mm:ss Z",
        "EEE, dd MMM yyyy HH:mm:ss XXX",
        "EEE, dd MMM yyyy HH:mm:ss zzz",
        "EEE, d MMM yyyy HH:mm:ss Z",
        "EEE, dd MMM yyyy HH:mm Z",
        "dd MMM yyyy HH:mm:ss Z",
        "yyyy-MM-dd'T'HH:mm:ssXXX",
        "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
        "yyyy-MM-dd HH:mm:ss",
    )

    fun parseDate(raw: String): Long {
        val s = raw.trim()
        if (s.isEmpty()) return 0
        runCatching { return OffsetDateTime.parse(s).toInstant().toEpochMilli() }
        runCatching { return Instant.parse(s).toEpochMilli() }
        runCatching { return ZonedDateTime.parse(s, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli() }
        for (p in patterns) {
            val t = runCatching {
                SimpleDateFormat(p, Locale.ENGLISH).apply { isLenient = false }.parse(s)?.time
            }.getOrNull()
            if (t != null) return t
        }
        return 0
    }
}
