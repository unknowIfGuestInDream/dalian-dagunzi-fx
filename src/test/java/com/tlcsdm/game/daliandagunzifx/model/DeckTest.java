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

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DeckTest {

    @Test
    void testDeckHas162Cards() {
        Deck deck = new Deck();
        assertEquals(162, deck.remaining());
    }

    @Test
    void testDeckHasCorrectJokers() {
        Deck deck = new Deck();
        List<Card> all = deck.deal(162);
        long smallJokers = all.stream()
            .filter(c -> c.getRank() == Rank.SMALL_JOKER)
            .count();
        long bigJokers = all.stream()
            .filter(c -> c.getRank() == Rank.BIG_JOKER)
            .count();
        assertEquals(3, smallJokers);
        assertEquals(3, bigJokers);
    }

    @Test
    void testDeckDealReducesRemaining() {
        Deck deck = new Deck();
        deck.deal(39);
        assertEquals(123, deck.remaining());
    }

    @Test
    void testDeckShuffle() {
        Deck deck1 = new Deck();
        List<Card> unshuffled = deck1.deal(162);

        Deck deck2 = new Deck();
        deck2.shuffle();
        List<Card> shuffled = deck2.deal(162);

        // Statistically, shuffled order should differ from unshuffled
        boolean different = false;
        for (int i = 0; i < 162; i++) {
            if (unshuffled.get(i).getId() != shuffled.get(i).getId()) {
                different = true;
                break;
            }
        }
        assertTrue(different, "Shuffled deck should differ from unshuffled deck");
    }
}
