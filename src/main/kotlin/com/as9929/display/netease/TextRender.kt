package com.as9929.display.netease

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.text.Text
import java.awt.Color
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object TextRender {
    private var scale = 1.0f
    private val color = Color(255, 255, 0, 255).rgb // White

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
                // This heavy work now happens off the main thread
                val musicTitle = CloudMusicHelper.getCloudMusicTitle()
                cachedTitle = musicTitle
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, 0, 1, TimeUnit.SECONDS)
    }

    fun onRender(context: DrawContext) {
        // 1. Instant check: If title is null (e.g. not Windows or not playing), stop rendering.
        // We read the variable directly; no heavy calculation here.
        val titleToRender = cachedTitle ?: return

        val client = MinecraftClient.getInstance()
        val textRenderer = client.textRenderer

        // 2. Define your text
        val text = Text.literal("Playing: $titleToRender")

        // 3. Calculate Dimensions (Standard Rendering Logic)
        val screenWidth = client.window.scaledWidth / scale
        val maxBoxWidth = 200
        val padding = 10
        val fullTextWidth = textRenderer.getWidth(text)
        val currentBoxWidth = fullTextWidth.coerceAtMost(maxBoxWidth)

        val x = (screenWidth - currentBoxWidth - padding).toInt()
        val y = 3

        // 4. Render
        context.matrices.pushMatrix()
        context.matrices.scale(scale, scale)

        RenderUtils.drawScrollingText(context, text, x, y, currentBoxWidth, color)

        context.matrices.popMatrix()
    }
}