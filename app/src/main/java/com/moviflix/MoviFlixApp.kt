package com.moviflix

import android.app.Application
import androidx.work.Configuration
import com.moviflix.api.HitvRepository
import com.moviflix.api.NetworkModule
import com.moviflix.utils.SessionManager

class MoviFlixApp : Application(), Configuration.Provider {

    lateinit var session: SessionManager
        private set

    lateinit var repository: HitvRepository
        private set

    override fun onCreate() {
        super.onCreate()
        session = SessionManager(this)
        val api = NetworkModule.provideApiService(session)
        repository = HitvRepository(api, NetworkModule.gson)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()
}
