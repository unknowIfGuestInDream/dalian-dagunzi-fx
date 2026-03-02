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
package com.tlcsdm.game.daliandagunzifx.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CardTest {

    @Test
    void testCardPoints() {
        Card fiveHearts = new Card(Suit.HEART, Rank.FIVE, 1);
        Card tenSpades = new Card(Suit.SPADE, Rank.TEN, 2);
        Card kingClubs = new Card(Suit.CLUB, Rank.KING, 3);
        Card threeDiamonds = new Card(Suit.DIAMOND, Rank.THREE, 4);

        assertEquals(5, fiveHearts.getPoints());
        assertEquals(10, tenSpades.getPoints());
        assertEquals(10, kingClubs.getPoints());
        assertEquals(0, threeDiamonds.getPoints());
    }

    @Test
    void testCardDisplayName() {
        Card aceSpades = new Card(Suit.SPADE, Rank.ACE, 1);
        assertEquals("♠A", aceSpades.getDisplayName());

        Card smallJoker = new Card(null, Rank.SMALL_JOKER, 100);
        assertEquals("小王", smallJoker.getDisplayName());

        Card bigJoker = new Card(null, Rank.BIG_JOKER, 101);
        assertEquals("大王", bigJoker.getDisplayName());
    }

    @Test
    void testCardEquals() {
        Card card1 = new Card(Suit.HEART, Rank.ACE, 5);
        Card card2 = new Card(Suit.HEART, Rank.ACE, 5);
        assertEquals(card1, card2);
    }

    @Test
    void testCardNotEquals() {
        Card card1 = new Card(Suit.HEART, Rank.ACE, 5);
        Card card2 = new Card(Suit.HEART, Rank.ACE, 6);
        assertNotEquals(card1, card2);
    }
}
