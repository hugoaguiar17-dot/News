package pt.hugo.noticias.ui

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val ptLocale = Locale("pt", "PT")

fun relativeTime(ts: Long, now: Long): String {
    if (ts <= 0) return ""
    val min = (now - ts) / 60_000
    return when {
        min < 1 -> "agora"
        min < 60 -> "há $min min"
        min < 24 * 60 -> "há ${min / 60} h"
        else -> SimpleDateFormat("dd/MM HH:mm", ptLocale).format(Date(ts))
    }
}

fun fullDate(ts: Long): String =
    if (ts <= 0) "" else SimpleDateFormat("d MMM yyyy, HH:mm", ptLocale).format(Date(ts))
