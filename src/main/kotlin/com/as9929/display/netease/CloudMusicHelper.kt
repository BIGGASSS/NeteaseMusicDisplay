package com.as9929.display.netease

import com.as9929.display.netease.config.ConfigManager
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.ptr.IntByReference
import com.sun.jna.win32.StdCallLibrary
import java.util.concurrent.TimeUnit

object CloudMusicHelper {

    private interface MyUser32 : StdCallLibrary {
        fun EnumWindows(lpEnumFunc: Callback, data: Pointer?): Boolean
        fun GetWindowThreadProcessId(hWnd: Pointer, lpdwProcessId: IntByReference): Int
        fun GetWindowTextW(hWnd: Pointer, lpString: CharArray, nMaxCount: Int): Int
        fun IsWindowVisible(hWnd: Pointer): Boolean
        fun GetWindowTextLengthW(hWnd: Pointer): Int

        interface Callback : com.sun.jna.Callback {
            fun invoke(hWnd: Pointer, data: Pointer?): Boolean
        }
    }

    private interface MyKernel32 : StdCallLibrary {
        fun OpenProcess(dwDesiredAccess: Int, bInheritHandle: Boolean, dwProcessId: Int): Pointer?
        fun CloseHandle(hObject: Pointer): Boolean
        fun QueryFullProcessImageNameW(hProcess: Pointer, dwFlags: Int, lpExeName: CharArray, lpdwSize: IntByReference): Boolean
    }

    private val user32: MyUser32 by lazy {
        Native.load("user32", MyUser32::class.java)
    }

    private val kernel32: MyKernel32 by lazy {
        Native.load("kernel32", MyKernel32::class.java)
    }

    private const val PROCESS_QUERY_INFORMATION = 0x0400
    private const val PROCESS_VM_READ = 0x0010
    private const val BUSCTL_TIMEOUT_MS = 1000L
    private const val MPRIS_SERVICE_PREFIX = "org.mpris.MediaPlayer2."
    private const val MPRIS_OBJECT_PATH = "/org/mpris/MediaPlayer2"
    private const val MPRIS_PLAYER_INTERFACE = "org.mpris.MediaPlayer2.Player"

    internal data class LinuxMprisService(
        val serviceName: String,
        val processName: String
    )

    internal data class LinuxTrackMetadata(
        val title: String,
        val artist: String?
    )

    fun getCloudMusicTitle(): String? {
        val osName = System.getProperty("os.name").lowercase()
        return when {
            osName.contains("win") -> getWindowsCloudMusicTitle()
            osName.contains("linux") -> getLinuxMprisTitle(ConfigManager.config.linuxMprisProcessKeyword)
            else -> null
        }
    }

    private fun getWindowsCloudMusicTitle(): String? {
        var foundTitle: String? = null
        val targetProcess = "cloudmusic.exe"

        try {
            user32.EnumWindows(object : MyUser32.Callback {
                override fun invoke(hWnd: Pointer, data: Pointer?): Boolean {
                    if (foundTitle != null) return false

                    if (user32.IsWindowVisible(hWnd)) {
                        val pidRef = IntByReference()
                        user32.GetWindowThreadProcessId(hWnd, pidRef)
                        val pid = pidRef.value

                        if (getPName(pid).equals(targetProcess, ignoreCase = true)) {
                            val title = getWText(hWnd)
                            if (title.isNotEmpty() && title != "DesktopLyrics" && title != "GDI+ Window") {
                                foundTitle = title
                                return false
                            }
                        }
                    }
                    return true
                }
            }, null)
        } catch (_: Throwable) {
            return null
        }

        return foundTitle
    }

    private fun getLinuxMprisTitle(processKeyword: String): String? {
        val servicesOutput = runBusctl("--user", "list", "--no-legend") ?: return null
        val candidates = parseLinuxMprisServices(servicesOutput, processKeyword)
        if (candidates.isEmpty()) return null

        val selectedService = selectLinuxMprisService(candidates) { service ->
            val statusOutput = runBusctl(
                "--user",
                "get-property",
                service.serviceName,
                MPRIS_OBJECT_PATH,
                MPRIS_PLAYER_INTERFACE,
                "PlaybackStatus"
            ) ?: return@selectLinuxMprisService null

            parsePlaybackStatus(statusOutput)
        } ?: return null

        val metadataOutput = runBusctl(
            "--user",
            "get-property",
            selectedService.serviceName,
            MPRIS_OBJECT_PATH,
            MPRIS_PLAYER_INTERFACE,
            "Metadata"
        ) ?: return null

        return formatLinuxDisplayTitle(parseLinuxMetadata(metadataOutput))
    }

    private val dbusAddress: String? by lazy {
        System.getenv("DBUS_SESSION_BUS_ADDRESS")
            ?: "/run/user/${getUid()}/bus"
                .takeIf { java.io.File(it).exists() }
                ?.let { "unix:path=$it" }
    }

    private fun getUid(): String {
        return try {
            ProcessBuilder("id", "-u")
                .start()
                .inputStream.bufferedReader().use { it.readText().trim() }
        } catch (_: Exception) {
            "1000"
        }
    }

