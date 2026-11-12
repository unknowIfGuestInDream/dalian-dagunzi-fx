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
import com.tlcsdm.game.daliandagunzifx.engine.GamePhase;
import com.tlcsdm.game.daliandagunzifx.engine.Player;
import com.tlcsdm.game.daliandagunzifx.engine.TrumpInfo;
import com.tlcsdm.game.daliandagunzifx.model.Card;
import com.tlcsdm.game.daliandagunzifx.model.PlayType;
import com.tlcsdm.game.daliandagunzifx.model.Rank;
import com.tlcsdm.game.daliandagunzifx.model.Suit;
import com.tlcsdm.game.daliandagunzifx.tracker.CardTracker;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class HardAI implements AIStrategy {

    private final CardTracker cardTracker;
    private final EasyAI rolloutAI;
    private final MediumAI fallbackAI;

    private static final long TIME_LIMIT_MS = 1500;
    private static final int MAX_SIMULATION_ITERATIONS = 200;
    private static final int MAX_CANDIDATES = 20;
    private static final int MAX_COMBINATIONS_PER_GENERATION = 50;
    private static final double WIN_THRESHOLD_BONUS = 10000.0;
    /** ISMCTS UCB1 探索常数 C（越大越倾向于探索未访问节点） */
    private static final double UCB_CONSTANT = Math.sqrt(2.0);

    public HardAI(CardTracker cardTracker) {
        this.cardTracker = cardTracker;
        this.rolloutAI = new EasyAI();
        this.fallbackAI = new MediumAI(cardTracker);
    }

    @Override
    public void setAggressive(boolean aggressive) {
        rolloutAI.setAggressive(aggressive);
        fallbackAI.setAggressive(aggressive);
    }

    @Override
    public Suit chooseTrumpSuit(Player player, Rank trumpRank, int minRequired) {
        return fallbackAI.chooseTrumpSuit(player, trumpRank, minRequired);
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

        // 领出时：若记牌器判断存在可安全兑现的绝对赢张(boss)，直接领出，稳拿该墩并省去 ISMCTS 开销
        if (engine.getTrickCardsPlayed() == 0) {
            Card boss = fallbackAI.findBossLead(player, engine);
            if (boss != null && validCards.contains(boss)) {
                return boss;
            }
        }

        // 跟牌时：对于明显的情况使用启发式策略，避免浪费 ISMCTS 时间
        if (engine.getTrickCardsPlayed() > 0) {
            TrumpInfo trumpInfo = engine.getTrumpInfo();
            boolean partnerWinning = rolloutAI.isPartnerWinning(player, engine);

            if (partnerWinning) {
                // 队友赢时，优先出非主牌（分牌或小牌），避免用主牌杀队友的牌
                List<Card> nonTrumpValid = new ArrayList<>();
                for (Card c : validCards) {
                    if (!trumpInfo.isTrump(c)) nonTrumpValid.add(c);
                }
                if (!nonTrumpValid.isEmpty()) {
                    return rolloutAI.playPointsForPartner(nonTrumpValid, trumpInfo);
                }
                return rolloutAI.playPointsForPartner(validCards, trumpInfo);
            }

            int trickPoints = rolloutAI.calculateCurrentTrickPoints(engine);
            boolean hasNonPointCard = validCards.stream()
                .anyMatch(c -> c.getPoints() == 0 && !rolloutAI.isSpecialTrump(c, trumpInfo));
            if (trickPoints == 0 && hasNonPointCard) {
                // 无分墩且有无分牌可出，不浪费高牌，出最小
                return rolloutAI.playLow(validCards, trumpInfo);
            }

            // 有分可争或手中只剩分牌，检查是否有能赢的非特殊主牌
            // 排除特殊主牌（2/王/主牌级）：这些牌价值极高，不应为中低分墩轻易打出，
            // 如果只剩特殊主牌能赢，视为"无法赢墩"走playLow保护它们
            int currentWinStrength = rolloutAI.getCurrentTrickWinnerStrength(engine);
            boolean canWin = validCards.stream().anyMatch(c ->
                trumpInfo.getCardStrength(c) > currentWinStrength
                    && !rolloutAI.isSpecialTrump(c, trumpInfo));
            if (!canWin) {
                // 无法赢墩，出最小的非分牌，避免给对方送分
                return rolloutAI.playLow(validCards, trumpInfo);
            }
        }

        List<Card> pruned = pruneEquivalentCards(validCards, engine.getTrumpInfo());
        if (pruned.size() <= 1) {
            return pruned.get(0);
        }
        return ismctsSearch(player, engine, pruned.stream()
            .map(List::of)
            .toList()).get(0);
    }

    @Override
    public List<Card> chooseCards(Player player, GameEngine engine) {
        PlayType trickType = engine.getCurrentTrickPlayType();
        if (trickType == null) {
            // 领出时考虑主动出对子(棒子)或滚子
            List<Card> leadMulti = rolloutAI.chooseLeadMulti(player, engine);
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

        // 多牌跟牌启发式守卫：以下两种场景结果明确，不需要 ISMCTS 蒙特卡罗模拟。
        // ISMCTS 在这些场景中容易因随机采样选出不合理的牌（如用王杀队友的牌、把分牌给对手等），
        // 使用MediumAI的确定性策略能保证出牌符合基本打牌常识。
        boolean partnerWinning = rolloutAI.isPartnerWinning(player, engine);
        if (partnerWinning) {
            // 场景1：队友赢时应出小牌或分牌给队友，确定性策略足够
            return fallbackAI.chooseCards(player, engine);
        }
        int trickPoints = rolloutAI.calculateCurrentTrickPoints(engine);
        if (trickPoints == 0) {
            // 场景2：无分墩不值得争，出最小即可，确定性策略足够
            return fallbackAI.chooseCards(player, engine);
        }

        List<List<Card>> candidates = generateMultiCardCandidates(player, engine, requiredCount);
        if (candidates.isEmpty()) {
            return fallbackAI.chooseCards(player, engine);
        }
        if (candidates.size() == 1) {
            return candidates.get(0);
        }
        // 场景3：若不存在"不动用特殊主牌(2/王/主牌级)即可赢墩"的出法，说明管不上或不值得用大牌去管，
        // 交由确定性策略出最小的牌，避免 ISMCTS 随机采样把大小王等特殊主牌白白垫掉（掉主跟棒子/滚子时尤为常见）。
        if (!hasNonSpecialWinningPlay(engine, candidates)) {
            return fallbackAI.chooseCards(player, engine);
        }
        return ismctsSearch(player, engine, candidates);
    }

    /**
     * 判断候选出牌中是否存在"不使用特殊主牌(2/王/主牌级)即可赢下当前墩"的出法。
     * 只有能用普通牌赢墩时才值得用 ISMCTS 去权衡是否争分；否则应保留特殊主牌、出最小牌。
     */
    private boolean hasNonSpecialWinningPlay(GameEngine engine, List<List<Card>> candidates) {
        TrumpInfo trumpInfo = engine.getTrumpInfo();
        PlayType trickType = engine.getCurrentTrickPlayType();
        int currentWinStrength = rolloutAI.getCurrentTrickWinnerStrength(engine);
        for (List<Card> candidate : candidates) {
            if (candidate.stream().anyMatch(c -> rolloutAI.isSpecialTrump(c, trumpInfo))) {
                continue;
            }
            if (engine.determinePlayType(candidate) != trickType) {
                continue;
            }
            if (trumpInfo.getCardStrength(candidate.get(0)) > currentWinStrength) {
                return true;
            }
        }
        return false;
    }

    private List<Card> pruneEquivalentCards(List<Card> cards, TrumpInfo trumpInfo) {
        Map<Suit, List<Card>> suitGroups = new LinkedHashMap<>();
        List<Card> trumpCards = new ArrayList<>();
        for (Card card : cards) {
            if (trumpInfo.isTrump(card)) {
                trumpCards.add(card);
            } else {
                suitGroups.computeIfAbsent(card.getSuit(), k -> new ArrayList<>()).add(card);
            }
        }

        List<Card> result = new ArrayList<>(trumpCards);
        for (List<Card> group : suitGroups.values()) {
            group.sort(Comparator.comparingInt(c -> c.getRank().getValue()));
            Card prev = null;
            for (Card card : group) {
                if (prev == null || card.getPoints() > 0 || prev.getPoints() > 0
                    || card.getRank().getValue() - prev.getRank().getValue() > 1) {
                    result.add(card);
                }
                prev = card;
            }
        }
        return result;
    }

    private List<Card> ismctsSearch(Player player, GameEngine engine,
                                    List<List<Card>> rootCandidates) {
        if (rootCandidates.size() == 1) return rootCandidates.get(0);

        ISMCTSNode root = new ISMCTSNode(null, null);
        int aiIndex = player.getId();

        long startTime = System.currentTimeMillis();

        while (System.currentTimeMillis() - startTime < TIME_LIMIT_MS) {
            // 1. 采样一个与公开信息相容的确定化（随机分配未知手牌）
            GameEngine state = createDeterminization(player, engine);

            // 2. 选择与展开（SO-ISMCTS：只在 AI 决策节点建树，其余玩家用 rollout 推进）
            ISMCTSNode node = root;
            List<ISMCTSNode> path = new ArrayList<>();
            path.add(root);
            root.availability++;

            // 过滤出在当前确定化中合法的根候选
            List<List<Card>> validMoves = getCompatibleCandidates(rootCandidates, state, aiIndex);
            if (validMoves.isEmpty()) continue;

            // 若还有未探索的动作则展开；否则 UCB1 选择已有节点
            List<List<Card>> untried = node.getUntriedMoves(validMoves);
            ISMCTSNode selected;
            if (!untried.isEmpty()) {
                List<Card> move = untried.get(ThreadLocalRandom.current().nextInt(untried.size()));
                selected = node.addChild(move);
            } else {
                selected = node.ucbSelect(validMoves);
                if (selected == null) continue;
            }
            path.add(selected);

            // 3. 在确定化状态下执行选中动作
            try {
                state.playCards(aiIndex, selected.action);
            } catch (Exception e) {
                continue;
            }

            // 4. 继续向更深层推进：推进状态至下一个 AI 决策点并做树展开（深度优先 SO-ISMCTS）
            node = selected;
            advanceAndExpandDeeper(node, state, aiIndex, path);

            // 5. Rollout：用 EasyAI 策略将游戏模拟到终局
            double reward = simulateToEnd(state, aiIndex);
            if (Double.isNaN(reward)) continue;

            // 6. 反向传播
            for (ISMCTSNode n : path) {
                n.update(reward);
            }
        }

        // 以访问次数最多的子节点作为最终决策（健壮策略）
        if (root.children.isEmpty()) return rootCandidates.get(0);

        ISMCTSNode best = null;
        int maxVisits = -1;
        for (ISMCTSNode child : root.children) {
            if (child.visits > maxVisits) {
                maxVisits = child.visits;
                best = child;
            }
        }
        return best != null ? best.action : rootCandidates.get(0);
    }

    /**
     * 在当前迭代中继续向更深的 AI 决策节点推进并尝试展开，
     * 实现多回合 SO-ISMCTS 树深度（其他玩家用 rollout 策略推进，不建树）。
     */
    private void advanceAndExpandDeeper(ISMCTSNode node, GameEngine state, int aiIndex,
                                        List<ISMCTSNode> path) {
        // 最多推进 MAX_SIMULATION_ITERATIONS 步，防止无限循环
        int steps = 0;
        while (state.getPhase() == GamePhase.PLAYING && steps++ < MAX_SIMULATION_ITERATIONS) {
            // 推进至下一个 AI 决策点（其他玩家由 rollout 策略出牌）
            advanceToNextAITurn(state, aiIndex);
            if (state.getPhase() != GamePhase.PLAYING || state.isRoundOver()) break;
            if (state.getCurrentPlayerIndex() != aiIndex) break;

            // 生成当前确定化状态下 AI 的候选动作
            List<List<Card>> validMoves = getCandidatesForState(state, aiIndex);
            if (validMoves.isEmpty()) break;

            List<List<Card>> untried = node.getUntriedMoves(validMoves);
            if (!untried.isEmpty()) {
                // 展开：随机选一个未尝试动作，加入树并停止向下展开
                List<Card> move = untried.get(ThreadLocalRandom.current().nextInt(untried.size()));
                ISMCTSNode child = node.addChild(move);
                path.add(child);
                try {
                    state.playCards(aiIndex, move);
                } catch (Exception ignored) {
                }
                break;
            } else {
                // 选择：UCB1 选择已有子节点并继续向下
                node.availability++;
                ISMCTSNode selected = node.ucbSelect(validMoves);
                if (selected == null) break;
                path.add(selected);
                try {
                    state.playCards(aiIndex, selected.action);
                } catch (Exception e) {
                    break;
                }
                node = selected;
            }
        }
    }

    /**
     * 推进模拟状态直到轮到指定 AI 玩家出牌（其他玩家用 rollout 策略出牌，墩完时自动结算）。
     */
    private void advanceToNextAITurn(GameEngine state, int aiIndex) {
        int steps = 0;
        while (state.getPhase() == GamePhase.PLAYING && !state.isRoundOver()
               && steps++ < MAX_SIMULATION_ITERATIONS) {
            if (state.getTrickCardsPlayed() == 4) {
                try {
                    state.evaluateTrick();
                } catch (Exception e) {
                    break;
                }
                if (state.isRoundOver() || state.getPhase() != GamePhase.PLAYING) break;
                continue;
            }
            if (state.getCurrentPlayerIndex() == aiIndex) break;
            // 其他玩家用 rollout AI 出牌
            int cur = state.getCurrentPlayerIndex();
            try {
                List<Card> play = rolloutAI.chooseCards(state.getPlayers()[cur], state);
                state.playCards(cur, play);
            } catch (Exception e) {
                break;
            }
        }
    }

    /**
     * 在指定模拟状态下，为 AI 玩家生成当前合法的候选动作列表（剪枝后）。
     */
    private List<List<Card>> getCandidatesForState(GameEngine state, int aiIndex) {
        Player aiPlayer = state.getPlayers()[aiIndex];
        if (aiPlayer.getHand().isEmpty()) return Collections.emptyList();

        PlayType trickType = state.getCurrentTrickPlayType();
        if (trickType == null) {
            // 领出：剪枝等价牌后，每张牌视为独立候选
            List<Card> pruned = pruneEquivalentCards(aiPlayer.getHand(), state.getTrumpInfo());
            List<List<Card>> result = new ArrayList<>(pruned.size());
            for (Card c : pruned) result.add(List.of(c));
            return result;
        }
        int required = switch (trickType) {
            case SINGLE -> 1;
            case PAIR, BANG -> 2;
            case GUNZI -> 3;
        };
        if (required == 1) {
            List<Card> valid = rolloutAI.getValidCards(aiPlayer, state);
            List<Card> pruned = pruneEquivalentCards(valid, state.getTrumpInfo());
            List<List<Card>> result = new ArrayList<>(pruned.size());
            for (Card c : pruned) result.add(List.of(c));
            return result;
        }
        List<List<Card>> candidates = generateMultiCardCandidates(aiPlayer, state, required);
        return candidates.isEmpty() ? Collections.emptyList() : candidates;
    }

    /**
     * 过滤候选动作：仅保留在指定确定化状态中合法的动作。
     * 由于候选来自 AI 自身手牌（确定化不改变 AI 手牌），通常全部合法；
     * 此方法作为安全守卫防止极端情况。
     */
    private List<List<Card>> getCompatibleCandidates(List<List<Card>> candidates,
                                                      GameEngine state, int playerIndex) {
        List<List<Card>> compatible = new ArrayList<>();
        for (List<Card> c : candidates) {
            if (state.isValidPlay(playerIndex, c)) compatible.add(c);
        }
        return compatible.isEmpty() ? candidates : compatible;
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

    private double simulateToEnd(GameEngine sim, int aiIndex) {
        int maxIterations = MAX_SIMULATION_ITERATIONS;
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

        // 处理最后一墩结算
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

        double score;
        if (aiTeam == declarerTeam) {
            // AI is on declarer team, lower defender points is better
            score = -defenderPoints;
            // 庄家赢的条件：闲家得分 < 120
            if (defenderPoints < 120) {
                score += WIN_THRESHOLD_BONUS;
            }
        } else {
            // AI is on defender team, higher defender points is better
            score = defenderPoints;
            // 闲家赢的条件：闲家得分 >= 120
            if (defenderPoints >= 120) {
                score += WIN_THRESHOLD_BONUS;
            }
        }
        return score;
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
        if (candidates.size() > MAX_CANDIDATES) {
            Collections.shuffle(candidates);
            candidates = new ArrayList<>(candidates.subList(0, MAX_CANDIDATES));
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
        if (result.size() >= MAX_COMBINATIONS_PER_GENERATION) return;
        for (int i = start; i < cards.size(); i++) {
            current.add(cards.get(i));
            generateCombinations(cards, k, i + 1, current, result);
            current.remove(current.size() - 1);
        }
    }

    public CardTracker getCardTracker() {
        return cardTracker;
    }

    /**
     * ISMCTS 搜索树节点。
     * <p>
     * 单观察者 ISMCTS（SO-ISMCTS）：只在 AI 自身的决策点建树，
     * 其他玩家的出牌通过 rollout 策略推进而不进入树，
     * 避免多玩家信息不对称导致的"策略融合"问题。
     * <p>
     * UCB1 公式：Q/N + C * sqrt(ln(availability) / N)
     * <ul>
     *   <li>Q/N：利用项（平均奖励）</li>
     *   <li>C * sqrt(ln(availability)/N)：探索项，availability 为该节点在合法集合中出现的次数</li>
     * </ul>
     */
    private static class ISMCTSNode {
        /** 到达本节点所采取的出牌动作（根节点为 null） */
        final List<Card> action;
        /** 本节点被访问的总次数 */
        int visits;
        /** 累计奖励（从 AI 视角） */
        double totalReward;
        /**
         * 本节点在历次迭代中出现在合法动作集合内的次数，
         * 用作 UCB1 探索项分母，防止未被当前确定化覆盖的节点获得不公平优势。
         */
        int availability;
        final List<ISMCTSNode> children;
        final ISMCTSNode parent;

        ISMCTSNode(List<Card> action, ISMCTSNode parent) {
            this.action = action;
            this.parent = parent;
            this.children = new ArrayList<>();
            this.availability = 1;
        }

        /** UCB1 分数。访问次数为 0 的节点返回 MAX_VALUE 以优先展开。 */
        double ucb1() {
            if (visits == 0) return Double.MAX_VALUE;
            return totalReward / visits
                + UCB_CONSTANT * Math.sqrt(Math.log(Math.max(1, availability)) / visits);
        }

        /**
         * 在合法动作集中找出 UCB1 分数最高的已有子节点，
         * 同时更新所有在合法集中出现的子节点的 availability 计数。
         */
        ISMCTSNode ucbSelect(List<List<Card>> validMoves) {
            ISMCTSNode best = null;
            double bestScore = Double.NEGATIVE_INFINITY;
            for (ISMCTSNode child : children) {
                boolean available = false;
                for (List<Card> move : validMoves) {
                    if (child.action.equals(move)) {
                        available = true;
                        break;
                    }
                }
                if (available) {
                    child.availability++;
                    double score = child.ucb1();
                    if (score > bestScore) {
                        bestScore = score;
                        best = child;
                    }
                }
            }
            return best;
        }

        /**
         * 返回在合法动作集中尚无对应子节点的动作列表（未探索分支）。
         */
        List<List<Card>> getUntriedMoves(List<List<Card>> validMoves) {
            List<List<Card>> untried = new ArrayList<>();
            outer:
            for (List<Card> move : validMoves) {
                for (ISMCTSNode child : children) {
                    if (child.action.equals(move)) continue outer;
                }
                untried.add(move);
            }
            return untried;
        }

        ISMCTSNode addChild(List<Card> move) {
            ISMCTSNode child = new ISMCTSNode(move, this);
            children.add(child);
            return child;
        }

        void update(double reward) {
            visits++;
            totalReward += reward;
        }
    }
}
