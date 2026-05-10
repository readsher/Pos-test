package com.postest.app

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.postest.app.data.repo.SettingsRepo
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class PosApplication : Application() {

    @Inject lateinit var settingsRepo: SettingsRepo
    @Inject lateinit var seeder: com.postest.app.data.db.DatabaseSeeder

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            seeder.seedIfEmpty()
            val lang = settingsRepo.getLanguage()
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(lang))
        }
    }
}
