package com.as9929.display.netease.config

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.fabricmc.loader.api.FabricLoader
import org.slf4j.LoggerFactory
import java.io.File

@Serializable
data class ModConfig(
    val enabled: Boolean = true,
    val x: Int = -1,              // -1 = auto (right-aligned), otherwise absolute position
    val y: Int = 3,               // absolute Y position from top
    val colorHex: String = "#FFFF00",  // Yellow default (matches hardcoded color)
    val maxBoxWidth: Int = 200,   // Matches hardcoded maxBoxWidth
    val scale: Float = 1.0f       // Matches hardcoded scale
) {
    companion object {
        const val DEFAULT_X = -1
        const val DEFAULT_Y = 3
        const val DEFAULT_COLOR = "#FFFF00"
        const val DEFAULT_MAX_WIDTH = 200
        const val DEFAULT_SCALE = 1.0f
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

    @Volatile
    private var _config: ModConfig = loadConfig()
    
    // Cache the parsed color for performance
    @Volatile
    private var _cachedColor: Int? = null

    val config: ModConfig
        get() = _config

    fun loadConfig(): ModConfig {
        return try {
            if (configFile.exists()) {
                val content = configFile.readText()
                val config = json.decodeFromString<ModConfig>(content)
                _cachedColor = parseColor(config.colorHex)
                config
            } else {
                ModConfig().also { saveConfig(it) }
            }
        } catch (e: Exception) {
            logger.error("Failed to load config, using defaults", e)
            _cachedColor = 0xFFFF00
            ModConfig()
        }
    }

    fun saveConfig(config: ModConfig = _config) {
        // Offload to background thread to avoid blocking the main thread
        Thread {
            try {
                if (!configDir.exists()) {
                    configDir.mkdirs()
                }
                configFile.writeText(json.encodeToString(config))
                logger.info("Config saved successfully")
            } catch (e: Exception) {
                logger.error("Failed to save config", e)
            }
        }.apply {
            isDaemon = true
            name = "NeteaseMusicDisplay-ConfigSave"
            start()
        }
    }

    fun updateConfig(newConfig: ModConfig) {
        _config = newConfig
        _cachedColor = parseColor(newConfig.colorHex)
        saveConfig(newConfig)
    }

    fun resetToDefaults() {
        updateConfig(ModConfig())
    }

    // Thread-safe getter for cached color
    fun getColorInt(): Int {
        return _cachedColor ?: 0xFFFF00
    }

    private fun parseColor(hex: String): Int {
        return try {
            var colorString = hex.trim()
            if (colorString.startsWith("#")) {
                colorString = colorString.substring(1)
            }
            colorString.toInt(16) or 0xFF000000.toInt() // Add alpha
        } catch (e: Exception) {
            0xFFFF00 // Fallback to yellow
        }
    }
}