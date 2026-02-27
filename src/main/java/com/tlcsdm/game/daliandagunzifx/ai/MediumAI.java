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
import com.tlcsdm.game.daliandagunzifx.model.PlayType;
import com.tlcsdm.game.daliandagunzifx.model.Rank;
import com.tlcsdm.game.daliandagunzifx.model.Suit;
import com.tlcsdm.game.daliandagunzifx.tracker.CardTracker;

import java.util.*;
import java.util.stream.Collectors;

public class MediumAI implements AIStrategy {

    private final CardTracker cardTracker;
    private final EasyAI easyAI;

    public MediumAI(CardTracker cardTracker) {
        this.cardTracker = cardTracker;
        this.easyAI = new EasyAI();
    }

    @Override
    public Suit chooseTrumpSuit(Player player, Rank trumpRank) {
        // Pick strongest suit: most cards of suit + trump rank cards in that suit
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
        int bestScore = 0;
        for (Suit suit : Suit.values()) {
            int trumpRankCount = trumpRankCounts.getOrDefault(suit, 0);
            if (trumpRankCount < 2) continue;
            // Score = total cards in suit + bonus for trump rank cards in suit
            int score = suitCounts.getOrDefault(suit, 0) + trumpRankCount;
            if (score > bestScore) {
                bestScore = score;
                bestSuit = suit;
            }
        }
        return bestSuit;
    }

    @Override
    public List<Card> chooseKittyCards(Player player, List<Card> kitty, TrumpInfo trumpInfo) {
        List<Card> hand = new ArrayList<>(player.getHand());

        // Count cards per effective suit to identify long/short suits
        Map<Suit, Integer> suitCounts = new EnumMap<>(Suit.class);
        for (Card card : hand) {
            Suit effective = trumpInfo.getEffectiveSuit(card);
            if (effective != null) {
                suitCounts.merge(effective, 1, Integer::sum);
            }
        }

        // Sort by keep score ascending: lowest score cards are discarded first
        hand.sort(Comparator.comparingInt(c -> getKeepScore(c, trumpInfo, suitCounts)));

        List<Card> result = new ArrayList<>();
        for (Card card : hand) {
            if (result.size() >= 6) break;
            if (card.getRank() != Rank.SMALL_JOKER && card.getRank() != Rank.BIG_JOKER) {
                result.add(card);
            }
        }
        return result;
    }

    private int getKeepScore(Card card, TrumpInfo trumpInfo, Map<Suit, Integer> suitCounts) {
        // Higher score = keep, lower score = discard first
        if (trumpInfo.isTrump(card)) {
            return 1000 + trumpInfo.getCardStrength(card);
        }

        Suit effective = trumpInfo.getEffectiveSuit(card);
        int suitLength = effective != null ? suitCounts.getOrDefault(effective, 0) : 0;
        int points = card.getPoints();

        // Point cards: always keep
        if (points > 0) return 500 + points + suitLength;
        // Long suits are worth keeping (>=5 cards)
        if (suitLength >= 5) return 200 + card.getRank().getValue() + suitLength * 10;
        // Short weak suits should be discarded
        return card.getRank().getValue() + suitLength * 10;
    }

    @Override
    public Card chooseCard(Player player, GameEngine engine) {
        List<Card> validCards = easyAI.getValidCards(player, engine);

        if (engine.getTrickCardsPlayed() == 0) {
            return chooseLeadCard(player, validCards, engine);
        }
        return chooseFollowCard(player, validCards, engine);
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
        return chooseMultiFollowCards(player, engine, requiredCount);
    }

