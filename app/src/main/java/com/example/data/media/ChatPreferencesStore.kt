package com.example.data.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

private val Context.chatPrefsDataStore by preferencesDataStore(name = "weconnect_chat_ui_prefs")

enum class WallpaperPreset(
    val id: String,
    val label: String,
    val subtitle: String,
    val forceDarkScrim: Boolean = false
) {
    CLASSIC_DOODLE("CLASSIC_DOODLE", "Classic Doodle", "Iconic woven chat canvas"),
    EMERALD_AURORA("EMERALD_AURORA", "Emerald Aurora", "Glowing northern teal waves", forceDarkScrim = true),
    MIDNIGHT_COSMOS("MIDNIGHT_COSMOS", "Midnight Cosmos", "Starry slate constellations", forceDarkScrim = true),
    SUNSET_TERRACOTTA("SUNSET_TERRACOTTA", "Sunset Dunes", "Warm coral & amber dusk", forceDarkScrim = true),
    BOTANICAL_MINT("BOTANICAL_MINT", "Botanical Sage", "Fresh organic seafoam arcs"),
    ROSE_QUARTZ("ROSE_QUARTZ", "Rose Twilight", "Soft blush & plum shimmer"),
    AMOLED_CYBER("AMOLED_CYBER", "Obsidian Grid", "Ultra-sleek stealth mesh", forceDarkScrim = true),
    CUSTOM_PHOTO("CUSTOM_PHOTO", "Custom Photo", "Your gallery wallpaper")
}

data class ChatWallpaperConfig(
    val preset: WallpaperPreset = WallpaperPreset.CLASSIC_DOODLE,
    val customPhotoPath: String? = null,
    val dimAlpha: Float = 0.15f,
    val isPerChatOverride: Boolean = false
)

object ChatPreferencesStore {

    private val KEY_GLOBAL_PRESET = stringPreferencesKey("global_wallpaper_preset")
    private val KEY_GLOBAL_PHOTO = stringPreferencesKey("global_wallpaper_photo")
    private val KEY_GLOBAL_DIM = floatPreferencesKey("global_wallpaper_dim")

    private fun presetKey(chatId: String) = stringPreferencesKey("chat_preset_$chatId")
    private fun photoKey(chatId: String) = stringPreferencesKey("chat_photo_$chatId")
    private fun dimKey(chatId: String) = floatPreferencesKey("chat_dim_$chatId")
    private fun starredKey(chatId: String) = stringSetPreferencesKey("chat_starred_$chatId")

    fun observeChatWallpaper(context: Context, chatId: String): Flow<ChatWallpaperConfig> {
        return context.chatPrefsDataStore.data.map { prefs ->
            val chatPresetId = prefs[presetKey(chatId)]
            val isOverride = chatPresetId != null
            val activePresetId = chatPresetId ?: prefs[KEY_GLOBAL_PRESET]
            val preset = WallpaperPreset.entries.firstOrNull { it.id == activePresetId }
                ?: WallpaperPreset.CLASSIC_DOODLE

            val photoPath = if (isOverride) prefs[photoKey(chatId)] else prefs[KEY_GLOBAL_PHOTO]
            val dim = (if (isOverride) prefs[dimKey(chatId)] else prefs[KEY_GLOBAL_DIM]) ?: 0.15f

            ChatWallpaperConfig(
                preset = preset,
                customPhotoPath = photoPath,
                dimAlpha = dim.coerceIn(0f, 0.75f),
                isPerChatOverride = isOverride
            )
        }
    }

    suspend fun setPresetWallpaper(
        context: Context,
        chatId: String,
        preset: WallpaperPreset,
        applyToAllChats: Boolean
    ) {
        context.chatPrefsDataStore.edit { prefs ->
            if (applyToAllChats) {
                prefs[KEY_GLOBAL_PRESET] = preset.id
                prefs.remove(presetKey(chatId))
                if (preset != WallpaperPreset.CUSTOM_PHOTO) {
                    prefs.remove(KEY_GLOBAL_PHOTO)
                    prefs.remove(photoKey(chatId))
                }
            } else {
                prefs[presetKey(chatId)] = preset.id
                if (preset != WallpaperPreset.CUSTOM_PHOTO) {
                    prefs.remove(photoKey(chatId))
                }
            }
        }
    }

    suspend fun setCustomPhotoWallpaper(
        context: Context,
        chatId: String,
        uri: Uri,
        applyToAllChats: Boolean
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val input = context.contentResolver.openInputStream(uri) ?: return@withContext false
            val original = input.use { BitmapFactory.decodeStream(it) } ?: return@withContext false

            val maxDim = 1280
            val scale = minOf(maxDim.toFloat() / original.width, maxDim.toFloat() / original.height, 1f)
            val targetW = (original.width * scale).toInt().coerceAtLeast(1)
            val targetH = (original.height * scale).toInt().coerceAtLeast(1)
            val scaled = if (targetW != original.width || targetH != original.height) {
                Bitmap.createScaledBitmap(original, targetW, targetH, true)
            } else original

            val wallDir = File(context.filesDir, "wallpapers")
            if (!wallDir.exists()) wallDir.mkdirs()
            val targetName = if (applyToAllChats) "global_wall_${System.currentTimeMillis()}.jpg" else "chat_${chatId}_${System.currentTimeMillis()}.jpg"
            val outFile = File(wallDir, targetName)
            FileOutputStream(outFile).use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            if (scaled != original) scaled.recycle()

            context.chatPrefsDataStore.edit { prefs ->
                if (applyToAllChats) {
                    prefs[KEY_GLOBAL_PRESET] = WallpaperPreset.CUSTOM_PHOTO.id
                    prefs[KEY_GLOBAL_PHOTO] = outFile.absolutePath
                    prefs.remove(presetKey(chatId))
                    prefs.remove(photoKey(chatId))
                } else {
                    prefs[presetKey(chatId)] = WallpaperPreset.CUSTOM_PHOTO.id
                    prefs[photoKey(chatId)] = outFile.absolutePath
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun setWallpaperDim(
        context: Context,
        chatId: String,
        dimAlpha: Float,
        applyToAllChats: Boolean
    ) {
        val clamped = dimAlpha.coerceIn(0f, 0.75f)
        context.chatPrefsDataStore.edit { prefs ->
            if (applyToAllChats) {
                prefs[KEY_GLOBAL_DIM] = clamped
                prefs.remove(dimKey(chatId))
            } else {
                prefs[dimKey(chatId)] = clamped
            }
        }
    }

    suspend fun resetChatWallpaper(context: Context, chatId: String) {
        context.chatPrefsDataStore.edit { prefs ->
            prefs.remove(presetKey(chatId))
            prefs.remove(photoKey(chatId))
            prefs.remove(dimKey(chatId))
        }
    }

    fun observeStarredMessageIds(context: Context, chatId: String): Flow<Set<String>> {
        return context.chatPrefsDataStore.data.map { prefs ->
            prefs[starredKey(chatId)] ?: emptySet()
        }
    }

    suspend fun toggleStarMessage(context: Context, chatId: String, messageId: String): Boolean {
        var nowStarred = false
        context.chatPrefsDataStore.edit { prefs ->
            val current = prefs[starredKey(chatId)] ?: emptySet()
            val next = if (messageId in current) {
                nowStarred = false
                current - messageId
            } else {
                nowStarred = true
                current + messageId
            }
            prefs[starredKey(chatId)] = next
        }
        return nowStarred
    }
}
