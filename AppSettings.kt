package com.iosexperience.app

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.store by preferencesDataStore(name = "ios_experience_settings")

data class AppSettings(
    val iosIcons: Boolean = true,
    val glass: Boolean = true,
    val animations: Boolean = true,
    val haptics: Boolean = true,
    val cornerRadius: Float = 14f,
    val columns: Int = 4,
    val glassOpacity: Float = 0.25f,
    val dock: Set<String> = emptySet(),
    val islandEnabled: Boolean = false,
    val islandTopOffset: Float = 8f,
)

const val MAX_DOCK = 4

class SettingsRepository(private val context: Context) {
    private object K {
        val icons = booleanPreferencesKey("ios_icons")
        val glass = booleanPreferencesKey("glass")
        val anim = booleanPreferencesKey("animations")
        val haptics = booleanPreferencesKey("haptics")
        val radius = floatPreferencesKey("corner_radius")
        val columns = intPreferencesKey("columns")
        val opacity = floatPreferencesKey("glass_opacity")
        val dock = stringSetPreferencesKey("dock")
        val island = booleanPreferencesKey("island_enabled")
        val islandOffset = floatPreferencesKey("island_top_offset")
    }

    private val d = AppSettings()

    val settings: Flow<AppSettings> = context.store.data.map { p ->
        AppSettings(
            iosIcons = p[K.icons] ?: d.iosIcons,
            glass = p[K.glass] ?: d.glass,
            animations = p[K.anim] ?: d.animations,
            haptics = p[K.haptics] ?: d.haptics,
            cornerRadius = p[K.radius] ?: d.cornerRadius,
            columns = p[K.columns] ?: d.columns,
            glassOpacity = p[K.opacity] ?: d.glassOpacity,
            dock = p[K.dock] ?: d.dock,
            islandEnabled = p[K.island] ?: d.islandEnabled,
            islandTopOffset = p[K.islandOffset] ?: d.islandTopOffset,
        )
    }

    suspend fun setIosIcons(v: Boolean) { context.store.edit { it[K.icons] = v } }
    suspend fun setGlass(v: Boolean) { context.store.edit { it[K.glass] = v } }
    suspend fun setAnimations(v: Boolean) { context.store.edit { it[K.anim] = v } }
    suspend fun setHaptics(v: Boolean) { context.store.edit { it[K.haptics] = v } }
    suspend fun setCornerRadius(v: Float) { context.store.edit { it[K.radius] = v } }
    suspend fun setColumns(v: Int) { context.store.edit { it[K.columns] = v } }
    suspend fun setGlassOpacity(v: Float) { context.store.edit { it[K.opacity] = v } }

    suspend fun setIslandEnabled(v: Boolean) { context.store.edit { it[K.island] = v } }
    suspend fun setIslandTopOffset(v: Float) { context.store.edit { it[K.islandOffset] = v } }

    suspend fun toggleDock(pkg: String) {
        context.store.edit { p ->
            val cur: Set<String> = p[K.dock] ?: emptySet()
            p[K.dock] = when {
                pkg in cur -> cur - pkg
                cur.size < MAX_DOCK -> cur + pkg
                else -> cur
            }
        }
    }
}
