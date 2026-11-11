package pt.hugo.noticias.notify

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import pt.hugo.noticias.MainActivity
import pt.hugo.noticias.R
import pt.hugo.noticias.data.Article

object Notifier {
    private const val CHANNEL = "noticias_novas"
    private const val GROUP = "pt.hugo.noticias.NOVAS"
    private const val SUMMARY_ID = 1
    private const val MAX_INDIVIDUAL = 6

    const val EXTRA_LINK = "link"
    const val EXTRA_TITLE = "title"
    const val EXTRA_SOURCE = "source"
    const val EXTRA_IMAGE = "image"
    const val EXTRA_PUBLISHED = "published"

    fun ensureChannel(ctx: Context) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "Notícias novas", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Aviso quando as fontes escolhidas publicam uma notícia"
                }
            )
        }
    }

    fun canNotify(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun show(ctx: Context, articles: List<Article>) {
        if (articles.isEmpty() || !canNotify(ctx)) return
        ensureChannel(ctx)
        val nm = NotificationManagerCompat.from(ctx)
        val sorted = articles.sortedByDescending { it.publishedAt }

        sorted.take(MAX_INDIVIDUAL).forEach { a ->
            val n = NotificationCompat.Builder(ctx, CHANNEL)
                .setSmallIcon(R.drawable.ic_stat_news)
                .setContentTitle(a.source.name)
                .setContentText(a.title)
                .setStyle(NotificationCompat.BigTextStyle().bigText(a.title))
                .setWhen(if (a.publishedAt > 0) a.publishedAt else System.currentTimeMillis())
                .setShowWhen(true)
                .setAutoCancel(true)
                .setGroup(GROUP)
                .setContentIntent(articleIntent(ctx, a))
                .build()
            nm.notify(a.id.hashCode(), n)
        }

        if (sorted.size > 1) {
            val inbox = NotificationCompat.InboxStyle()
            sorted.take(5).forEach { inbox.addLine("${it.source.name}: ${it.title}") }
            if (sorted.size > 5) inbox.setSummaryText("+${sorted.size - 5} mais")
            val summary = NotificationCompat.Builder(ctx, CHANNEL)
                .setSmallIcon(R.drawable.ic_stat_news)
                .setContentTitle("${sorted.size} notícias novas")
                .setStyle(inbox)
                .setGroup(GROUP)
                .setGroupSummary(true)
                .setAutoCancel(true)
                .setContentIntent(openAppIntent(ctx))
                .build()
            nm.notify(SUMMARY_ID, summary)
        }
    }

    private fun articleIntent(ctx: Context, a: Article): PendingIntent {
        val intent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_LINK, a.link)
            putExtra(EXTRA_TITLE, a.title)
            putExtra(EXTRA_SOURCE, a.source.id)
            putExtra(EXTRA_IMAGE, a.imageUrl)
            putExtra(EXTRA_PUBLISHED, a.publishedAt)
        }
        return PendingIntent.getActivity(
            ctx, a.id.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun openAppIntent(ctx: Context): PendingIntent {
        val intent = Intent(ctx, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        return PendingIntent.getActivity(ctx, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}
