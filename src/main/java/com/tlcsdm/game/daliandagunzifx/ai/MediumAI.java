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

package com.tlcsdm.game.daliandagunzifx.ai;

import com.tlcsdm.game.daliandagunzifx.engine.GameEngine;
import com.tlcsdm.game.daliandagunzifx.engine.Player;
import com.tlcsdm.game.daliandagunzifx.engine.TrumpInfo;
import com.tlcsdm.game.daliandagunzifx.model.Card;
import com.tlcsdm.game.daliandagunzifx.model.Rank;
import com.tlcsdm.game.daliandagunzifx.model.PlayType;
import com.tlcsdm.game.daliandagunzifx.model.Suit;

import java.util.*;
import java.util.stream.Collectors;

public class MediumAI implements AIStrategy {

    @Override
    public Suit chooseTrumpSuit(Player player, Rank trumpRank) {
        Map<Suit, Integer> trumpRankCounts = new EnumMap<>(Suit.class);
        Map<Suit, Integer> suitCounts = new EnumMap<>(Suit.class);
        for (Card card : player.getHand()) {
            if (card.getSuit() == null) continue;
            suitCounts.merge(card.getSuit(), 1, Integer::sum);
            if (card.getRank() == trumpRank) {
                trumpRankCounts.merge(card.getSuit(), 1, Integer::sum);
            }
        }

        Suit bestSuit = null;
        int bestTotal = 0;
        for (Map.Entry<Suit, Integer> entry : trumpRankCounts.entrySet()) {
            if (entry.getValue() >= 2) {
                int total = suitCounts.getOrDefault(entry.getKey(), 0);
                if (total > bestTotal) {
                    bestTotal = total;
                    bestSuit = entry.getKey();
                }
            }
        }
        return bestSuit;
    }

    @Override
    public List<Card> chooseKittyCards(Player player, List<Card> kitty, TrumpInfo trumpInfo) {
        List<Card> hand = new ArrayList<>(player.getHand());
        // Sort by keep priority ascending: lowest priority cards are discarded first
        hand.sort(Comparator.comparingInt(c -> getKeepPriority(c, trumpInfo)));

        List<Card> result = new ArrayList<>();
        for (Card card : hand) {
            if (result.size() >= 6) break;
            if (card.getRank() != Rank.SMALL_JOKER && card.getRank() != Rank.BIG_JOKER) {
                result.add(card);
            }
        }
        return result;
    }

    private int getKeepPriority(Card card, TrumpInfo trumpInfo) {
        if (trumpInfo.isTrump(card)) return 100 + trumpInfo.getCardStrength(card);
        if (card.getPoints() > 0) return 50 + card.getPoints();
        return card.getRank().getValue();
    }

    @Override
    public Card chooseCard(Player player, GameEngine engine) {
        List<Card> validCards = getValidCards(player, engine);

        if (engine.getTrickCardsPlayed() == 0) {
            return chooseLead(player, validCards, engine.getTrumpInfo());
        }
        return chooseFollow(player, validCards, engine);
    }

    @Override
    public List<Card> chooseCards(Player player, GameEngine engine) {
        PlayType trickType = engine.getCurrentTrickPlayType();
        if (trickType == null) {
            return List.of(chooseCard(player, engine));
        }
        int requiredCount = switch (trickType) {
            case SINGLE -> 1;
            case PAIR, BANG -> 2;
            case GUNZI -> 3;
        };
        if (requiredCount == 1) {
            return List.of(chooseCard(player, engine));
        }
        return chooseMultiCards(player, engine, requiredCount);
    }

