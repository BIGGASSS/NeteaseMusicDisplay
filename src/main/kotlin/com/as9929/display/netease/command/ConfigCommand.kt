package com.as9929.display.netease.command

import com.as9929.display.netease.config.ConfigManager
import com.as9929.display.netease.config.ModConfig
import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.FloatArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.client.MinecraftClient
import net.minecraft.text.Text

object ConfigCommand {
    private val COLOR_MAP = mapOf(
        "black" to "#000000",
        "dark_blue" to "#0000AA",
        "dark_green" to "#00AA00",
        "dark_aqua" to "#00AAAA",
        "dark_red" to "#AA0000",
        "dark_purple" to "#AA00AA",
        "gold" to "#FFAA00",
        "gray" to "#AAAAAA",
        "dark_gray" to "#555555",
        "blue" to "#5555FF",
        "green" to "#55FF55",
        "aqua" to "#55FFFF",
        "red" to "#FF5555",
        "light_purple" to "#FF55FF",
        "yellow" to "#FFFF55",
        "white" to "#FFFFFF"
    )

    // Maximum Y position - prevents rendering off-screen
    private fun getMaxY(): Int {
        val window = MinecraftClient.getInstance().window
        return (window.scaledHeight - 20).coerceAtLeast(0)
    }

    fun register() {
        ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
            registerCommands(dispatcher)
        }
    }

    private fun registerCommands(dispatcher: CommandDispatcher<FabricClientCommandSource>) {
        dispatcher.register(
            ClientCommandManager.literal("nmd")
                .then(
                    ClientCommandManager.literal("color")
                        .then(
                            ClientCommandManager.argument("value", StringArgumentType.string())
                                .executes { ctx -> setColor(ctx, StringArgumentType.getString(ctx, "value")) }
                        )
                )
                .then(
                    ClientCommandManager.literal("pos")
                        .then(
                            ClientCommandManager.argument("x", IntegerArgumentType.integer(-1))
                                .then(
                                    ClientCommandManager.argument("y", IntegerArgumentType.integer(0))
                                        .executes { ctx ->
                                            val x = IntegerArgumentType.getInteger(ctx, "x")
                                            val y = IntegerArgumentType.getInteger(ctx, "y")
                                            setPosition(ctx, x, y)
                                        }
                                )
                        )
                )
                .then(
                    ClientCommandManager.literal("width")
                        .then(
                            ClientCommandManager.argument("pixels", IntegerArgumentType.integer(50, 500))
                                .executes { ctx ->
                                    val width = IntegerArgumentType.getInteger(ctx, "pixels")
                                    setWidth(ctx, width)
                                }
                        )
                )
                .then(
                    ClientCommandManager.literal("scale")
                        .then(
                            ClientCommandManager.argument("value", FloatArgumentType.floatArg(0.5f, 3.0f))
                                .executes { ctx ->
                                    val scale = FloatArgumentType.getFloat(ctx, "value")
                                    setScale(ctx, scale)
                                }
                        )
                )
                .then(
                    ClientCommandManager.literal("toggle")
                        .executes { ctx -> toggle(ctx) }
                )
                .then(
                    ClientCommandManager.literal("reset")
                        .executes { ctx -> reset(ctx) }
                )
                .then(
                    ClientCommandManager.literal("status")
                        .executes { ctx -> status(ctx) }
                )
        )
    }

    private fun setColor(ctx: CommandContext<FabricClientCommandSource>, value: String): Int {
        val config = ConfigManager.config
        
        // Check if it's a named color first
        val hexValue = COLOR_MAP[value.lowercase()] ?: run {
            // Validate hex format
            var hex = value.trim()
            if (hex.startsWith("#")) {
                hex = hex.substring(1)
            }
            
            // Validate hex length and characters
            if (hex.length != 6 || !hex.all { it.isDigit() || it in 'A'..'F' || it in 'a'..'f' }) {
                ctx.source.sendError(Text.literal("Invalid color. Use hex code (e.g., #FF5733) or color name (e.g., red, yellow)"))
                return 0
            }
            
            "#$hex"
        }
        
        // Create new immutable config with updated values
        val newConfig = config.copy(colorHex = hexValue)
        ConfigManager.updateConfig(newConfig)
        
        ctx.source.sendFeedback(Text.literal("§aMusic display color set to: $hexValue"))
        return 1
    }

    private fun setPosition(ctx: CommandContext<FabricClientCommandSource>, x: Int, y: Int): Int {
        // Validate Y position to prevent off-screen rendering
        val maxY = getMaxY()
        val clampedY = y.coerceAtMost(maxY)
        
        if (y > maxY) {
            ctx.source.sendFeedback(Text.literal("§eY position clamped from $y to $maxY to prevent off-screen rendering"))
        }
        
        val config = ConfigManager.config
        val newConfig = config.copy(x = x, y = clampedY)
        ConfigManager.updateConfig(newConfig)
        
        val xText = if (x == -1) "auto (right-aligned)" else x.toString()
        ctx.source.sendFeedback(Text.literal("§aMusic display position set to: X=$xText, Y=$clampedY"))
        return 1
    }

    private fun setWidth(ctx: CommandContext<FabricClientCommandSource>, width: Int): Int {
        val config = ConfigManager.config
        val newConfig = config.copy(maxBoxWidth = width)
        ConfigManager.updateConfig(newConfig)
        
        ctx.source.sendFeedback(Text.literal("§aMusic display max width set to: $width pixels"))
        return 1
    }

    private fun setScale(ctx: CommandContext<FabricClientCommandSource>, scale: Float): Int {
        val config = ConfigManager.config
        val newConfig = config.copy(scale = scale)
        ConfigManager.updateConfig(newConfig)
        
        ctx.source.sendFeedback(Text.literal("§aMusic display scale set to: $scale"))
        return 1
    }

    private fun toggle(ctx: CommandContext<FabricClientCommandSource>): Int {
        val config = ConfigManager.config
        val newConfig = config.copy(enabled = !config.enabled)
        ConfigManager.updateConfig(newConfig)
        
        val status = if (newConfig.enabled) "§aenabled" else "§cdisabled"
        ctx.source.sendFeedback(Text.literal("Music display is now $status"))
        return 1
    }

    private fun reset(ctx: CommandContext<FabricClientCommandSource>): Int {
        ConfigManager.resetToDefaults()
        ctx.source.sendFeedback(Text.literal("§aMusic display settings reset to defaults"))
        return 1
    }

    private fun status(ctx: CommandContext<FabricClientCommandSource>): Int {
        val config = ConfigManager.config
        
        val xText = if (config.x == -1) "auto" else config.x.toString()
        val status = if (config.enabled) "§aenabled" else "§cdisabled"
        
        ctx.source.sendFeedback(Text.literal("§6=== Netease Music Display Status ==="))
        ctx.source.sendFeedback(Text.literal("Status: $status"))
        ctx.source.sendFeedback(Text.literal("Position: X=$xText, Y=${config.y}"))
        ctx.source.sendFeedback(Text.literal("Color: ${config.colorHex}"))
        ctx.source.sendFeedback(Text.literal("Max Width: ${config.maxBoxWidth}px"))
        ctx.source.sendFeedback(Text.literal("Scale: ${config.scale}"))
        
        return 1
    }
}