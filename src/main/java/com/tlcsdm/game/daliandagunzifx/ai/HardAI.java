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
import com.tlcsdm.game.daliandagunzifx.engine.GamePhase;
import com.tlcsdm.game.daliandagunzifx.engine.Player;
import com.tlcsdm.game.daliandagunzifx.engine.TrumpInfo;
import com.tlcsdm.game.daliandagunzifx.model.Card;
import com.tlcsdm.game.daliandagunzifx.model.PlayType;
import com.tlcsdm.game.daliandagunzifx.model.Rank;
import com.tlcsdm.game.daliandagunzifx.model.Suit;
import com.tlcsdm.game.daliandagunzifx.tracker.CardTracker;

import java.util.*;

public class HardAI implements AIStrategy {

    private final CardTracker cardTracker;
    private final EasyAI rolloutAI;
    private final MediumAI fallbackAI;

    private static final int NUM_DETERMINIZATIONS = 30;
    private static final long TIME_LIMIT_MS = 1500;

    public HardAI(CardTracker cardTracker) {
        this.cardTracker = cardTracker;
        this.rolloutAI = new EasyAI();
        this.fallbackAI = new MediumAI(cardTracker);
    }

    @Override
    public Suit chooseTrumpSuit(Player player, Rank trumpRank) {
        return fallbackAI.chooseTrumpSuit(player, trumpRank);
    }

    @Override
    public List<Card> chooseKittyCards(Player player, List<Card> kitty, TrumpInfo trumpInfo) {
        return fallbackAI.chooseKittyCards(player, kitty, trumpInfo);
    }

    @Override
    public Card chooseCard(Player player, GameEngine engine) {
        List<Card> validCards = rolloutAI.getValidCards(player, engine);
        if (validCards.size() <= 1) {
            return validCards.get(0);
        }
        return evaluateBestAction(player, engine, validCards.stream()
            .map(List::of)
            .toList()).get(0);
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

        List<List<Card>> candidates = generateMultiCardCandidates(player, engine, requiredCount);
        if (candidates.isEmpty()) {
            return fallbackAI.chooseCards(player, engine);
        }
        if (candidates.size() == 1) {
            return candidates.get(0);
        }
        return evaluateBestAction(player, engine, candidates);
    }

