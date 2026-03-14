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

        // A是最大的非主牌，必须保留
        if (card.getRank() == Rank.ACE) return 600 + suitLength;
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
            // 领出时考虑主动出对子(棒子)或滚子
            List<Card> leadMulti = easyAI.chooseLeadMulti(player, engine);
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

        // 棒子/滚子跟牌规则：有同花色棒子/滚子时必须出棒子/滚子
        PlayType leadType = engine.getCurrentTrickPlayType();
        if ((leadType == PlayType.BANG || leadType == PlayType.GUNZI)
            && suitCards.size() >= requiredCount) {
            List<Card> matchingGroup = easyAI.findWeakestGroup(suitCards, leadType, trumpInfo);
            if (matchingGroup != null) {
                return matchingGroup;
            }
        }

        boolean partnerWinning = easyAI.isPartnerWinning(player, engine);

        // 跟花色牌按强度升序，但保留特殊主牌
        // 队友没赢时额外避免出分牌（5/10/K），防止给对方送分
        suitCards.sort(Comparator.comparingInt(c -> {
            int strength = trumpInfo.getCardStrength(c);
            if (easyAI.isSpecialTrump(c, trumpInfo)) return strength + EasyAI.SORT_PRIORITY_OFFSET;
            if (!partnerWinning && c.getPoints() > 0) return strength + EasyAI.POINT_CARD_PENALTY_OFFSET;
            return strength;
        }));
        // Sort other cards: non-point weakest first, 保留A（非常有价值的单出牌）
        otherCards.sort(Comparator.comparingInt(c -> {
            int base = trumpInfo.getCardStrength(c);
            if (c.getPoints() > 0) return base + EasyAI.SORT_PRIORITY_OFFSET;
            if (c.getRank() == Rank.ACE) return base + EasyAI.POINT_CARD_PENALTY_OFFSET;
            return base;
        }));
        // Sort trump cards: weakest first, special trumps last
        trumpCards.sort(Comparator.comparingInt(c -> {
            int strength = trumpInfo.getCardStrength(c);
            if (easyAI.isSpecialTrump(c, trumpInfo)) return strength + EasyAI.SORT_PRIORITY_OFFSET;
            return strength;
        }));

        List<Card> result = new ArrayList<>();
        // Must play suit cards first
        for (Card card : suitCards) {
            if (result.size() >= requiredCount) break;
            result.add(card);
        }
        // Fill with other cards if not enough suit cards
        if (result.size() < requiredCount) {
            int remaining = requiredCount - result.size();
            if (partnerWinning) {
                // 队友赢时优先垫非主牌分牌
                otherCards.sort(Comparator.comparingInt((Card c) -> c.getPoints()).reversed()
                    .thenComparingInt(trumpInfo::getCardStrength));
                for (Card card : otherCards) {
                    if (result.size() >= requiredCount) break;
                    result.add(card);
                }
                // 非主牌不够再用主牌
                for (Card card : trumpCards) {
                    if (result.size() >= requiredCount) break;
                    result.add(card);
                }
            } else {
                // 队友没赢，尝试用主牌管上（需要能组成完整的棒子/滚子才有意义）
                List<Card> winningTrumpGroup = easyAI.findWinningTrumpGroup(
                    trumpCards, engine, remaining, trumpInfo);
                if (winningTrumpGroup != null) {
                    result.addAll(winningTrumpGroup);
                } else {
                    // 无法用主牌管上，优先垫非主牌（避免浪费主牌）
                    for (Card card : otherCards) {
                        if (result.size() >= requiredCount) break;
                        result.add(card);
                    }
                    // 非主牌不够时才用主牌（被迫出牌，不是为了管上）
                    for (Card card : trumpCards) {
                        if (result.size() >= requiredCount) break;
                        if (!result.contains(card)) {
                            result.add(card);
                        }
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
        List<Card> trumpCards = new ArrayList<>();
        for (Card card : validCards) {
            Suit effective = trumpInfo.getEffectiveSuit(card);
            if (effective != null) {
                suitCards.computeIfAbsent(effective, k -> new ArrayList<>()).add(card);
            } else {
                trumpCards.add(card);
            }
        }

        // 1. 任何花色有A优先安全领出
        for (Map.Entry<Suit, List<Card>> entry : suitCards.entrySet()) {
            for (Card card : entry.getValue()) {
                if (card.getRank() == Rank.ACE) return card;
            }
        }

        // Score each suit considering tracked void information
        // 同时考虑短套策略（制造缺门）
        Suit bestSuit = null;
        int bestScore = -1;
        for (Map.Entry<Suit, List<Card>> entry : suitCards.entrySet()) {
            Suit suit = entry.getKey();
            int size = entry.getValue().size();
            int score = size * 10;
            // Bonus if opponents are void (they can't follow suit)
            if (cardTracker.isVoid(opp1, suit)) score += 20;
            if (cardTracker.isVoid(opp3, suit)) score += 20;
            // Penalty if partner is void
            if (cardTracker.isVoid(partnerIndex, suit)) score -= 30;
            // Prefer suits with fewer remaining cards (easier to exhaust)
            score += Math.max(0, 26 - cardTracker.getRemainingCount(suit));
            // 短套加分：1-2张的短套有制造缺门价值
            if (size <= EasyAI.SHORT_SUIT_THRESHOLD) score += 15;
            if (score > bestScore) {
                bestScore = score;
                bestSuit = suit;
            }
        }

        if (bestSuit != null) {
            List<Card> cards = suitCards.get(bestSuit);
            // 优先选非分牌(避免主动送10/K/5)
            Optional<Card> nonPointLead = cards.stream()
                .filter(c -> c.getPoints() == 0)
                .max(Comparator.comparingInt(trumpInfo::getCardStrength));
            if (nonPointLead.isPresent()) return nonPointLead.get();
            return cards.stream()
                .max(Comparator.comparingInt(trumpInfo::getCardStrength))
                .orElse(validCards.get(0));
        }

        // 只剩主牌，优先出非特殊的小主牌
        List<Card> nonSpecialTrump = trumpCards.stream()
            .filter(c -> !easyAI.isSpecialTrump(c, trumpInfo))
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

    private Card chooseFollowCard(Player player, List<Card> validCards, GameEngine engine) {
        TrumpInfo trumpInfo = engine.getTrumpInfo();
        Card leadCard = engine.getCurrentTrick()[engine.getCurrentTrickLeader()];
        Suit leadSuit = trumpInfo.getEffectiveSuit(leadCard);

        List<Card> suitCards = new ArrayList<>();
        List<Card> trumpCards = new ArrayList<>();
        List<Card> nonTrumpCards = new ArrayList<>();

        for (Card card : validCards) {
            Suit effective = trumpInfo.getEffectiveSuit(card);
            if (effective == leadSuit) {
                suitCards.add(card);
            } else if (trumpInfo.isTrump(card)) {
                trumpCards.add(card);
            } else {
                nonTrumpCards.add(card);
            }
        }

        if (!suitCards.isEmpty()) {
            return chooseSuitFollow(player, suitCards, engine);
        }

        boolean partnerWinning = easyAI.isPartnerWinning(player, engine);

        // Consider if it's worth using trump based on trick points
        if (!trumpCards.isEmpty() && !partnerWinning) {
            int trickPoints = calculateCurrentTrickPoints(engine);
            // 检查非主牌中是否有无分牌可以安全垫
            boolean hasNonPointNonTrump = nonTrumpCards.stream()
                .anyMatch(c -> c.getPoints() == 0 && !easyAI.isSpecialTrump(c, trumpInfo));
            if (trickPoints >= 10 || engine.getTrickCardsPlayed() == 3
                || !hasNonPointNonTrump) {
                // 检查后续对手是否也缺该花色（会用主牌毙），需要选更大的主牌
                boolean subsequentOpponentVoid = isSubsequentOpponentVoid(player, engine, leadSuit);

                List<Card> candidates;
                if (subsequentOpponentVoid) {
                    // 后续对手也缺门，他们也会用主牌毙 → 放宽限制，允许使用2
                    candidates = trumpCards.stream()
                        .filter(c -> c.getRank() != Rank.BIG_JOKER
                            && c.getRank() != Rank.SMALL_JOKER
                            && c.getRank() != trumpInfo.getTrumpRank())
                        .collect(Collectors.toList());
                    if (candidates.isEmpty()) candidates = trumpCards;
                } else {
                    // 有分可争、最后出牌、或只有分牌可垫时 → 尝试用非特殊主牌赢墩
                    List<Card> nonSpecialTrumps = trumpCards.stream()
                        .filter(c -> !easyAI.isSpecialTrump(c, trumpInfo))
                        .toList();
                    candidates = nonSpecialTrumps.isEmpty() ? trumpCards : nonSpecialTrumps;
                }
                // Play minimum trump that can win
                int currentWinStrength = getCurrentWinningStrength(engine);
                Card bestTrump = null;
                for (Card card : candidates) {
                    int strength = trumpInfo.getCardStrength(card);
                    if (strength > currentWinStrength) {
                        if (bestTrump == null || strength < trumpInfo.getCardStrength(bestTrump)) {
                            bestTrump = card;
                        }
                    }
                }
                if (bestTrump != null) return bestTrump;
            }
        }

        // 队友赢时给非主牌分牌，避免浪费主牌
        if (partnerWinning) {
            if (!nonTrumpCards.isEmpty()) {
                return playPointsIfSafe(nonTrumpCards, trumpInfo);
            }
            return playPointsIfSafe(validCards, trumpInfo);
        }

        // 优先垫非主牌
        if (!nonTrumpCards.isEmpty()) {
            return easyAI.playLow(nonTrumpCards, trumpInfo);
        }
        return easyAI.playLow(validCards, trumpInfo);
    }

    private Card chooseSuitFollow(Player player, List<Card> suitCards, GameEngine engine) {
        TrumpInfo trumpInfo = engine.getTrumpInfo();
        boolean partnerWinning = easyAI.isPartnerWinning(player, engine);

        if (partnerWinning) {
            // 队友赢时，优先给分牌
            return easyAI.playPointsForPartner(suitCards, trumpInfo);
        }

        // 有分值得争，或手中只剩分牌时尝试压牌，保护特殊主牌（2/王/主牌级）
        int trickPoints = calculateCurrentTrickPoints(engine);
        boolean hasNonPointCard = suitCards.stream()
            .anyMatch(c -> c.getPoints() == 0 && !easyAI.isSpecialTrump(c, trumpInfo));
        if (trickPoints > 0 || !hasNonPointCard) {
            int currentWinStrength = getCurrentWinningStrength(engine);
            Card bestWinner = null;
            for (Card card : suitCards) {
                if (easyAI.isSpecialTrump(card, trumpInfo)) continue;
                int strength = trumpInfo.getCardStrength(card);
                if (strength > currentWinStrength) {
                    if (bestWinner == null || strength < trumpInfo.getCardStrength(bestWinner)) {
                        bestWinner = card;
                    }
                }
            }
            if (bestWinner != null) return bestWinner;
        }

        // 无分或赢不了，出最小（保护特殊主牌）
        return easyAI.playLow(suitCards, trumpInfo);
    }

    private int getCurrentWinningStrength(GameEngine engine) {
        TrumpInfo trumpInfo = engine.getTrumpInfo();
        Card[] trick = engine.getCurrentTrick();
        int leader = engine.getCurrentTrickLeader();
        Card leadCard = trick[leader];
        Suit leadSuit = trumpInfo.getEffectiveSuit(leadCard);
        PlayType trickPlayType = engine.getCurrentTrickPlayType();

        int highestStrength = -1;
        // 从领出者开始按出牌顺序遍历，确保同牌力时先出者赢
        for (int offset = 0; offset < 4; offset++) {
            int i = (leader + offset) % 4;
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
            }
        }
        return highestStrength;
    }

    private int calculateCurrentTrickPoints(GameEngine engine) {
        int points = 0;
        List<Card>[] trickCards = engine.getCurrentTrickCards();
        for (int i = 0; i < 4; i++) {
            if (trickCards[i] != null) {
                for (Card card : trickCards[i]) {
                    points += card.getPoints();
                }
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

    /**
     * 检查后续未出牌的对手是否也缺该花色。
     * 如果对手缺门，他们也会用主牌毙，所以需要选更大的主牌。
     */
    private boolean isSubsequentOpponentVoid(Player player, GameEngine engine, Suit leadSuit) {
        if (leadSuit == null) return false;
        int leader = engine.getCurrentTrickLeader();
        Player[] allPlayers = engine.getPlayers();
        // 找到当前玩家在出牌顺序中的位置
        int myPosition = -1;
        for (int i = 0; i < 4; i++) {
            if ((leader + i) % 4 == player.getId()) {
                myPosition = i;
                break;
            }
        }
        // 检查后续未出牌的对手
        for (int j = myPosition + 1; j < 4; j++) {
            int futureIdx = (leader + j) % 4;
            if (allPlayers[futureIdx].getTeam() != player.getTeam()) {
                if (cardTracker.isVoid(futureIdx, leadSuit)) {
                    return true;
                }
            }
        }
        return false;
    }
}
