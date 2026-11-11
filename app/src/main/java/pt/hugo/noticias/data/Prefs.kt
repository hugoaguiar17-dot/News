package pt.hugo.noticias.data

import android.content.Context

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("noticias", Context.MODE_PRIVATE)

    var disabledIds: Set<String>
        get() = sp.getStringSet("disabled", null)?.toSet() ?: emptySet()
        set(v) = sp.edit().putStringSet("disabled", v).apply()

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

    var fontScale: Float
        get() = sp.getFloat("font", 1f)
        set(v) = sp.edit().putFloat("font", v).apply()
}
