package com.vucelich.vistamacfix;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class AppleGlTest {
    private final String originalOs = System.getProperty("os.name");

    @AfterEach
    void restore() {
        System.setProperty("os.name", originalOs);
        System.clearProperty(AppleGl.PROPERTY);
        AppleGl.resetForTests();
    }

    @Test
    void linuxDoesNotSkip() {
        System.setProperty("os.name", "Linux");
        System.clearProperty(AppleGl.PROPERTY);
        AppleGl.resetForTests();
        assertFalse(AppleGl.isActive());
    }

    @Test
    void macSkips() {
        System.setProperty("os.name", "Mac OS X");
        System.clearProperty(AppleGl.PROPERTY);
        AppleGl.resetForTests();
        assertTrue(AppleGl.isActive());
    }

    @Test
    void forceFlagSimulatesTheMacPath() {
        System.setProperty("os.name", "Linux");
        System.setProperty(AppleGl.PROPERTY, "true");
        AppleGl.resetForTests();
        assertTrue(AppleGl.isActive());
    }

    @Test
    void forceFalseStaysOffOnMac() {
        System.setProperty("os.name", "Mac OS X");
        System.setProperty(AppleGl.PROPERTY, "false");
        AppleGl.resetForTests();
        assertFalse(AppleGl.isActive());
    }
}
