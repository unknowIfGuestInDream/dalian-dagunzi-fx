/*
 * Copyright (c) 2026 unknowIfGuestInDream.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *     * Neither the name of unknowIfGuestInDream, any associated website, nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UNKNOWIFGUESTINDREAM BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
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
        assertFalse(UpdateChecker.isNewerVersion("abc", "xyz"));
    }
}
