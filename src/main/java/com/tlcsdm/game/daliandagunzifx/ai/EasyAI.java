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

public class EasyAI implements AIStrategy {

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
            // 领出时考虑主动出对子(棒子)或滚子
            List<Card> leadMulti = chooseLeadMulti(player, engine);
            if (leadMulti != null) {
                return leadMulti;
            }
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

    /**
     * 领出时检查是否有适合打出的对子(棒子)或滚子。
     * 优先出强势的非分牌棒子/滚子。
     */
    protected List<Card> chooseLeadMulti(Player player, GameEngine engine) {
        TrumpInfo trumpInfo = engine.getTrumpInfo();
        List<Card> hand = player.getHand();

        // 按花色+点数分组，找出相同花色+点数的牌
        Map<String, List<Card>> groups = new java.util.LinkedHashMap<>();
        for (Card card : hand) {
            String key = (card.getSuit() == null ? "JOKER" : card.getSuit().name()) + "_" + card.getRank().name();
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(card);
        }

        List<Card> bestGunzi = null;
        int bestGunziStrength = -1;
        List<Card> bestBang = null;
        int bestBangStrength = -1;

        for (List<Card> group : groups.values()) {
            if (group.size() >= 3) {
                // 滚子候选
                List<Card> gunzi = group.subList(0, 3);
                if (engine.determinePlayType(gunzi) == PlayType.GUNZI) {
                    int strength = trumpInfo.getCardStrength(gunzi.get(0));
                    // 优先选非分牌的强牌滚子
                    boolean hasPoints = gunzi.get(0).getPoints() > 0;
                    int score = strength + (hasPoints ? -5000 : 0);
                    if (bestGunzi == null || score > bestGunziStrength) {
                        bestGunzi = new ArrayList<>(gunzi);
                        bestGunziStrength = score;
                    }
                }
            }
            if (group.size() >= 2) {
                // 棒子候选
                List<Card> bang = group.subList(0, 2);
                if (engine.determinePlayType(bang) == PlayType.BANG) {
                    int strength = trumpInfo.getCardStrength(bang.get(0));
                    boolean hasPoints = bang.get(0).getPoints() > 0;
                    int score = strength + (hasPoints ? -5000 : 0);
                    if (bestBang == null || score > bestBangStrength) {
                        bestBang = new ArrayList<>(bang);
                        bestBangStrength = score;
                    }
                }
            }
        }

        // 优先出滚子（威力更大）
        if (bestGunzi != null && engine.isValidPlay(player.getId(), bestGunzi)) {
            return bestGunzi;
        }
        if (bestBang != null && engine.isValidPlay(player.getId(), bestBang)) {
            return bestBang;
        }
        return null;
    }

    protected List<Card> chooseMultiCards(Player player, GameEngine engine, int requiredCount) {
        TrumpInfo trumpInfo = engine.getTrumpInfo();
        Card leadCard = engine.getCurrentTrick()[engine.getCurrentTrickLeader()];
        Suit leadSuit = trumpInfo.getEffectiveSuit(leadCard);

        List<Card> suitCards = new ArrayList<>();
        List<Card> otherCards = new ArrayList<>();
        for (Card card : player.getHand()) {
            if (trumpInfo.getEffectiveSuit(card) == leadSuit) {
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
        // Lead with strongest non-trump suit (most cards), prefer A (likely to win), avoid point cards
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
            // 只有A明确最大，可以安全领出
            for (Card card : cards) {
                if (card.getRank() == Rank.ACE) return card;
            }
            // 优先选非分牌(避免主动送10/K/5)
            Optional<Card> nonPointLead = cards.stream()
                .filter(c -> c.getPoints() == 0)
                .max(Comparator.comparingInt(trumpInfo::getCardStrength));
            if (nonPointLead.isPresent()) return nonPointLead.get();
            // 只有分牌时，出最大的（最有可能赢回来）
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

        // Must follow suit
        if (!suitCards.isEmpty()) {
            boolean partnerWinning = isPartnerWinning(player, engine);
            if (partnerWinning) {
                // 队友赢时，出最小的牌
                return suitCards.stream()
                    .min(Comparator.comparingInt(trumpInfo::getCardStrength))
                    .orElse(suitCards.get(0));
            }
            // 队友没赢，看看这墩是否有分值得争
            int trickPoints = calculateCurrentTrickPoints(engine);
            if (trickPoints > 0) {
                // 有分，尝试用最小能赢的牌来赢
                int currentWinStrength = getCurrentTrickWinnerStrength(engine);
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
            }
            // 无分或赢不了，出最小
            return suitCards.stream()
                .min(Comparator.comparingInt(trumpInfo::getCardStrength))
                .orElse(suitCards.get(0));
        }

        // Cannot follow suit
        boolean partnerWinning = isPartnerWinning(player, engine);

        if (partnerWinning) {
            return playLow(validCards, trumpInfo);
        }

        // 队友没赢，考虑是否值得用主牌
        int trickPoints = calculateCurrentTrickPoints(engine);
        if (!trumpCards.isEmpty() && trickPoints >= 5) {
            return playTrump(player, trumpCards, engine);
        }

        return playLow(validCards, trumpInfo);
    }

    protected int calculateCurrentTrickPoints(GameEngine engine) {
        int points = 0;
        for (Card card : engine.getCurrentTrick()) {
            if (card != null) {
                points += card.getPoints();
            }
        }
        return points;
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
        PlayType trickPlayType = engine.getCurrentTrickPlayType();

        int highestStrength = -1;
        for (int i = 0; i < 4; i++) {
            Card card = trick[i];
            if (card == null) continue;

            // For BANG/GUNZI, only valid matching plays can compete
            if (trickPlayType == PlayType.BANG || trickPlayType == PlayType.GUNZI) {
                List<Card>[] trickCards = engine.getCurrentTrickCards();
                PlayType playType = engine.determinePlayType(trickCards[i]);
                if (playType != trickPlayType) continue;
            }

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
        PlayType trickPlayType = engine.getCurrentTrickPlayType();
        int winnerIndex = leader;
        int highestStrength = -1;

        for (int i = 0; i < 4; i++) {
            Card card = trick[i];
            if (card == null) continue;

            // For BANG/GUNZI, only valid matching plays can compete
            if (trickPlayType == PlayType.BANG || trickPlayType == PlayType.GUNZI) {
                List<Card>[] trickCards = engine.getCurrentTrickCards();
                PlayType playType = engine.determinePlayType(trickCards[i]);
                if (playType != trickPlayType) continue;
            }

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
