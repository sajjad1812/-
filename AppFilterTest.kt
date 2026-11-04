package com.iosexperience.app

import org.junit.Assert.assertEquals
import org.junit.Test

class AppFilterTest {
    private val apps = listOf("Camera", "Calendar", "Maps", "Messages")

    @Test fun blankQueryReturnsAll() {
        assertEquals(apps, filterByLabel(apps, "  ") { it })
    }

    @Test fun matchesCaseInsensitive() {
        assertEquals(listOf("Camera", "Calendar"), filterByLabel(apps, "CA") { it })
    }

    @Test fun noMatchReturnsEmpty() {
        assertEquals(emptyList<String>(), filterByLabel(apps, "zzz") { it })
    }

    @Test fun defaultSettingsAreSane() {
        val s = AppSettings()
        assertEquals(4, s.columns)
        assertEquals(true, s.iosIcons)
        assertEquals(emptySet<String>(), s.dock)
    }
}
