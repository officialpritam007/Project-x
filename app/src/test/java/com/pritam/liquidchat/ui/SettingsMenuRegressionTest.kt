package com.pritam.liquidchat.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsMenuRegressionTest {
    @Test fun ownedBlockOffersExactlyOneAction() {
        assertEquals("Block contact",blockActionLabel(false))
        assertEquals("Unblock contact",blockActionLabel(true))
    }
    @Test fun unknownBlockStatusCannotPretendContactIsUnblocked() {
        assertEquals("Checking block status…",blockActionLabel(null))
    }
    @Test fun powerSavingNeverShowsAppearancePanel() {
        assertEquals(SettingsPanel.POWER_SAVING,settingsPanelFor("Power Saving"))
        assertEquals(SettingsPanel.APPEARANCE,settingsPanelFor("Chat Settings"))
        assertEquals(SettingsPanel.APPEARANCE,settingsPanelFor("Liquid Chat General Settings"))
        assertEquals(SettingsPanel.OTHER,settingsPanelFor("Liquid Chat Conversation Screen"))
        assertEquals(SettingsPanel.OTHER,settingsPanelFor("Privacy & Security"))
        assertEquals(SettingsPanel.OTHER,settingsPanelFor("Notifications"))
    }
}
