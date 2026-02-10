package com.as9929.display.netease

import com.as9929.display.netease.command.ConfigCommand
import com.as9929.display.netease.config.ConfigManager
import net.fabricmc.api.ModInitializer
import org.slf4j.LoggerFactory

object NeteaseMusicDisplay : ModInitializer {
	const val MOD_ID = "netease-music-display"
    private val logger = LoggerFactory.getLogger(MOD_ID)

	override fun onInitialize() {
		// Load configuration
		ConfigManager.loadConfig()
		logger.info("$MOD_ID config loaded.")
		
		// Register commands
		ConfigCommand.register()
		logger.info("$MOD_ID commands registered.")
		
		logger.info("$MOD_ID loaded.")
	}
}