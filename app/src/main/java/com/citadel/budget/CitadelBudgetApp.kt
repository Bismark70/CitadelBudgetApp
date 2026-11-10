package com.citadel.budget

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.citadel.budget.data.SettingsStore
import com.citadel.budget.data.local.CitadelDatabase
import com.citadel.budget.data.local.DefaultData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CitadelBudgetApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.seedIfEmpty()
    }
}

/**
 * Simple service locator. Everything runs locally on this device — no network stack at all.
 */
class AppContainer(private val context: Context) {

    val db: CitadelDatabase by lazy {
        Room.databaseBuilder(context, CitadelDatabase::class.java, "citadel.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    val settings: SettingsStore by lazy { SettingsStore(context) }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun seedIfEmpty() = appScope.launch {
        if (db.categoryDao().count() == 0) {
            db.categoryDao().insertAll(DefaultData.categories())
        }
    }
}
