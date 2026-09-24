package io.music_assistant.client.settings

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AnnouncementPrefsTest {
    @Test
    fun `pre-announce defaults on and survives reopening the app settings`() {
        val settings = MapSettings()
        val repo = SettingsRepository(settings, MapSettings())

        assertTrue(repo.announcementPreAnnounce.value)

        repo.setAnnouncementPreAnnounce(false)
        assertFalse(repo.announcementPreAnnounce.value)

        val reopened = SettingsRepository(settings, MapSettings())
        assertFalse(reopened.announcementPreAnnounce.value)

        reopened.setAnnouncementPreAnnounce(true)
        assertTrue(SettingsRepository(settings, MapSettings()).announcementPreAnnounce.value)
    }
}