    private List<Card> chooseMultiFollowCards(Player player, GameEngine engine, int requiredCount) {
        TrumpInfo trumpInfo = engine.getTrumpInfo();
        Card leadCard = engine.getCurrentTrick()[engine.getCurrentTrickLeader()];
        Suit leadSuit = trumpInfo.getEffectiveSuit(leadCard);

        List<Card> suitCards = new ArrayList<>();
        List<Card> trumpCards = new ArrayList<>();
        List<Card> otherCards = new ArrayList<>();
        for (Card card : player.getHand()) {
            Suit effective = trumpInfo.getEffectiveSuit(card);
            if (effective == leadSuit) {
                suitCards.add(card);
            } else if (trumpInfo.isTrump(card)) {
                trumpCards.add(card);
            } else {
                otherCards.add(card);
            }
        }

        boolean partnerWinning = easyAI.isPartnerWinning(player, engine);

        // Sort suit cards by strength ascending (weakest first)
        suitCards.sort(Comparator.comparingInt(trumpInfo::getCardStrength));
        // Sort other cards: non-point weakest first
        otherCards.sort(Comparator.comparingInt(c -> {
            int base = trumpInfo.getCardStrength(c);
            if (c.getPoints() > 0) return base + 10000;
            return base;
        }));
        trumpCards.sort(Comparator.comparingInt(trumpInfo::getCardStrength));

        List<Card> result = new ArrayList<>();
        // Must play suit cards first
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
                for (Card card : otherCards) {
                    if (result.size() >= requiredCount) break;
                    result.add(card);
                }
            } else {
                // Use low trump if available and worth it
                int trickPoints = calculateCurrentTrickPoints(engine);
                if (!trumpCards.isEmpty() && trickPoints >= 10) {
                    for (Card card : trumpCards) {
                        if (result.size() >= requiredCount) break;
                        result.add(card);
                    }
                }
                // Fill remaining with weakest non-trump non-point cards
                for (Card card : otherCards) {
                    if (result.size() >= requiredCount) break;
                    result.add(card);
                }
                // If still not enough, use trump
                for (Card card : trumpCards) {
                    if (result.size() >= requiredCount) break;
                    if (!result.contains(card)) {
                        result.add(card);
                    }
                }
            }
        }
        return result;
    }

    private Card chooseLeadCard(Player player, List<Card> validCards, GameEngine engine) {
        TrumpInfo trumpInfo = engine.getTrumpInfo();
        int partnerIndex = (player.getId() + 2) % 4;
        int opp1 = (player.getId() + 1) % 4;
        int opp3 = (player.getId() + 3) % 4;

        // Partition cards by effective suit
        Map<Suit, List<Card>> suitCards = new EnumMap<>(Suit.class);
        for (Card card : validCards) {
            Suit effective = trumpInfo.getEffectiveSuit(card);
            if (effective != null) {
                suitCards.computeIfAbsent(effective, k -> new ArrayList<>()).add(card);
            }
        }

        // Score each suit considering tracked void information
        Suit bestSuit = null;
        int bestScore = -1;
        for (Map.Entry<Suit, List<Card>> entry : suitCards.entrySet()) {
            Suit suit = entry.getKey();
            int score = entry.getValue().size() * 10;
            // Bonus if opponents are void (they can't follow suit)
            if (cardTracker.isVoid(opp1, suit)) score += 20;
            if (cardTracker.isVoid(opp3, suit)) score += 20;
            // Penalty if partner is void
            if (cardTracker.isVoid(partnerIndex, suit)) score -= 30;
            // Prefer suits with fewer remaining cards (easier to exhaust)
            score += Math.max(0, 26 - cardTracker.getRemainingCount(suit));
            if (score > bestScore) {
                bestScore = score;
                bestSuit = suit;
            }
        }

        if (bestSuit != null) {
            List<Card> cards = suitCards.get(bestSuit);
            // Play A or K first
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

        // Only trump cards remain, play lowest
        return validCards.stream()
            .min(Comparator.comparingInt(trumpInfo::getCardStrength))
            .orElse(validCards.get(0));
    }

    private Card chooseFollowCard(Player player, List<Card> validCards, GameEngine engine) {
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

        if (!suitCards.isEmpty()) {
            return chooseSuitFollow(player, suitCards, engine);
        }

        boolean partnerWinning = easyAI.isPartnerWinning(player, engine);

        // Consider if it's worth using trump based on trick points
        if (!trumpCards.isEmpty() && !partnerWinning) {
            int trickPoints = calculateCurrentTrickPoints(engine);
            if (trickPoints >= 10 || engine.getTrickCardsPlayed() == 3) {
                // Play minimum trump that can win
                int currentWinStrength = getCurrentWinningStrength(engine);
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
        }

        // Partner winning: dump point cards when safe
        if (partnerWinning) {
            return playPointsIfSafe(validCards, trumpInfo);
        }

        return easyAI.playLow(validCards, trumpInfo);
    }

    private Card chooseSuitFollow(Player player, List<Card> suitCards, GameEngine engine) {
        TrumpInfo trumpInfo = engine.getTrumpInfo();
        boolean partnerWinning = easyAI.isPartnerWinning(player, engine);

        if (partnerWinning) {
            return suitCards.stream()
                .min(Comparator.comparingInt(trumpInfo::getCardStrength))
                .orElse(suitCards.get(0));
        }

        // Try to win with the minimum card that beats the current winner
        int currentWinStrength = getCurrentWinningStrength(engine);
        Card bestWinner = null;
        for (Card card : suitCards) {
            int strength = trumpInfo.getCardStrength(card);
            if (strength > currentWinStrength) {
                if (bestWinner == null || strength < trumpInfo.getCardStrength(bestWinner)) {
                    bestWinner = card;
                }
            }
        }
        if (bestWinner != null) return bestWinner;

        // Can't win, play lowest
        return suitCards.stream()
            .min(Comparator.comparingInt(trumpInfo::getCardStrength))
            .orElse(suitCards.get(0));
    }

    private int getCurrentWinningStrength(GameEngine engine) {
        TrumpInfo trumpInfo = engine.getTrumpInfo();
        Card[] trick = engine.getCurrentTrick();
        int leader = engine.getCurrentTrickLeader();
        Card leadCard = trick[leader];
        Suit leadSuit = trumpInfo.getEffectiveSuit(leadCard);

        int highestStrength = -1;
        for (int i = 0; i < 4; i++) {
            Card card = trick[i];
            if (card == null) continue;
            Suit cardSuit = trumpInfo.getEffectiveSuit(card);
            int strength = trumpInfo.getCardStrength(card);
            boolean canCompete = trumpInfo.isTrump(card) || cardSuit == leadSuit;
            if (canCompete && strength > highestStrength) {
                highestStrength = strength;
            }
        }
        return highestStrength;
    }

    private int calculateCurrentTrickPoints(GameEngine engine) {
        int points = 0;
        for (Card card : engine.getCurrentTrick()) {
            if (card != null) {
                points += card.getPoints();
            }
        }
        return points;
    }

    private Card playPointsIfSafe(List<Card> cards, TrumpInfo trumpInfo) {
        // When partner is winning, dump point cards (5s first, then 10/K)
        for (Card card : cards) {
            if (card.getRank() == Rank.FIVE) return card;
        }
        for (Card card : cards) {
            if (card.getPoints() > 0) return card;
        }
        return easyAI.playLow(cards, trumpInfo);
    }

    public CardTracker getCardTracker() {
        return cardTracker;
    }
}
