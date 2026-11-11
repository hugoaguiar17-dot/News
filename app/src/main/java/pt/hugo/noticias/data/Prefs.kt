package pt.hugo.noticias.data

import android.content.Context

class Prefs(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("noticias", Context.MODE_PRIVATE)

    var disabledIds: Set<String>
        get() = sp.getStringSet("disabled", null)?.toSet() ?: emptySet()
        set(v) = sp.edit().putStringSet("disabled", v).apply()

    /** Fontes com notificações ligadas. */
    var notifyIds: Set<String>
        get() = sp.getStringSet("notify", null)?.toSet() ?: emptySet()
        set(v) = sp.edit().putStringSet("notify", v).apply()

    /** Fontes cujo feed já foi visto pelo menos uma vez pelas notificações (evita avalanche ao ligar). */
    var notifyInitIds: Set<String>
        get() = sp.getStringSet("notify_init", null)?.toSet() ?: emptySet()
        set(v) = sp.edit().putStringSet("notify_init", v).apply()

    var customSources: List<Source>
        get() = sp.getString("custom", "").orEmpty().lines().filter { it.isNotBlank() }.mapNotNull { line ->
            val p = line.split("\t")
            if (p.size < 4) return@mapNotNull null
            val region = runCatching { Region.valueOf(p[3]) }.getOrDefault(Region.PT)
            Source(p[0], p[1], p[2], region, custom = true)
        }
        set(v) = sp.edit().putString(
            "custom",
            v.joinToString("\n") { "${it.id}\t${it.name}\t${it.url}\t${it.region.name}" },
        ).apply()

    val allSources: List<Source> get() = DefaultSources.all + customSources

    var fontScale: Float
        get() = sp.getFloat("font", 1f)
        set(v) = sp.edit().putFloat("font", v).apply()

    /** Notícias já vistas (na app ou já notificadas). */
    val seenIds: Set<String>
        get() = sp.getString("seen", "").orEmpty().lines().filter { it.isNotBlank() }.toSet()

    fun markSeen(ids: Collection<String>) {
        if (ids.isEmpty()) return
        synchronized(lock) {
            val current = sp.getString("seen", "").orEmpty().lines().filter { it.isNotBlank() }
            val merged = LinkedHashSet(current).apply { addAll(ids) }.toList().takeLast(MAX_SEEN)
            sp.edit().putString("seen", merged.joinToString("\n")).commit()
        }
    }

    private companion object {
        const val MAX_SEEN = 2000
        val lock = Any()
    }
}
