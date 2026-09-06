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
import com.tlcsdm.game.daliandagunzifx.model.Card;
import com.tlcsdm.game.daliandagunzifx.model.Rank;
import com.tlcsdm.game.daliandagunzifx.model.Suit;
import com.tlcsdm.game.daliandagunzifx.tracker.CardTracker;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 自适应AI（{@link AdaptiveAI}）单元测试：验证根据历史对局自动升级难度的逻辑。
 */
class AdaptiveAITest {

    private AdaptiveAI newAI() {
        return new AdaptiveAI(new CardTracker());
    }

    // ======================== 难度升级测试 ========================

    @Test
    void 初始状态为简单难度() {
        AdaptiveAI ai = newAI();
        assertEquals(0, ai.getTotalRounds());
        assertEquals(0, ai.getPlayerWins());
        assertEquals(AILevel.EASY.getDisplayName(), ai.getCurrentLevelName());
    }

    @Test
    void 达到中等阈值后升级为中等() {
        AdaptiveAI ai = newAI();
        for (int i = 0; i < AdaptiveAI.MEDIUM_THRESHOLD; i++) {
            ai.recordResult(false);
        }
        assertEquals(AdaptiveAI.MEDIUM_THRESHOLD, ai.getTotalRounds());
        assertEquals(AILevel.MEDIUM.getDisplayName(), ai.getCurrentLevelName());
    }

    @Test
    void 达到困难阈值后升级为困难() {
        AdaptiveAI ai = newAI();
        for (int i = 0; i < AdaptiveAI.HARD_THRESHOLD; i++) {
            ai.recordResult(false);
        }
        assertEquals(AdaptiveAI.HARD_THRESHOLD, ai.getTotalRounds());
        assertEquals(AILevel.HARD.getDisplayName(), ai.getCurrentLevelName());
    }

    @Test
    void 未达到中等阈值始终保持简单() {
        AdaptiveAI ai = newAI();
        for (int i = 0; i < AdaptiveAI.MEDIUM_THRESHOLD - 1; i++) {
            ai.recordResult(true);
        }
        assertEquals(AILevel.EASY.getDisplayName(), ai.getCurrentLevelName());
    }

    // ======================== 统计记录测试 ========================

    @Test
    void 记录胜负后统计正确() {
        AdaptiveAI ai = newAI();
        ai.recordResult(true);
        ai.recordResult(false);
        ai.recordResult(true);
        assertEquals(3, ai.getTotalRounds());
        assertEquals(2, ai.getPlayerWins());
    }

    // ======================== 代理出牌测试 ========================

    private GameEngine setupGame() {
        Player[] players = new Player[]{
            new Player(0, "P0", true),
            new Player(1, "P1", false),
            new Player(2, "P2", false),
            new Player(3, "P3", false)
        };
        GameEngine engine = new GameEngine(players);
        engine.startNewRound();
        engine.declareTrump(0, Suit.SPADE);
        List<Card> kittyCards = players[0].getHand().stream()
            .filter(c -> c.getRank() != Rank.SMALL_JOKER && c.getRank() != Rank.BIG_JOKER)
            .limit(6)
            .toList();
        engine.setKitty(kittyCards);
        return engine;
    }

    @Test
    void 简单阶段能正常出牌() {
        AdaptiveAI ai = newAI();
        GameEngine engine = setupGame();
        Player currentPlayer = engine.getPlayers()[engine.getCurrentPlayerIndex()];
        Card card = ai.chooseCard(currentPlayer, engine);
        assertNotNull(card);
        assertTrue(currentPlayer.getHand().contains(card));
    }

    @Test
    void 中等阶段能正常出牌() {
        AdaptiveAI ai = newAI();
        for (int i = 0; i < AdaptiveAI.MEDIUM_THRESHOLD; i++) {
            ai.recordResult(false);
        }
        assertEquals(AILevel.MEDIUM.getDisplayName(), ai.getCurrentLevelName());

        GameEngine engine = setupGame();
        Player currentPlayer = engine.getPlayers()[engine.getCurrentPlayerIndex()];
        Card card = ai.chooseCard(currentPlayer, engine);
        assertNotNull(card);
        assertTrue(currentPlayer.getHand().contains(card));
    }

    @Test
    void 困难阶段能正常出牌() {
        AdaptiveAI ai = newAI();
        for (int i = 0; i < AdaptiveAI.HARD_THRESHOLD; i++) {
            ai.recordResult(false);
        }
        assertEquals(AILevel.HARD.getDisplayName(), ai.getCurrentLevelName());

        GameEngine engine = setupGame();
        Player currentPlayer = engine.getPlayers()[engine.getCurrentPlayerIndex()];
        Card card = ai.chooseCard(currentPlayer, engine);
        assertNotNull(card);
        assertTrue(currentPlayer.getHand().contains(card));
    }

    // ======================== chooseTrumpSuit 测试 ========================

    @Test
    void 能正常选择主牌花色() {
        AdaptiveAI ai = newAI();
        GameEngine engine = setupGame();
        Player currentPlayer = engine.getPlayers()[engine.getCurrentPlayerIndex()];
        // 仅验证方法调用不抛出异常（简单AI在没有足够主牌时可能返回null表示不叫主）
        assertDoesNotThrow(() -> ai.chooseTrumpSuit(currentPlayer, Rank.THREE));
    }

    // ======================== AILevel 枚举测试 ========================

    @Test
    void AILevel枚举包含ADAPTIVE() {
        AILevel[] values = AILevel.values();
        boolean found = false;
        for (AILevel v : values) {
            if (v == AILevel.ADAPTIVE) {
                found = true;
                break;
            }
        }
        assertTrue(found, "AILevel枚举应包含ADAPTIVE选项");
        assertEquals("自适应", AILevel.ADAPTIVE.getDisplayName());
    }
}
