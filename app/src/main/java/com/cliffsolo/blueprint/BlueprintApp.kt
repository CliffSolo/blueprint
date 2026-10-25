package com.cliffsolo.blueprint

import android.app.Application
import com.cliffsolo.blueprint.data.AppDatabase
import com.cliffsolo.blueprint.data.CourseStore
import com.cliffsolo.blueprint.data.SettingsRepo
import com.cliffsolo.blueprint.sync.GithubSync
import com.cliffsolo.blueprint.sync.SyncWorker
import com.cliffsolo.blueprint.tts.Narrator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Holds the app-wide objects. Small enough that a DI framework would be overkill. */
class BlueprintApp : Application() {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    val db by lazy { AppDatabase.create(this) }
    val store by lazy { CourseStore(this) }
    val settings by lazy { SettingsRepo(this) }
    val sync by lazy { GithubSync(store, db.sync()) }
    val narrator by lazy { Narrator(this) }

    override fun onCreate() {
        super.onCreate()
        scope.launch { store.refresh() }
        scope.launch { settings.settings.collect { narrator.applySettings(it) } }
        scope.launch {
            settings.settings.map { it.autoSync }.distinctUntilChanged().collect { on ->
                if (on) SyncWorker.schedule(this@BlueprintApp) else SyncWorker.cancel(this@BlueprintApp)
            }
        }
    }
}
