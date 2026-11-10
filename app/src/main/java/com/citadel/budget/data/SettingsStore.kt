package com.citadel.budget.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import java.util.UUID

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class UserSettings(
    val onboarded: Boolean = false,
    val userName: String = "",
    val incomeCents: Long = 0L,
    val needsPct: Int = 50,
    val wantsPct: Int = 30,
    val savingsPct: Int = 20,
    val currency: String = "USD",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val pinEnabled: Boolean = false,
    val pinHash: String = "",
    val pinSalt: String = "",
    val survivalCents: Long = 0L,
    val babyStepsDone: Set<Int> = emptySet()
)

private val Context.citadelStore by preferencesDataStore(name = "citadel_settings")

class SettingsStore(private val context: Context) {

    private object K {
        val onboarded = booleanPreferencesKey("onboarded")
        val userName = stringPreferencesKey("user_name")
        val income = longPreferencesKey("income_cents")
        val needsPct = intPreferencesKey("needs_pct")
        val wantsPct = intPreferencesKey("wants_pct")
        val savingsPct = intPreferencesKey("savings_pct")
        val currency = stringPreferencesKey("currency")
        val theme = stringPreferencesKey("theme_mode")
        val pinEnabled = booleanPreferencesKey("pin_enabled")
        val pinHash = stringPreferencesKey("pin_hash")
        val pinSalt = stringPreferencesKey("pin_salt")
        val survival = longPreferencesKey("survival_cents")
        val babySteps = stringPreferencesKey("baby_steps")
    }

    val settings: Flow<UserSettings> = context.citadelStore.data.map { p ->
        UserSettings(
            onboarded = p[K.onboarded] ?: false,
            userName = p[K.userName] ?: "",
            incomeCents = p[K.income] ?: 0L,
            needsPct = p[K.needsPct] ?: 50,
            wantsPct = p[K.wantsPct] ?: 30,
            savingsPct = p[K.savingsPct] ?: 20,
            currency = p[K.currency] ?: "USD",
            themeMode = runCatching { ThemeMode.valueOf(p[K.theme] ?: "SYSTEM") }
                .getOrDefault(ThemeMode.SYSTEM),
            pinEnabled = p[K.pinEnabled] ?: false,
            pinHash = p[K.pinHash] ?: "",
            pinSalt = p[K.pinSalt] ?: "",
            survivalCents = p[K.survival] ?: 0L,
            babyStepsDone = (p[K.babySteps] ?: "")
                .split(',')
                .mapNotNull { it.trim().toIntOrNull() }
                .toSet()
        )
    }

    suspend fun finishOnboarding(name: String, incomeCents: Long, currency: String) =
        context.citadelStore.edit {
            it[K.onboarded] = true
            it[K.userName] = name.trim()
            it[K.income] = incomeCents.coerceAtLeast(0)
            it[K.currency] = currency
        }

    suspend fun setName(name: String) = context.citadelStore.edit { it[K.userName] = name.trim() }

    suspend fun setIncome(cents: Long) =
        context.citadelStore.edit { it[K.income] = cents.coerceAtLeast(0) }

    suspend fun setSplit(needs: Int, wants: Int, savings: Int) = context.citadelStore.edit {
        it[K.needsPct] = needs
        it[K.wantsPct] = wants
        it[K.savingsPct] = savings
    }

    suspend fun setCurrency(code: String) =
        context.citadelStore.edit { it[K.currency] = code }

    suspend fun setTheme(mode: ThemeMode) =
        context.citadelStore.edit { it[K.theme] = mode.name }

    suspend fun setSurvival(cents: Long) =
        context.citadelStore.edit { it[K.survival] = cents.coerceAtLeast(0) }

    suspend fun setPin(pin: String) = context.citadelStore.edit {
        val salt = UUID.randomUUID().toString()
        it[K.pinSalt] = salt
        it[K.pinHash] = hash(salt, pin)
        it[K.pinEnabled] = true
    }

    suspend fun clearPin() = context.citadelStore.edit {
        it[K.pinEnabled] = false
        it[K.pinHash] = ""
        it[K.pinSalt] = ""
    }

    suspend fun toggleBabyStep(step: Int, done: Boolean) = context.citadelStore.edit {
        val current = (it[K.babySteps] ?: "")
            .split(',')
            .mapNotNull { s -> s.toIntOrNull() }
            .toSet()
        it[K.babySteps] = (if (done) current + step else current - step)
            .sorted()
            .joinToString(",")
    }

    suspend fun eraseAll() = context.citadelStore.edit { it.clear() }

    companion object {
        fun hash(salt: String, pin: String): String =
            MessageDigest.getInstance("SHA-256")
                .digest("$salt:$pin".toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
    }
}
