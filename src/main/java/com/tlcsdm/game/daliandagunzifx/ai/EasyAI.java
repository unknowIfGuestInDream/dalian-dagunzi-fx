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

    // 小掉主策略的强度上限：低于此值的主牌视为"小主"（排除2/主牌级/王牌）
    private static final int SMALL_TRUMP_THRESHOLD = 950;

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
     * 策略：
     * 1. 优先出强副牌（A或K）的棒子/滚子，这些牌力大，很可能赢墩
     * 2. 没有强副牌时，考虑小掉主（出小主牌的棒子/滚子，清理弱主）
     * 3. 不出弱副牌的棒子/滚子，避免浪费多张牌却赢不了
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

        // 候选：强副牌棒子/滚子（A或K）
        List<Card> bestStrongGunzi = null;
        int bestStrongGunziStrength = -1;
        List<Card> bestStrongBang = null;
        int bestStrongBangStrength = -1;

        // 候选：小主棒子/滚子（小掉主策略，越小越好）
        List<Card> bestSmallTrumpGunzi = null;
        int bestSmallTrumpGunziStrength = Integer.MAX_VALUE;
        List<Card> bestSmallTrumpBang = null;
        int bestSmallTrumpBangStrength = Integer.MAX_VALUE;

        for (List<Card> group : groups.values()) {
            Card sample = group.get(0);
            int strength = trumpInfo.getCardStrength(sample);
            boolean isTrump = trumpInfo.isTrump(sample);
            Rank rank = sample.getRank();

            if (group.size() >= 3) {
                List<Card> gunzi = group.subList(0, 3);
                if (engine.determinePlayType(gunzi) == PlayType.GUNZI) {
                    if (!isTrump && (rank == Rank.ACE || rank == Rank.KING)) {
                        // 强副牌滚子（A/K级别很可能赢）
                        if (bestStrongGunzi == null || strength > bestStrongGunziStrength) {
                            bestStrongGunzi = new ArrayList<>(gunzi);
                            bestStrongGunziStrength = strength;
                        }
                    } else if (isTrump && strength < SMALL_TRUMP_THRESHOLD) {
                        // 小主滚子（非2/非主牌级/非王的主牌，用于小掉主）
                        if (bestSmallTrumpGunzi == null || strength < bestSmallTrumpGunziStrength) {
                            bestSmallTrumpGunzi = new ArrayList<>(gunzi);
                            bestSmallTrumpGunziStrength = strength;
                        }
                    }
                }
            }
            if (group.size() >= 2) {
                List<Card> bang = group.subList(0, 2);
                if (engine.determinePlayType(bang) == PlayType.BANG) {
                    if (!isTrump && (rank == Rank.ACE || rank == Rank.KING)) {
                        // 强副牌棒子（A/K级别很可能赢）
                        if (bestStrongBang == null || strength > bestStrongBangStrength) {
                            bestStrongBang = new ArrayList<>(bang);
                            bestStrongBangStrength = strength;
                        }
                    } else if (isTrump && strength < SMALL_TRUMP_THRESHOLD) {
                        // 小主棒子（用于小掉主）
                        if (bestSmallTrumpBang == null || strength < bestSmallTrumpBangStrength) {
                            bestSmallTrumpBang = new ArrayList<>(bang);
                            bestSmallTrumpBangStrength = strength;
                        }
                    }
                }
            }
        }

        // 优先出强副牌滚子（A/K级别很可能赢墩）
        if (bestStrongGunzi != null && engine.isValidPlay(player.getId(), bestStrongGunzi)) {
            return bestStrongGunzi;
        }
        // 然后是强副牌棒子
        if (bestStrongBang != null && engine.isValidPlay(player.getId(), bestStrongBang)) {
            return bestStrongBang;
        }
        // 最后考虑小掉主（清理弱主牌）
        if (bestSmallTrumpGunzi != null && engine.isValidPlay(player.getId(), bestSmallTrumpGunzi)) {
            return bestSmallTrumpGunzi;
        }
        if (bestSmallTrumpBang != null && engine.isValidPlay(player.getId(), bestSmallTrumpBang)) {
            return bestSmallTrumpBang;
        }
        return null;
    }

    protected List<Card> chooseMultiCards(Player player, GameEngine engine, int requiredCount) {
        TrumpInfo trumpInfo = engine.getTrumpInfo();
        Card leadCard = engine.getCurrentTrick()[engine.getCurrentTrickLeader()];
        Suit leadSuit = trumpInfo.getEffectiveSuit(leadCard);

        List<Card> suitCards = new ArrayList<>();
        List<Card> otherNonTrump = new ArrayList<>();
        List<Card> otherTrump = new ArrayList<>();
        for (Card card : player.getHand()) {
            if (trumpInfo.getEffectiveSuit(card) == leadSuit) {
                suitCards.add(card);
            } else if (trumpInfo.isTrump(card)) {
                otherTrump.add(card);
            } else {
                otherNonTrump.add(card);
            }
        }

        boolean partnerWinning = isPartnerWinning(player, engine);

        // Sort suit cards: play weakest first, but preserve special trump
        suitCards.sort(Comparator.comparingInt(c -> {
            int strength = trumpInfo.getCardStrength(c);
            // 特殊主牌（王、2、主牌级）排后面，尽量不先出
            if (isSpecialTrump(c, trumpInfo)) return strength + 10000;
            return strength;
        }));
        // Sort non-trump cards: play weakest non-point first
        otherNonTrump.sort(Comparator.comparingInt(c -> {
            int base = trumpInfo.getCardStrength(c);
            if (c.getPoints() > 0) return base + 10000;
            return base;
        }));
        // Sort trump cards: weakest first
        otherTrump.sort(Comparator.comparingInt(trumpInfo::getCardStrength));

        List<Card> result = new ArrayList<>();
        // Play suit cards first (required by rules)
        for (Card card : suitCards) {
            if (result.size() >= requiredCount) break;
            result.add(card);
        }
        // Fill with other cards if not enough suit cards
        if (result.size() < requiredCount) {
            if (partnerWinning) {
                // 队友赢时给分牌，但优先非主牌，避免浪费主牌
                otherNonTrump.sort(Comparator.comparingInt((Card c) -> c.getPoints()).reversed()
                    .thenComparingInt(trumpInfo::getCardStrength));
                for (Card card : otherNonTrump) {
                    if (result.size() >= requiredCount) break;
                    result.add(card);
                }
                // 非主牌不够时再用主牌
                for (Card card : otherTrump) {
                    if (result.size() >= requiredCount) break;
                    result.add(card);
                }
            } else {
                // 优先垫非主牌，保留主牌实力
                for (Card card : otherNonTrump) {
                    if (result.size() >= requiredCount) break;
                    result.add(card);
                }
                for (Card card : otherTrump) {
                    if (result.size() >= requiredCount) break;
                    result.add(card);
                }
            }
        }
        return result;
    }

    protected Card chooseLead(Player player, List<Card> validCards, TrumpInfo trumpInfo) {
        // 领出策略：综合考虑花色强度、长短套和安全性
        Map<Suit, List<Card>> suitCards = new EnumMap<>(Suit.class);
        List<Card> trumpCards = new ArrayList<>();
        for (Card card : validCards) {
            Suit effective = trumpInfo.getEffectiveSuit(card);
            if (effective != null) {
                suitCards.computeIfAbsent(effective, k -> new ArrayList<>()).add(card);
            } else {
                trumpCards.add(card);
            }
        }

        // 1. 任何花色有A，优先安全领出
        for (Map.Entry<Suit, List<Card>> entry : suitCards.entrySet()) {
            for (Card card : entry.getValue()) {
                if (card.getRank() == Rank.ACE) return card;
            }
        }

        // 2. 考虑出短套副牌（1-2张），制造缺门以便后续用主牌杀分
        Suit shortSuit = null;
        int shortSize = Integer.MAX_VALUE;
        for (Map.Entry<Suit, List<Card>> entry : suitCards.entrySet()) {
            int size = entry.getValue().size();
            if (size <= 2 && size < shortSize) {
                // 短套中有非分牌才考虑
                boolean hasNonPoint = entry.getValue().stream().anyMatch(c -> c.getPoints() == 0);
                if (hasNonPoint) {
                    shortSize = size;
                    shortSuit = entry.getKey();
                }
            }
        }
        if (shortSuit != null) {
            List<Card> cards = suitCards.get(shortSuit);
            Optional<Card> nonPointLead = cards.stream()
                .filter(c -> c.getPoints() == 0)
                .min(Comparator.comparingInt(trumpInfo::getCardStrength));
            if (nonPointLead.isPresent()) return nonPointLead.get();
        }

        // 3. 从最长的副牌花色领出（保持控制力）
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

        // 只剩主牌，优先出非特殊的小主牌
        List<Card> nonSpecialTrump = trumpCards.stream()
            .filter(c -> !isSpecialTrump(c, trumpInfo))
            .collect(Collectors.toList());
        if (!nonSpecialTrump.isEmpty()) {
            return nonSpecialTrump.stream()
                .min(Comparator.comparingInt(trumpInfo::getCardStrength))
                .orElse(nonSpecialTrump.get(0));
        }
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

        // 必须跟花色
        if (!suitCards.isEmpty()) {
            boolean partnerWinning = isPartnerWinning(player, engine);
            if (partnerWinning) {
                // 队友赢时，优先给分牌
                return playPointsForPartner(suitCards, trumpInfo);
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

        // 没有该花色可跟
        List<Card> nonTrumpCards = validCards.stream()
            .filter(c -> !trumpInfo.isTrump(c))
            .collect(Collectors.toList());
        boolean partnerWinning = isPartnerWinning(player, engine);

        if (partnerWinning) {
            // 队友赢时，优先垫非主牌分牌，避免浪费主牌
            if (!nonTrumpCards.isEmpty()) {
                return playPointsForPartner(nonTrumpCards, trumpInfo);
            }
            return playPointsForPartner(validCards, trumpInfo);
        }

        // 队友没赢，考虑是否值得用主牌
        int trickPoints = calculateCurrentTrickPoints(engine);
        if (!trumpCards.isEmpty() && trickPoints >= 5) {
            return playTrump(player, trumpCards, engine);
        }

        // 优先垫非主牌，避免浪费主牌
        if (!nonTrumpCards.isEmpty()) {
            return playLow(nonTrumpCards, trumpInfo);
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

    /**
     * 队友赢时优先给分牌（5/10/K），让队友带走更多分数。
     * 优先出非特殊主牌的分值最高的牌（K/10 > 5），保留王牌、2、主牌级等高价值主牌。
     * 无分牌时出最小的非特殊主牌。
     */
    protected Card playPointsForPartner(List<Card> cards, TrumpInfo trumpInfo) {
        // 分离特殊主牌（王、2、主牌级）和普通牌
        List<Card> normalCards = new ArrayList<>();
        List<Card> specialTrumpCards = new ArrayList<>();
        for (Card c : cards) {
            if (isSpecialTrump(c, trumpInfo)) {
                specialTrumpCards.add(c);
            } else {
                normalCards.add(c);
            }
        }
        // 优先从普通牌中给分
        if (!normalCards.isEmpty()) {
            return normalCards.stream()
                .filter(c -> c.getPoints() > 0)
                .max(Comparator.comparingInt(Card::getPoints))
                .orElseGet(() -> normalCards.stream()
                    .min(Comparator.comparingInt(trumpInfo::getCardStrength))
                    .orElse(normalCards.get(0)));
        }
        // 只剩特殊主牌时，出最小的
        return specialTrumpCards.stream()
            .min(Comparator.comparingInt(trumpInfo::getCardStrength))
            .orElse(cards.get(0));
    }

    /**
     * 判断是否为高价值特殊主牌（王、2、主牌级），这些牌不应轻易丢弃。
     */
    protected boolean isSpecialTrump(Card card, TrumpInfo trumpInfo) {
        Rank rank = card.getRank();
        return rank == Rank.BIG_JOKER || rank == Rank.SMALL_JOKER
            || rank == Rank.TWO || rank == trumpInfo.getTrumpRank();
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
