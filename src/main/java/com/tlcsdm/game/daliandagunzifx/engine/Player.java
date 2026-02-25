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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Player {

    private final int id;
    private final String name;
    private final List<Card> hand;
    private final int team;
    private final boolean isHuman;
    private Rank level;

    public Player(int id, String name, boolean isHuman) {
        this.id = id;
        this.name = name;
        this.hand = new ArrayList<>();
        this.team = id % 2;
        this.isHuman = isHuman;
        this.level = Rank.TWO;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<Card> getHand() {
        return hand;
    }

    public int getTeam() {
        return team;
    }

    public boolean isHuman() {
        return isHuman;
    }

    public Rank getLevel() {
        return level;
    }

    public void setLevel(Rank level) {
        this.level = level;
    }

    public void addCards(List<Card> cards) {
        hand.addAll(cards);
    }

    public void removeCards(List<Card> cards) {
        hand.removeAll(cards);
    }

    public boolean hasCards(List<Card> cards) {
        List<Card> temp = new ArrayList<>(hand);
        for (Card card : cards) {
            if (!temp.remove(card)) {
                return false;
            }
        }
        return true;
    }

    public List<Card> getCardsOfSuit(Suit suit, TrumpInfo trumpInfo) {
        List<Card> result = new ArrayList<>();
        for (Card card : hand) {
            Suit effective = trumpInfo.getEffectiveSuit(card);
            if (suit == null) {
                // Looking for trump cards
                if (effective == null) {
                    result.add(card);
                }
            } else {
                if (effective == suit) {
                    result.add(card);
                }
            }
        }
        return result;
    }

    public List<Card> getTrumpCards(TrumpInfo trumpInfo) {
        return getCardsOfSuit(null, trumpInfo);
    }

    public void sortHand(TrumpInfo trumpInfo) {
        hand.sort(Comparator.comparingInt(trumpInfo::getCardStrength).reversed());
    }
}
