package com.as9929.display.netease.config

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class ModConfigSerializationTest {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    @Test
    fun loadsDefaultLinuxKeywordWhenMissingInConfig() {
        val config = json.decodeFromString<ModConfig>("{}")

        assertEquals("electron", config.linuxMprisProcessKeyword)
    }

    @Test
    fun persistsConfiguredLinuxKeyword() {
        val encoded = json.encodeToString(ModConfig(linuxMprisProcessKeyword = "chrome"))
        val value = Json.parseToJsonElement(encoded)
            .jsonObject["linuxMprisProcessKeyword"]
            ?.jsonPrimitive
            ?.content

        assertEquals("chrome", value)
    }
}
