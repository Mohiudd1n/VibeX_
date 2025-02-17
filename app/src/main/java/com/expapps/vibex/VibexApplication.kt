package com.expapps.vibex

import android.app.Application
import com.google.android.exoplayer2.database.ExoDatabaseProvider
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import com.google.android.gms.ads.MobileAds


class VibexApplication : Application() {


    companion object {
        var simpleCache: SimpleCache? = null
    }

    var exoPlayerCacheSize = (90 * 1024 * 1024).toLong()
    private lateinit var leastRecentlyUsedCacheEvictor: LeastRecentlyUsedCacheEvictor
    private lateinit var exoDatabaseProvider: ExoDatabaseProvider
    override fun onCreate() {
        super.onCreate()

        MobileAds.initialize(this)

        leastRecentlyUsedCacheEvictor = LeastRecentlyUsedCacheEvictor(exoPlayerCacheSize)
        exoDatabaseProvider = ExoDatabaseProvider(this)
        simpleCache = SimpleCache(cacheDir, leastRecentlyUsedCacheEvictor, exoDatabaseProvider)


    }
}