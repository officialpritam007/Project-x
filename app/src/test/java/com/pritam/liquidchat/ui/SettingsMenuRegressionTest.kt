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
    @Test fun referenceSettingsAreThePrimaryLandingWithWorkingBackNavigation() {
        assertEquals("Liquid Chat Preferences",REFERENCE_SETTINGS_LANDING)
        assertEquals(null,referenceSettingsBackTarget(REFERENCE_SETTINGS_LANDING,false))
        assertEquals(REFERENCE_SETTINGS_LANDING,
            referenceSettingsBackTarget("Liquid Chat Privacy",true))
        assertEquals(REFERENCE_SETTINGS_LANDING,
            referenceSettingsBackTarget("Account",true))
        assertEquals(REFERENCE_SETTINGS_LANDING,
            referenceSettingsBackTarget("Power Saving",true))
        assertEquals(REFERENCE_SETTINGS_LANDING,
            referenceSettingsBackTarget("Liquid Chat Themes",false))
        assertEquals(null,referenceSettingsBackTarget("Account",false))
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
