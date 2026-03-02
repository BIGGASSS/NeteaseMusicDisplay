package com.as9929.display.netease

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class CloudMusicHelperTest {

    @Test
    fun parsesMprisServiceFromBusctlListLine() {
        val output = """
            org.mpris.MediaPlayer2.chromium.instance312477    312477 chrome          user  :1.339        user@1000.service -       -
            org.mpris.MediaPlayer2.chromium.instance492134    492134 electron        user  :1.497        user@1000.service -       -
            org.mpris.MediaPlayer2.plasma-browser-integration 312829 plasma-browser- user  :1.341        user@1000.service -       -
        """.trimIndent()

        val services = CloudMusicHelper.parseLinuxMprisServices(output, "electron")

        assertEquals(1, services.size)
        assertEquals("org.mpris.MediaPlayer2.chromium.instance492134", services[0].serviceName)
        assertEquals("electron", services[0].processName)
    }

    @Test
    fun filtersServicesByKeywordCaseInsensitive() {
        val output = """
            org.mpris.MediaPlayer2.chromium.instance492134    492134 Electron        user  :1.497        user@1000.service -       -
            org.mpris.MediaPlayer2.chromium.instance600000    600000 chrome          user  :1.500        user@1000.service -       -
        """.trimIndent()

        val services = CloudMusicHelper.parseLinuxMprisServices(output, "electron")

        assertEquals(1, services.size)
        assertEquals("Electron", services[0].processName)
    }

    @Test
    fun prefersPlayingServiceWhenMultipleCandidates() {
        val services = listOf(
            CloudMusicHelper.LinuxMprisService("org.mpris.MediaPlayer2.first", "electron"),
            CloudMusicHelper.LinuxMprisService("org.mpris.MediaPlayer2.second", "electron")
        )

        val selected = CloudMusicHelper.selectLinuxMprisService(services) { service ->
            if (service.serviceName.endsWith("second")) "Playing" else "Paused"
        }

        assertNotNull(selected)
        assertEquals("org.mpris.MediaPlayer2.second", selected.serviceName)
    }

    @Test
    fun parsesMetadataTitleAndArtistFromBusctlOutput() {
        val metadataOutput = """
            a{sv} 6 "mpris:artUrl" s "file:///tmp/artwork" "mpris:length" x 317076916 "mpris:trackid" o "/org/example/MediaPlayer2/TrackList/TrackABC123" "xesam:album" s "Album Name" "xesam:artist" as 1 "Artist Name" "xesam:title" s "Song Title"
        """.trimIndent()

        val metadata = CloudMusicHelper.parseLinuxMetadata(metadataOutput)

        assertNotNull(metadata)
        assertEquals("Song Title", metadata.title)
        assertEquals("Artist Name", metadata.artist)
    }

    @Test
    fun formatsAsTitleDashArtist() {
        val metadata = CloudMusicHelper.LinuxTrackMetadata(title = "Song Title", artist = "Artist Name")

        val display = CloudMusicHelper.formatLinuxDisplayTitle(metadata)

        assertEquals("Song Title - Artist Name", display)
    }

    @Test
    fun fallsBackToTitleWhenArtistMissing() {
        val metadata = CloudMusicHelper.LinuxTrackMetadata(title = "Song Title", artist = null)

        val display = CloudMusicHelper.formatLinuxDisplayTitle(metadata)

        assertEquals("Song Title", display)
    }

    @Test
    fun returnsNullWhenNoTitle() {
        val metadataOutput = """
            a{sv} 1 "xesam:artist" as 1 "Artist Name"
        """.trimIndent()

        val metadata = CloudMusicHelper.parseLinuxMetadata(metadataOutput)

        assertNull(metadata)
    }
}
