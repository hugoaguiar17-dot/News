package pt.hugo.noticias.notify

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import pt.hugo.noticias.data.Http
import pt.hugo.noticias.data.NewsRepository
import pt.hugo.noticias.data.Prefs
import java.util.concurrent.TimeUnit

/** Corre em segundo plano (~15 em 15 min, o mínimo do Android) e notifica notícias novas. */
class NewsCheckWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val prefs = Prefs(applicationContext)
        val notify = prefs.notifyIds
        val disabled = prefs.disabledIds
        val sources = prefs.allSources.filter { it.id in notify && it.id !in disabled }
        if (sources.isEmpty()) return Result.success()

        val result = NewsRepository(Http.client).fetchAll(sources)
        val seen = prefs.seenIds
        val initialized = prefs.notifyInitIds
        val cutoff = System.currentTimeMillis() - 6 * 60 * 60_000L

        // Fontes acabadas de ligar: só marcam como vistas, sem notificar o feed inteiro.
        val fresh = result.articles.filter {
            it.source.id in initialized && it.id !in seen && (it.publishedAt == 0L || it.publishedAt > cutoff)
        }

        prefs.markSeen(result.articles.map { it.id })
        val loaded = result.articles.map { it.source.id }.toSet()
        prefs.notifyInitIds = initialized + loaded

        Notifier.show(applicationContext, fresh)
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "news_check"

        fun schedule(ctx: Context) {
            val prefs = Prefs(ctx)
            val active = prefs.notifyIds - prefs.disabledIds
            val wm = WorkManager.getInstance(ctx)
            if (active.isEmpty()) {
                wm.cancelUniqueWork(WORK_NAME)
                return
            }
            val request = PeriodicWorkRequestBuilder<NewsCheckWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            wm.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
