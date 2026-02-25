/*
 * Copyright (c) 2025 unknowIfGuestInDream.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Deck {

    private final List<Card> cards;
    private int dealIndex;

    public Deck() {
        cards = new ArrayList<>(162);
        int id = 0;

        // Three copies of each standard card (4 suits × 13 ranks × 3)
        for (int copy = 0; copy < 3; copy++) {
            for (Suit suit : Suit.values()) {
                for (Rank rank : Rank.values()) {
                    if (rank == Rank.SMALL_JOKER || rank == Rank.BIG_JOKER) {
                        continue;
                    }
                    cards.add(new Card(suit, rank, id++));
                }
            }
        }

        // 6 jokers (3 small, 3 big)
        for (int i = 0; i < 3; i++) {
            cards.add(new Card(null, Rank.SMALL_JOKER, id++));
        }
        for (int i = 0; i < 3; i++) {
            cards.add(new Card(null, Rank.BIG_JOKER, id++));
        }

        dealIndex = 0;
    }

    public void shuffle() {
        Collections.shuffle(cards);
        dealIndex = 0;
    }

    public List<Card> deal(int count) {
        if (dealIndex + count > cards.size()) {
            throw new IllegalStateException("Not enough cards remaining to deal " + count);
        }
        List<Card> dealt = new ArrayList<>(cards.subList(dealIndex, dealIndex + count));
        dealIndex += count;
        return dealt;
    }

    public int remaining() {
        return cards.size() - dealIndex;
    }

    public List<Card> getKitty(int count) {
        return deal(count);
    }
}