    private fun runBusctl(vararg args: String): String? {
        return try {
            val pb = ProcessBuilder(listOf("busctl") + args)
                .redirectErrorStream(true)
            dbusAddress?.let { pb.environment()["DBUS_SESSION_BUS_ADDRESS"] = it }
            val process = pb.start()

            if (!process.waitFor(BUSCTL_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                process.destroyForcibly()
                return null
            }

            if (process.exitValue() != 0) {
                return null
            }

            process.inputStream.bufferedReader().use { it.readText().trim() }
        } catch (_: Exception) {
            null
        }
    }

    internal fun parseLinuxMprisServices(listOutput: String, processKeyword: String): List<LinuxMprisService> {
        val keyword = processKeyword.trim()
        val shouldFilterByKeyword = keyword.isNotEmpty()

        return listOutput.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { line ->
                val parts = line.split(Regex("\\s+"))
                if (parts.size < 3) return@mapNotNull null

                val serviceName = parts[0]
                val processName = parts[2]

                if (!serviceName.startsWith(MPRIS_SERVICE_PREFIX)) {
                    return@mapNotNull null
                }

                if (shouldFilterByKeyword && !processName.contains(keyword, ignoreCase = true)) {
                    return@mapNotNull null
                }

                LinuxMprisService(serviceName, processName)
            }
            .toList()
    }

    internal fun selectLinuxMprisService(
        services: List<LinuxMprisService>,
        playbackStatusProvider: (LinuxMprisService) -> String?
    ): LinuxMprisService? {
        if (services.isEmpty()) return null

        for (service in services) {
            val status = playbackStatusProvider(service)
            if (status.equals("Playing", ignoreCase = true)) {
                return service
            }
        }

        return services.first()
    }

    internal fun parsePlaybackStatus(statusOutput: String): String? {
        val match = Regex("""\bs\s+"([^"]+)"""").find(statusOutput) ?: return null
        return decodeBusctlString(match.groupValues[1]).ifBlank { null }
    }

    internal fun parseLinuxMetadata(metadataOutput: String): LinuxTrackMetadata? {
        val title = extractMetadataString(metadataOutput, "xesam:title") ?: return null
        val artist = extractArtist(metadataOutput)
        return LinuxTrackMetadata(title = title, artist = artist)
    }

    internal fun formatLinuxDisplayTitle(metadata: LinuxTrackMetadata?): String? {
        metadata ?: return null
        return if (metadata.artist.isNullOrBlank()) {
            metadata.title
        } else {
            "${metadata.title} - ${metadata.artist}"
        }
    }

    private fun extractMetadataString(metadataOutput: String, key: String): String? {
        val pattern = Regex(""""$key"\s+s\s+"((?:\\.|[^"\\])*)"""")
        val value = pattern.find(metadataOutput)?.groupValues?.get(1) ?: return null
        return decodeBusctlString(value).ifBlank { null }
    }

    private fun extractArtist(metadataOutput: String): String? {
        val pattern = Regex(""""xesam:artist"\s+as\s+\d+\s+"((?:\\.|[^"\\])*)"""")
        val value = pattern.find(metadataOutput)?.groupValues?.get(1) ?: return null
        return decodeBusctlString(value).ifBlank { null }
    }

    private fun decodeBusctlString(value: String): String {
        val bytes = mutableListOf<Byte>()
        var i = 0
        while (i < value.length) {
            if (value[i] == '\\' && i + 1 < value.length) {
                when (value[i + 1]) {
                    '\\' -> { bytes.add('\\'.code.toByte()); i += 2 }
                    '"' -> { bytes.add('"'.code.toByte()); i += 2 }
                    'n' -> { bytes.add('\n'.code.toByte()); i += 2 }
                    't' -> { bytes.add('\t'.code.toByte()); i += 2 }
                    in '0'..'3' -> {
                        // Octal escape: \NNN (1-3 digits)
                        val end = (i + 4).coerceAtMost(value.length)
                        val octal = value.substring(i + 1, end).takeWhile { it in '0'..'7' }
                        if (octal.isNotEmpty()) {
                            bytes.add(octal.toInt(8).toByte())
                            i += 1 + octal.length
                        } else {
                            bytes.add(value[i].code.toByte())
                            i++
                        }
                    }
                    else -> { bytes.add(value[i].code.toByte()); i++ }
                }
            } else {
                bytes.add(value[i].code.toByte())
                i++
            }
        }
        return String(bytes.toByteArray(), Charsets.UTF_8)
    }

    private fun getWText(hWnd: Pointer): String {
        val length = user32.GetWindowTextLengthW(hWnd) + 1
        val buffer = CharArray(length)
        user32.GetWindowTextW(hWnd, buffer, length)
        return Native.toString(buffer)
    }

    private fun getPName(pid: Int): String {
        // Safe access to kernel32 via lazy load
        val hProcess = kernel32.OpenProcess(PROCESS_QUERY_INFORMATION or PROCESS_VM_READ, false, pid) ?: return ""
        try {
            val buffer = CharArray(1024)
            val size = IntByReference(buffer.size)
            if (kernel32.QueryFullProcessImageNameW(hProcess, 0, buffer, size)) {
                val fullPath = Native.toString(buffer)
                return fullPath.substringAfterLast('\\')
            }
        } finally {
            kernel32.CloseHandle(hProcess)
        }
        return ""
    }
}
