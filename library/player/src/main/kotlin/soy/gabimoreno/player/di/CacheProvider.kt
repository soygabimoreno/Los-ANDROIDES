package soy.gabimoreno.player.di

import android.content.Context
import com.google.android.exoplayer2.database.ExoDatabaseProvider
import com.google.android.exoplayer2.upstream.cache.NoOpCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import java.io.File

object CacheProvider {
    @Volatile
    private var simpleCache: SimpleCache? = null

    fun get(context: Context): SimpleCache =
        simpleCache ?: synchronized(this) {
            simpleCache ?: run {
                val cacheDir = File(context.cacheDir, "media")
                val databaseProvider = ExoDatabaseProvider(context)
                val cache = SimpleCache(cacheDir, NoOpCacheEvictor(), databaseProvider)
                simpleCache = cache
                cache
            }
        }

    fun release() {
        simpleCache?.release()
        simpleCache = null
    }
}
