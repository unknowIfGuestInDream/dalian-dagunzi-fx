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

package com.tlcsdm.game.daliandagunzifx.tracker;

import com.tlcsdm.game.daliandagunzifx.model.Card;
import com.tlcsdm.game.daliandagunzifx.model.Rank;
import com.tlcsdm.game.daliandagunzifx.model.Suit;

import java.util.*;
import java.util.stream.Collectors;

public class CardTracker {

    private final List<Card> allCards;
    private final Set<Card> playedCards;
    private final Map<Integer, Set<Suit>> playerVoidSuits;

    public CardTracker() {
        allCards = buildFullDeck();
        playedCards = new HashSet<>();
        playerVoidSuits = new HashMap<>();
        for (int i = 0; i < 4; i++) {
            playerVoidSuits.put(i, new HashSet<>());
        }
    }

    private List<Card> buildFullDeck() {
        List<Card> cards = new ArrayList<>(162);
        int id = 0;
        for (int copy = 0; copy < 3; copy++) {
            for (Suit suit : Suit.values()) {
                for (Rank rank : Rank.values()) {
                    if (rank == Rank.SMALL_JOKER || rank == Rank.BIG_JOKER) continue;
                    cards.add(new Card(suit, rank, id++));
                }
            }
        }
        for (int i = 0; i < 3; i++) {
            cards.add(new Card(null, Rank.SMALL_JOKER, id++));
        }
        for (int i = 0; i < 3; i++) {
            cards.add(new Card(null, Rank.BIG_JOKER, id++));
        }
        return cards;
    }

    public void reset() {
        playedCards.clear();
        for (Set<Suit> suits : playerVoidSuits.values()) {
            suits.clear();
        }
    }

    public void cardPlayed(Card card, int playerIndex) {
        playedCards.add(card);
    }

    public void markVoidSuit(int playerIndex, Suit suit) {
        playerVoidSuits.computeIfAbsent(playerIndex, k -> new HashSet<>()).add(suit);
    }

    public boolean isVoid(int playerIndex, Suit suit) {
        Set<Suit> voids = playerVoidSuits.get(playerIndex);
        return voids != null && voids.contains(suit);
    }

    public int getRemainingCount(Suit suit) {
        return (int) allCards.stream()
            .filter(c -> c.getSuit() == suit && !playedCards.contains(c))
            .count();
    }

    public int getRemainingCount(Rank rank) {
        return (int) allCards.stream()
            .filter(c -> c.getRank() == rank && !playedCards.contains(c))
            .count();
    }

    public Set<Card> getPlayedCards() {
        return Collections.unmodifiableSet(playedCards);
    }

    public int getRemainingCardCount() {
        return allCards.size() - playedCards.size();
    }

    public List<Card> getSuitRemainingCards(Suit suit) {
        return allCards.stream()
            .filter(c -> c.getSuit() == suit && !playedCards.contains(c))
            .collect(Collectors.toList());
    }

    public List<Card> getRankRemainingCards(Rank rank) {
        return allCards.stream()
            .filter(c -> c.getRank() == rank && !playedCards.contains(c))
            .collect(Collectors.toList());
    }

    public Map<Suit, List<Card>> getPlayedCardsBySuit() {
        Map<Suit, List<Card>> result = new EnumMap<>(Suit.class);
        for (Suit suit : Suit.values()) {
            result.put(suit, new ArrayList<>());
        }
        for (Card card : playedCards) {
            if (card.getSuit() != null) {
                result.get(card.getSuit()).add(card);
            }
        }
        return result;
    }

    public Map<Rank, List<Card>> getPlayedCardsByRank() {
        Map<Rank, List<Card>> result = new EnumMap<>(Rank.class);
        for (Rank rank : Rank.values()) {
            result.put(rank, new ArrayList<>());
        }
        for (Card card : playedCards) {
            result.get(card.getRank()).add(card);
        }
        return result;
    }
}
