package com.as9929.display.netease

import net.fabricmc.api.ModInitializer
import org.slf4j.LoggerFactory

object NeteaseMusicDisplay : ModInitializer {
	const val MOD_ID = "netease-music-display"
    private val logger = LoggerFactory.getLogger(MOD_ID)

	override fun onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
		logger.info("$MOD_ID loaded.")
	}
}