    protected List<Card> chooseMultiCards(Player player, GameEngine engine, int requiredCount) {
        TrumpInfo trumpInfo = engine.getTrumpInfo();
        Card leadCard = engine.getCurrentTrick()[engine.getCurrentTrickLeader()];
        Suit leadSuit = trumpInfo.getEffectiveSuit(leadCard);

        List<Card> suitCards = new ArrayList<>();
        List<Card> otherCards = new ArrayList<>();
        for (Card card : player.getHand()) {
            if (java.util.Objects.equals(trumpInfo.getEffectiveSuit(card), leadSuit)) {
                suitCards.add(card);
            } else {
                otherCards.add(card);
            }
        }

        boolean partnerWinning = isPartnerWinning(player, engine);

        // Sort suit cards: play weakest first
        suitCards.sort(Comparator.comparingInt(trumpInfo::getCardStrength));
        // Sort other cards: play weakest non-point first
        otherCards.sort(Comparator.comparingInt(c -> {
            int base = trumpInfo.getCardStrength(c);
            if (c.getPoints() > 0) return base + 10000;
            return base;
        }));

        List<Card> result = new ArrayList<>();
        // Play suit cards first (required by rules)
        for (Card card : suitCards) {
            if (result.size() >= requiredCount) break;
            result.add(card);
        }
        // Fill with other cards if not enough suit cards
        if (result.size() < requiredCount) {
            if (partnerWinning) {
                // Dump point cards when partner is winning
                otherCards.sort(Comparator.comparingInt((Card c) -> c.getPoints()).reversed()
                    .thenComparingInt(trumpInfo::getCardStrength));
            }
            for (Card card : otherCards) {
                if (result.size() >= requiredCount) break;
                result.add(card);
            }
        }
        return result;
    }

    protected Card chooseLead(Player player, List<Card> validCards, TrumpInfo trumpInfo) {
        // Lead with strongest non-trump suit (most cards), play A or K first
        Map<Suit, List<Card>> suitCards = new EnumMap<>(Suit.class);
        for (Card card : validCards) {
            Suit effective = trumpInfo.getEffectiveSuit(card);
            if (effective != null) {
                suitCards.computeIfAbsent(effective, k -> new ArrayList<>()).add(card);
            }
        }

        Suit bestSuit = null;
        int bestCount = 0;
        for (Map.Entry<Suit, List<Card>> entry : suitCards.entrySet()) {
            if (entry.getValue().size() > bestCount) {
                bestCount = entry.getValue().size();
                bestSuit = entry.getKey();
            }
        }

        if (bestSuit != null) {
            List<Card> cards = suitCards.get(bestSuit);
            for (Card card : cards) {
                if (card.getRank() == Rank.ACE) return card;
            }
            for (Card card : cards) {
                if (card.getRank() == Rank.KING) return card;
            }
            return cards.stream()
                .max(Comparator.comparingInt(trumpInfo::getCardStrength))
                .orElse(validCards.get(0));
        }

        // Only trump cards left, play lowest
        return validCards.stream()
            .min(Comparator.comparingInt(trumpInfo::getCardStrength))
            .orElse(validCards.get(0));
    }

    protected Card chooseFollow(Player player, List<Card> validCards, GameEngine engine) {
        TrumpInfo trumpInfo = engine.getTrumpInfo();
        Card leadCard = engine.getCurrentTrick()[engine.getCurrentTrickLeader()];
        Suit leadSuit = trumpInfo.getEffectiveSuit(leadCard);

        List<Card> suitCards = new ArrayList<>();
        List<Card> trumpCards = new ArrayList<>();

        for (Card card : validCards) {
            Suit effective = trumpInfo.getEffectiveSuit(card);
            if (effective == leadSuit) {
                suitCards.add(card);
            } else if (trumpInfo.isTrump(card)) {
                trumpCards.add(card);
            }
        }

        // Must follow suit: play lowest card of that suit
        if (!suitCards.isEmpty()) {
            return suitCards.stream()
                .min(Comparator.comparingInt(trumpInfo::getCardStrength))
                .orElse(suitCards.get(0));
        }

        // Cannot follow suit
        boolean partnerWinning = isPartnerWinning(player, engine);

        if (partnerWinning) {
            return playLow(validCards, trumpInfo);
        }

        // Partner not winning, play trump if available
        if (!trumpCards.isEmpty()) {
            return playTrump(player, trumpCards, engine);
        }

        return playLow(validCards, trumpInfo);
    }

