package com.efremandrei.matzpen

import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeModeTest {
    @Test fun `saved three way choice takes precedence over old flags`() {
        assertEquals(ThemeMode.LIGHT, ThemeMode.restore("light", true, true))
        assertEquals(ThemeMode.DARK, ThemeMode.restore("dark", false, true))
        assertEquals(ThemeMode.ISRAELI, ThemeMode.restore("israeli", true, false))
    }

    @Test fun `old settings migrate to one exclusive theme`() {
        assertEquals(ThemeMode.ISRAELI, ThemeMode.restore(null, true, true))
        assertEquals(ThemeMode.DARK, ThemeMode.restore(null, true, false))
        assertEquals(ThemeMode.LIGHT, ThemeMode.restore(null, false, false))
    }
}
