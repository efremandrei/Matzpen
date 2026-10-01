package com.efremandrei.matzpen

enum class ThemeMode(val id: String) {
    LIGHT("light"), DARK("dark"), ISRAELI("israeli");

    companion object {
        fun restore(saved: String?, legacyDark: Boolean, legacyIsraeli: Boolean): ThemeMode =
            entries.firstOrNull { it.id == saved }
                ?: if (legacyIsraeli) ISRAELI else if (legacyDark) DARK else LIGHT
    }
}
