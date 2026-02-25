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

package com.tlcsdm.game.daliandagunzifx.engine;

import com.tlcsdm.game.daliandagunzifx.model.Card;
import com.tlcsdm.game.daliandagunzifx.model.Rank;
import com.tlcsdm.game.daliandagunzifx.model.Suit;

public class TrumpInfo {

    private final Suit trumpSuit;
    private final Rank trumpRank;

    public TrumpInfo(Suit trumpSuit, Rank trumpRank) {
        this.trumpSuit = trumpSuit;
        this.trumpRank = trumpRank;
    }

    public Suit getTrumpSuit() {
        return trumpSuit;
    }

    public Rank getTrumpRank() {
        return trumpRank;
    }

    public boolean isTrump(Card card) {
        Rank rank = card.getRank();
        if (rank == Rank.BIG_JOKER || rank == Rank.SMALL_JOKER) {
            return true;
        }
        if (rank == trumpRank) {
            return true;
        }
        return trumpSuit != null && card.getSuit() == trumpSuit;
    }

    public int getCardStrength(Card card) {
        Rank rank = card.getRank();
        Suit suit = card.getSuit();

        if (rank == Rank.BIG_JOKER) {
            return 1000;
        }
        if (rank == Rank.SMALL_JOKER) {
            return 999;
        }
        // Trump rank in trump suit
        if (rank == trumpRank && trumpSuit != null && suit == trumpSuit) {
            return 998;
        }
        // Trump rank in other suits
        if (rank == trumpRank) {
            return 997;
        }
        // Trump suit cards (excluding trump rank, already handled)
        if (trumpSuit != null && suit == trumpSuit) {
            return 900 + rank.getValue();
        }
        // Non-trump cards
        return rank.getValue();
    }

    /**
     * Returns the effective suit of a card. Trump cards return null (meaning trump).
     * Cards of trump rank are considered trump regardless of their actual suit.
     */
    public Suit getEffectiveSuit(Card card) {
        if (isTrump(card)) {
            return null;
        }
        return card.getSuit();
    }
}
