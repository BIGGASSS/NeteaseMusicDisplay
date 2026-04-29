package com.as9929.display.netease

import com.as9929.display.netease.config.ConfigManager
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object TextRender {
    @Volatile
    private var cachedTitle: String? = null

    // Create a single background thread to handle the heavy querying
    private val executor = Executors.newSingleThreadScheduledExecutor { r ->
        val t = Thread(r, "NeteaseMusicQueryThread")
        t.isDaemon = true // Ensure thread closes when the game closes
        t
    }

    init {
        // Schedule the query to run every 1 second (initial delay 0)
        executor.scheduleAtFixedRate({
            try {
                // This heavy work happens off the main thread
                val musicTitle = CloudMusicHelper.getCloudMusicTitle()
                cachedTitle = musicTitle
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, 0, 1, TimeUnit.SECONDS)
    }

    fun onRender(context: GuiGraphicsExtractor) {
        val config = ConfigManager.config
        
        // 0. Check if enabled
        if (!config.enabled) return
        
        // 1. Instant check: If title is null (e.g. not Windows or not playing), stop rendering.
        val titleToRender = cachedTitle ?: return

        val client = Minecraft.getInstance()
        val textRenderer = client.font

        // 2. Define the text
        val text = Component.literal("Playing: $titleToRender")

        // 3. Calculate Dimensions (Standard Rendering Logic)
        val scale = config.scale
        val screenWidth = client.window.guiScaledWidth / scale
        val maxBoxWidth = config.maxBoxWidth
        val padding = 10
        val fullTextWidth = textRenderer.width(text)
        val currentBoxWidth = fullTextWidth.coerceAtMost(maxBoxWidth)

        // Calculate X position: use config if not -1, otherwise auto right-align
        val x = if (config.x == -1) {
            (screenWidth - currentBoxWidth - padding).toInt()
        } else {
            config.x
        }
        val y = config.y
        
        // Get color from config
        val color = ConfigManager.getColorInt()

        // 4. Render
        context.pose().pushMatrix()
        context.pose().scale(scale, scale)

        RenderUtils.drawScrollingText(context, text, x, y, currentBoxWidth, color)

        context.pose().popMatrix()
    }
}
