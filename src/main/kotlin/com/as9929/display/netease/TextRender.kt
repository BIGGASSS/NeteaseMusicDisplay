package com.as9929.display.netease

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.text.Text
import java.awt.Color

object TextRender {
    private var scale = 1.0f
    private val color = Color(255, 255, 0, 255).rgb // White
    private var cachedTitle: String = "No Music"
    private var tickCounter = 0

    fun onRender(context: DrawContext) {
        if (CloudMusicHelper.getCloudMusicTitle() == null) return
        tickCounter++
        if (tickCounter >= 20) { // Update every 1 second (20 ticks)
            tickCounter = 0
            // Run in background thread if you notice lag, though usually this is fast enough
            val musicTitle = CloudMusicHelper.getCloudMusicTitle()
            cachedTitle = musicTitle ?: "No Music Playing"
        }

        val client = MinecraftClient.getInstance()
        val textRenderer = client.textRenderer

        // 1. Define your text first (needed to calculate width)
        val text = Text.literal("Playing: $cachedTitle")

        // 2. Calculate the Right Edge
        // We divide scaledWidth by your 'scale' var to match the matrix scaling
        val screenWidth = client.window.scaledWidth / scale
        val maxBoxWidth = 200
        val padding = 10 // Space from the edge
        val fullTextWidth = textRenderer.getWidth(text)
        val currentBoxWidth = fullTextWidth.coerceAtMost(maxBoxWidth)

        // 3. Calculate Coordinates
        // X = Far Right - Text Size - Padding
        val x = (screenWidth - currentBoxWidth - padding).toInt()
        val y = 3 // Distance from top

        // 4. Render
        context.matrices.pushMatrix()
        context.matrices.scale(scale, scale) // 2 args for 1.21.10

        RenderUtils.drawScrollingText(context, text, x, y, currentBoxWidth, color)

        context.matrices.popMatrix()
    }
}