    protected Card playTrump(Player player, List<Card> trumpCards, GameEngine engine) {
        TrumpInfo trumpInfo = engine.getTrumpInfo();

        // If last player and team is winning, play low trump
        if (engine.getTrickCardsPlayed() == 3 && isTeamWinning(player, engine)) {
            return trumpCards.stream()
                .min(Comparator.comparingInt(trumpInfo::getCardStrength))
                .orElse(trumpCards.get(0));
        }
        // Play the minimum trump that can beat the current winning card
        int currentWinStrength = getCurrentTrickWinnerStrength(engine);
        Card bestTrump = null;
        for (Card card : trumpCards) {
            int strength = trumpInfo.getCardStrength(card);
            if (strength > currentWinStrength) {
                if (bestTrump == null || strength < trumpInfo.getCardStrength(bestTrump)) {
                    bestTrump = card;
                }
            }
        }
        if (bestTrump != null) return bestTrump;
        // Can't beat current winner, play lowest trump
        return trumpCards.stream()
            .min(Comparator.comparingInt(trumpInfo::getCardStrength))
            .orElse(trumpCards.get(0));
    }

    private int getCurrentTrickWinnerStrength(GameEngine engine) {
        TrumpInfo trumpInfo = engine.getTrumpInfo();
        Card[] trick = engine.getCurrentTrick();
        int leader = engine.getCurrentTrickLeader();
        Card leadCard = trick[leader];
        Suit leadSuit = trumpInfo.getEffectiveSuit(leadCard);

        int highestStrength = -1;
        for (int i = 0; i < 4; i++) {
            Card card = trick[i];
            if (card == null) continue;
            boolean canCompete = trumpInfo.isTrump(card)
                || trumpInfo.getEffectiveSuit(card) == leadSuit;
            if (canCompete) {
                highestStrength = Math.max(highestStrength, trumpInfo.getCardStrength(card));
            }
        }
        return highestStrength;
    }

    protected Card playLow(List<Card> cards, TrumpInfo trumpInfo) {
        List<Card> nonPoint = cards.stream()
            .filter(c -> c.getPoints() == 0)
            .collect(Collectors.toList());
        List<Card> target = nonPoint.isEmpty() ? cards : nonPoint;
        return target.stream()
            .min(Comparator.comparingInt(trumpInfo::getCardStrength))
            .orElse(cards.get(0));
    }

    protected boolean isPartnerWinning(Player player, GameEngine engine) {
        int partnerIndex = (player.getId() + 2) % 4;
        return getCurrentTrickWinner(engine) == partnerIndex;
    }

    protected boolean isTeamWinning(Player player, GameEngine engine) {
        int winnerIndex = getCurrentTrickWinner(engine);
        return winnerIndex == player.getId() || winnerIndex == (player.getId() + 2) % 4;
    }

    protected int getCurrentTrickWinner(GameEngine engine) {
        TrumpInfo trumpInfo = engine.getTrumpInfo();
        Card[] trick = engine.getCurrentTrick();
        int leader = engine.getCurrentTrickLeader();
        Card leadCard = trick[leader];

        if (leadCard == null) return leader;

        Suit leadSuit = trumpInfo.getEffectiveSuit(leadCard);
        int winnerIndex = leader;
        int highestStrength = -1;

        for (int i = 0; i < 4; i++) {
            Card card = trick[i];
            if (card == null) continue;

            Suit cardSuit = trumpInfo.getEffectiveSuit(card);
            int strength = trumpInfo.getCardStrength(card);

            boolean canCompete = trumpInfo.isTrump(card) || cardSuit == leadSuit;
            if (canCompete && strength > highestStrength) {
                highestStrength = strength;
                winnerIndex = i;
            }
        }
        return winnerIndex;
    }

    protected List<Card> getValidCards(Player player, GameEngine engine) {
        List<Card> valid = new ArrayList<>();
        for (Card card : player.getHand()) {
            if (engine.isValidPlay(player.getId(), card)) {
                valid.add(card);
            }
        }
        return valid;
    }
}
