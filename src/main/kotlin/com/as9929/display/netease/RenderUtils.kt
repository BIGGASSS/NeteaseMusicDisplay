package com.as9929.display.netease

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component

object RenderUtils {

    fun drawScrollingText(
        context: GuiGraphicsExtractor,
        text: Component,
        x: Int,
        y: Int,
        maxWidth: Int,
        color: Int
    ) {
        val client = Minecraft.getInstance()
        val textRenderer = client.font
        val textWidth = textRenderer.width(text)

        // 1. Static Text (No Scroll needed)
        if (textWidth <= maxWidth) {
            context.text(textRenderer, text, x, y, color, true)
            return
        }

        // 2. Setup Scissor (The "Crop" Box)
        context.enableScissor(x, y, x + maxWidth, y + textRenderer.lineHeight)

        // 3. Smooth Math
        val speed = 30.0 // Pixels per second
        val gap = 40.0   // Space between loops
        val fullScrollWidth = textWidth + gap

        // Use high-precision floating point time
        val time = System.currentTimeMillis() / 1000.0
        val offset = (time * speed) % fullScrollWidth

        // --- KEY FIX: Use Matrix Translation instead of changing X ---

        // Draw 1st Copy
        context.pose().pushMatrix()
        // We translate the ENTIRE coordinate system to the left by 'offset'
        context.pose().translate(-offset.toFloat(), 0f)
        // We draw at 'x' (which is now shifted left by the matrix)
        context.text(textRenderer, text, x, y, color, true)
        context.pose().popMatrix()

        // Draw 2nd Copy (The loop)
        // We only draw this if the first copy has moved far enough left
        if (x - offset + textWidth < x + maxWidth) {
            context.pose().pushMatrix()
            // Translate left by offset, but push right by fullScrollWidth for the loop
            context.pose().translate((fullScrollWidth - offset).toFloat(), 0f)
            context.text(textRenderer, text, x, y, color, true)
            context.pose().popMatrix()
        }

        context.disableScissor()
    }
}
