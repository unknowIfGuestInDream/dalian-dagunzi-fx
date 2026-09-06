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
import com.tlcsdm.game.daliandagunzifx.model.Rank;
import com.tlcsdm.game.daliandagunzifx.model.Suit;
import com.tlcsdm.game.daliandagunzifx.tracker.CardTracker;

import java.util.List;

/**
 * 自适应AI：根据玩家的历史对局记录动态调整难度。
 * <ul>
 *   <li>对局轮数 &lt; {@value #MEDIUM_THRESHOLD}：使用简单AI（{@link EasyAI}）。</li>
 *   <li>对局轮数 &lt; {@value #HARD_THRESHOLD}：使用中等AI（{@link MediumAI}）。</li>
 *   <li>对局轮数 &ge; {@value #HARD_THRESHOLD}：使用困难AI（{@link HardAI}）。</li>
 * </ul>
 * 在中/困难阶段，若玩家胜率超过 {@value #AGGRESSION_WIN_RATE_THRESHOLD}，
 * 则自动开启冒险出牌策略，使AI更具攻击性。
 *
 * <p>可通过 {@link #recordResult(boolean)} 在每局结束后更新统计数据，以驱动难度升级。
 */
public class AdaptiveAI implements AIStrategy {

    /** 简单→中等的局数阈值 */
    public static final int MEDIUM_THRESHOLD = 5;
    /** 中等→困难的局数阈值 */
    public static final int HARD_THRESHOLD = 15;
    /** 触发冒险策略的玩家胜率阈值 */
    public static final double AGGRESSION_WIN_RATE_THRESHOLD = 0.6;

    private int totalRounds;
    private int playerWins;
    private CardTracker cardTracker;

    private AIStrategy delegate;

    public AdaptiveAI(CardTracker cardTracker) {
        this.cardTracker = cardTracker;
        this.totalRounds = 0;
        this.playerWins = 0;
        this.delegate = new EasyAI();
    }

    /**
     * 记录一局结果并更新内部策略委托。
     *
     * @param playerWon 本局玩家（人类一方）是否获胜
     */
    public void recordResult(boolean playerWon) {
        totalRounds++;
        if (playerWon) {
            playerWins++;
        }
        updateDelegate();
    }

    /** 根据当前统计数据重新选择底层AI策略并更新冒险模式。 */
    private void updateDelegate() {
        AILevel targetLevel = resolveTargetLevel();

        // 仅在阈值跨越时重新创建委托，避免每局重复构造
        boolean needNew = (delegate == null)
            || (targetLevel == AILevel.MEDIUM && !(delegate instanceof MediumAI))
            || (targetLevel == AILevel.HARD && !(delegate instanceof HardAI))
            || (targetLevel == AILevel.EASY && !(delegate instanceof EasyAI));
        if (needNew) {
            delegate = switch (targetLevel) {
                case EASY -> new EasyAI();
                case MEDIUM -> new MediumAI(cardTracker);
                case HARD -> new HardAI(cardTracker);
                default -> throw new IllegalStateException("未知AI难度等级：" + targetLevel);
            };
        }
        delegate.setAggressive(isAggressiveMode());
    }

    /**
     * 根据已累计的对局轮数解析当前应使用的底层AI等级。
     */
    private AILevel resolveTargetLevel() {
        if (totalRounds < MEDIUM_THRESHOLD) {
            return AILevel.EASY;
        } else if (totalRounds < HARD_THRESHOLD) {
            return AILevel.MEDIUM;
        } else {
            return AILevel.HARD;
        }
    }

    /**
     * 当玩家胜率超过阈值且已达到中等难度阶段时，开启冒险策略以应对玩家优势。
     */
    private boolean isAggressiveMode() {
        if (totalRounds < MEDIUM_THRESHOLD) {
            return false;
        }
        double winRate = (double) playerWins / totalRounds;
        return winRate >= AGGRESSION_WIN_RATE_THRESHOLD;
    }

    /**
     * 每局开始前更新内部持有的 {@link CardTracker} 引用，
     * 并强制重建委托以确保新局的追踪数据同步。
     *
     * @param cardTracker 本局新建的牌局追踪器
     */
    public void updateCardTracker(CardTracker cardTracker) {
        this.cardTracker = cardTracker;
        // 当前委托若是 MediumAI/HardAI，需要重建以绑定新 tracker
        if (delegate instanceof MediumAI || delegate instanceof HardAI) {
            delegate = null;
            updateDelegate();
        }
    }

    /** 返回当前已记录的总对局轮数。 */
    public int getTotalRounds() {
        return totalRounds;
    }

    /** 返回玩家（人类一方）的胜利局数。 */
    public int getPlayerWins() {
        return playerWins;
    }

    /**
     * 返回当前生效的底层AI等级名称，便于UI展示。
     *
     * @return "简单"、"中等" 或 "困难"
     */
    public String getCurrentLevelName() {
        return resolveTargetLevel().getDisplayName();
    }

    @Override
    public void setAggressive(boolean aggressive) {
        delegate.setAggressive(aggressive);
    }

    @Override
    public Suit chooseTrumpSuit(Player player, Rank trumpRank) {
        return delegate.chooseTrumpSuit(player, trumpRank);
    }

    @Override
    public List<Card> chooseKittyCards(Player player, List<Card> kitty, TrumpInfo trumpInfo) {
        return delegate.chooseKittyCards(player, kitty, trumpInfo);
    }

    @Override
    public Card chooseCard(Player player, GameEngine engine) {
        return delegate.chooseCard(player, engine);
    }

    @Override
    public List<Card> chooseCards(Player player, GameEngine engine) {
        return delegate.chooseCards(player, engine);
    }
}
