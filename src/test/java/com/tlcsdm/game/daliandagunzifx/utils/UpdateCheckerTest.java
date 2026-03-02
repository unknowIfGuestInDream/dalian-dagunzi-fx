package com.tlcsdm.game.daliandagunzifx.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UpdateCheckerTest {

    @Test
    void testIsNewerVersion_newer() {
        assertTrue(UpdateChecker.isNewerVersion("1.1.0", "1.0.0"));
        assertTrue(UpdateChecker.isNewerVersion("2.0.0", "1.9.9"));
        assertTrue(UpdateChecker.isNewerVersion("1.0.1", "1.0.0"));
    }

    @Test
    void testIsNewerVersion_same() {
        assertFalse(UpdateChecker.isNewerVersion("1.0.0", "1.0.0"));
        assertFalse(UpdateChecker.isNewerVersion("2.1.3", "2.1.3"));
    }

    @Test
    void testIsNewerVersion_older() {
        assertFalse(UpdateChecker.isNewerVersion("1.0.0", "1.0.1"));
        assertFalse(UpdateChecker.isNewerVersion("1.0.0", "2.0.0"));
        assertFalse(UpdateChecker.isNewerVersion("0.9.9", "1.0.0"));
    }

    @Test
    void testIsNewerVersion_differentLengths() {
        assertTrue(UpdateChecker.isNewerVersion("1.0.0.1", "1.0.0"));
        assertFalse(UpdateChecker.isNewerVersion("1.0", "1.0.0"));
        assertTrue(UpdateChecker.isNewerVersion("1.1", "1.0.0"));
    }

    @Test
    void testIsNewerVersion_nullInput() {
        assertFalse(UpdateChecker.isNewerVersion(null, "1.0.0"));
        assertFalse(UpdateChecker.isNewerVersion("1.0.0", null));
        assertFalse(UpdateChecker.isNewerVersion(null, null));
    }

    @Test
    void testIsNewerVersion_invalidFormat() {
        assertFalse(UpdateChecker.isNewerVersion("abc", "1.0.0"));
        assertTrue(UpdateChecker.isNewerVersion("1.0.0", "abc"));
    }
}
