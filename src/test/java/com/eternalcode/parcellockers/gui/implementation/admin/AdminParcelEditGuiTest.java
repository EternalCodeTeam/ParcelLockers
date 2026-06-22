package com.eternalcode.parcellockers.gui.implementation.admin;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AdminParcelEditGuiTest {

    @Test
    @DisplayName("Should return empty string for a null placeholder value")
    void nullToEmptyWhenValueIsNull() {
        assertEquals("", AdminParcelEditGui.nullToEmpty(null));
    }

    @Test
    @DisplayName("Should return the original value when not null")
    void nullToEmptyWhenValueIsNotNull() {
        assertEquals("desc", AdminParcelEditGui.nullToEmpty("desc"));
    }

    @Test
    @DisplayName("Should not throw when substituting a null parcel description into a template")
    void replaceWithNullDescriptionDoesNotThrow() {
        String template = "Description: {DESCRIPTION}";
        assertDoesNotThrow(() -> template.replace("{DESCRIPTION}", AdminParcelEditGui.nullToEmpty(null)));
        assertEquals("Description: ", template.replace("{DESCRIPTION}", AdminParcelEditGui.nullToEmpty(null)));
    }
}
