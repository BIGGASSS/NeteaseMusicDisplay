package com.as9929.display.netease.config

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.fabricmc.loader.api.FabricLoader
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.Executors

@Serializable
data class ModConfig(
    val enabled: Boolean = true,
    val x: Int = -1,              // -1 = auto (right-aligned), otherwise absolute position
    val y: Int = 3,               // absolute Y position from top
    val colorHex: String = "#FFFF00",  // Yellow default (matches hardcoded color)
    val maxBoxWidth: Int = 200,   // Matches hardcoded maxBoxWidth
    val scale: Float = 1.0f,      // Matches hardcoded scale
    val linuxMprisProcessKeyword: String = "open-orpheus"
) {
    companion object {
        const val DEFAULT_X = -1
        const val DEFAULT_Y = 3
        const val DEFAULT_COLOR = "#FFFF00"
        const val DEFAULT_MAX_WIDTH = 200
        const val DEFAULT_SCALE = 1.0f
        const val DEFAULT_LINUX_MPRIS_KEYWORD = "open-orpheus"

        /** Opaque yellow (ARGB), used when the configured color cannot be parsed. */
        val FALLBACK_COLOR: Int = 0xFFFFFF00.toInt()

        private val HEX_COLOR_REGEX = Regex("^[0-9a-fA-F]{6}$")

        /** Parses a "#RRGGBB" or "RRGGBB" hex string into an opaque ARGB int. */
        fun parseColor(hex: String): Int {
            val normalized = hex.trim().removePrefix("#")
            if (!HEX_COLOR_REGEX.matches(normalized)) {
                return FALLBACK_COLOR
            }
            return normalized.toInt(16) or 0xFF000000.toInt()
        }
    }
}

object ConfigManager {
    private val logger = LoggerFactory.getLogger("netease-music-display")
    private val configDir = FabricLoader.getInstance().configDir.toFile()
    private val configFile = File(configDir, "netease-music-display.json")
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    // Single-threaded executor so saves are serialized in submission order,
    // preventing an older config from overwriting a newer one.
    // Declared before _config so it is initialized before loadConfig() may use it.
    private val saveExecutor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "NeteaseMusicDisplay-ConfigSave").apply { isDaemon = true }
    }

    // Cache the parsed color for performance. Initialized before _config so that
    // the value assigned during loadConfig() is not reset by this initializer.
    @Volatile
    private var _cachedColor: Int = ModConfig.FALLBACK_COLOR

    @Volatile
    private var _config: ModConfig = loadConfig()

    val config: ModConfig
        get() = _config

    fun loadConfig(): ModConfig {
        return try {
            if (configFile.exists()) {
                val content = configFile.readText()
                val config = json.decodeFromString<ModConfig>(content)
                _cachedColor = ModConfig.parseColor(config.colorHex)
                config
            } else {
                ModConfig().also {
                    _cachedColor = ModConfig.parseColor(it.colorHex)
                    saveConfig(it)
                }
            }
        } catch (e: Exception) {
            logger.error("Failed to load config, using defaults", e)
            _cachedColor = ModConfig.parseColor(ModConfig.DEFAULT_COLOR)
            ModConfig()
        }
    }

    fun saveConfig(config: ModConfig = _config) {
        // Offload to background thread to avoid blocking the main thread
        saveExecutor.execute {
            try {
                if (!configDir.exists()) {
                    configDir.mkdirs()
                }
                configFile.writeText(json.encodeToString(config))
                logger.info("Config saved successfully")
            } catch (e: Exception) {
                logger.error("Failed to save config", e)
            }
        }
    }

    fun updateConfig(newConfig: ModConfig) {
        _config = newConfig
        _cachedColor = ModConfig.parseColor(newConfig.colorHex)
        saveConfig(newConfig)
    }

    fun resetToDefaults() {
        updateConfig(ModConfig())
    }

    // Thread-safe getter for cached color
    fun getColorInt(): Int {
        return _cachedColor
    }
}