    private List<Card> evaluateBestAction(Player player, GameEngine engine,
                                          List<List<Card>> candidates) {
        double[] totalScores = new double[candidates.size()];
        int[] counts = new int[candidates.size()];

        long startTime = System.currentTimeMillis();
        for (int d = 0; d < NUM_DETERMINIZATIONS; d++) {
            if (System.currentTimeMillis() - startTime > TIME_LIMIT_MS) break;

            GameEngine simBase = createDeterminization(player, engine);

            for (int i = 0; i < candidates.size(); i++) {
                GameEngine sim = simBase.copy();
                double score = simulateWithAction(sim, player.getId(), candidates.get(i));
                totalScores[i] += score;
                counts[i]++;
            }
        }

        int bestIdx = 0;
        double bestAvg = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < candidates.size(); i++) {
            double avg = counts[i] > 0 ? totalScores[i] / counts[i] : 0;
            if (avg > bestAvg) {
                bestAvg = avg;
                bestIdx = i;
            }
        }
        return candidates.get(bestIdx);
    }

    private GameEngine createDeterminization(Player aiPlayer, GameEngine engine) {
        GameEngine sim = engine.copy();
        int aiIndex = aiPlayer.getId();

        // Collect all cards from opponents' hands (these are the unknown cards)
        List<Card> unknownCards = new ArrayList<>();
        int[] handSizes = new int[4];
        for (int i = 0; i < 4; i++) {
            if (i == aiIndex) continue;
            handSizes[i] = engine.getPlayers()[i].getHand().size();
            unknownCards.addAll(sim.getPlayers()[i].getHand());
            sim.getPlayers()[i].getHand().clear();
        }

        // Shuffle unknown cards for random redistribution
        Collections.shuffle(unknownCards);

        // Distribute to opponents respecting void suit constraints
        TrumpInfo trumpInfo = engine.getTrumpInfo();
        for (int i = 0; i < 4; i++) {
            if (i == aiIndex) continue;
            List<Card> dealt = dealRespectingVoids(unknownCards, i, handSizes[i], trumpInfo);
            sim.getPlayers()[i].addCards(dealt);
        }

        return sim;
    }

    private List<Card> dealRespectingVoids(List<Card> pool, int playerIndex,
                                           int handSize, TrumpInfo trumpInfo) {
        List<Card> dealt = new ArrayList<>();
        Iterator<Card> it = pool.iterator();

        // First pass: pick cards that don't violate void constraints
        while (it.hasNext() && dealt.size() < handSize) {
            Card card = it.next();
            Suit effectiveSuit = trumpInfo.getEffectiveSuit(card);
            if (effectiveSuit != null && cardTracker.isVoid(playerIndex, effectiveSuit)) {
                continue;
            }
            dealt.add(card);
            it.remove();
        }

        // Second pass: if not enough cards, fill from remaining (ignoring voids)
        it = pool.iterator();
        while (it.hasNext() && dealt.size() < handSize) {
            Card card = it.next();
            dealt.add(card);
            it.remove();
        }

        return dealt;
    }

    private double simulateWithAction(GameEngine sim, int aiIndex, List<Card> action) {
        try {
            sim.playCards(aiIndex, action);
        } catch (Exception e) {
            return 0;
        }
        return simulateToEnd(sim, aiIndex);
    }

    private double simulateToEnd(GameEngine sim, int aiIndex) {
        int maxIterations = 200;
        int iter = 0;

        while (sim.getPhase() == GamePhase.PLAYING && iter++ < maxIterations) {
            if (sim.getTrickCardsPlayed() == 4) {
                sim.evaluateTrick();
                if (sim.isRoundOver()) break;
                continue;
            }

            int currentIndex = sim.getCurrentPlayerIndex();
            Player currentPlayer = sim.getPlayers()[currentIndex];

            try {
                List<Card> play = rolloutAI.chooseCards(currentPlayer, sim);
                sim.playCards(currentIndex, play);
            } catch (Exception e) {
                break;
            }
        }

        // Handle final trick evaluation
        if (sim.getTrickCardsPlayed() == 4 && sim.getPhase() == GamePhase.PLAYING) {
            try {
                sim.evaluateTrick();
            } catch (Exception ignored) {
            }
        }

        return evaluateScore(sim, aiIndex);
    }

    private double evaluateScore(GameEngine sim, int aiIndex) {
        int defenderPoints = sim.getDefenderPoints();
        int declarerTeam = sim.getPlayers()[sim.getDealerIndex()].getTeam();
        int aiTeam = aiIndex % 2;

        if (aiTeam == declarerTeam) {
            // AI is on declarer team, lower defender points is better
            return -defenderPoints;
        } else {
            // AI is on defender team, higher defender points is better
            return defenderPoints;
        }
    }

    private List<List<Card>> generateMultiCardCandidates(Player player, GameEngine engine,
                                                         int requiredCount) {
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

        List<List<Card>> candidates = new ArrayList<>();

        if (suitCards.size() >= requiredCount) {
            // All cards must be from suit, generate combinations
            generateCombinations(suitCards, requiredCount, 0, new ArrayList<>(), candidates);
        } else {
            // Must play all suit cards + fill from others
            List<Card> forced = new ArrayList<>(suitCards);
            int remaining = requiredCount - forced.size();
            if (remaining > 0) {
                List<List<Card>> otherCombos = new ArrayList<>();
                generateCombinations(otherCards, remaining, 0, new ArrayList<>(), otherCombos);
                for (List<Card> combo : otherCombos) {
                    List<Card> full = new ArrayList<>(forced);
                    full.addAll(combo);
                    candidates.add(full);
                }
            } else {
                candidates.add(forced);
            }
        }

        // Limit candidates to avoid combinatorial explosion
        if (candidates.size() > 20) {
            Collections.shuffle(candidates);
            candidates = new ArrayList<>(candidates.subList(0, 20));
        }

        // Validate candidates
        candidates.removeIf(c -> !engine.isValidPlay(player.getId(), c));

        return candidates;
    }

    private void generateCombinations(List<Card> cards, int k, int start,
                                      List<Card> current, List<List<Card>> result) {
        if (current.size() == k) {
            result.add(new ArrayList<>(current));
            return;
        }
        if (result.size() >= 50) return;
        for (int i = start; i < cards.size(); i++) {
            current.add(cards.get(i));
            generateCombinations(cards, k, i + 1, current, result);
            current.remove(current.size() - 1);
        }
    }

    public CardTracker getCardTracker() {
        return cardTracker;
    }
}
