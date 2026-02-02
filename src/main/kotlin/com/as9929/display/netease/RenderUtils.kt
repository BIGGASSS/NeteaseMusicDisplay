package com.as9929.display.netease

import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.DrawContext
import net.minecraft.text.Text

object RenderUtils {

    fun drawScrollingText(
        context: DrawContext,
        text: Text,
        x: Int,
        y: Int,
        maxWidth: Int,
        color: Int
    ) {
        val client = MinecraftClient.getInstance()
        val textRenderer = client.textRenderer
        val textWidth = textRenderer.getWidth(text)

        // 1. Static Text (No Scroll needed)
        if (textWidth <= maxWidth) {
            context.drawText(textRenderer, text, x, y, color, true)
            return
        }

        // 2. Setup Scissor (The "Crop" Box)
        val window = client.window
        val scale = window.scaleFactor
        val scissorX = (x * scale).toInt()
        val scissorY = (window.framebufferHeight - (y + textRenderer.fontHeight) * scale).toInt()
        val scissorW = (maxWidth * scale).toInt()
        val scissorH = (textRenderer.fontHeight * scale).toInt()

        context.enableScissor(x, y, x + maxWidth, y + textRenderer.fontHeight)

        // 3. Smooth Math
        val speed = 30.0 // Pixels per second
        val gap = 40.0   // Space between loops
        val fullScrollWidth = textWidth + gap

        // Use high-precision floating point time
        val time = System.currentTimeMillis() / 1000.0
        val offset = (time * speed) % fullScrollWidth

        // --- KEY FIX: Use Matrix Translation instead of changing X ---

        // Draw 1st Copy
        context.matrices.pushMatrix()
        // We translate the ENTIRE coordinate system to the left by 'offset'
        context.matrices.translate(-offset.toFloat(), 0f)
        // We draw at 'x' (which is now shifted left by the matrix)
        context.drawText(textRenderer, text, x, y, color, true)
        context.matrices.popMatrix()

        // Draw 2nd Copy (The loop)
        // We only draw this if the first copy has moved far enough left
        if (x - offset + textWidth < x + maxWidth) {
            context.matrices.pushMatrix()
            // Translate left by offset, but push right by fullScrollWidth for the loop
            context.matrices.translate((fullScrollWidth - offset).toFloat(), 0f)
            context.drawText(textRenderer, text, x, y, color, true)
            context.matrices.popMatrix()
        }

        context.disableScissor()
    }
}