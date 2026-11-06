package com.aistudio.smartcostpro.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

/**
 * Cost App Pro - Application class.
 *
 * - Annotated with [HiltAndroidApp] to bootstrap Hilt dependency injection.
 * - Hooks into [HiltWorkerFactory] so WorkManager (used for background backup / sync)
 *   constructs workers with their Hilt-injected dependencies.
 * - Plants a [Timber.DebugTree] in debug builds for diagnostics.
 */
@HiltAndroidApp
class CostAppProApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        Timber.tag("CostAppPro").i("Application started — version ${BuildConfig.VERSION_NAME}")
    }
}
