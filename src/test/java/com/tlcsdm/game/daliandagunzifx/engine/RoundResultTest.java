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
package com.tlcsdm.game.daliandagunzifx.engine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RoundResultTest {

    @Test
    void testDeclarerWins() {
        RoundResult result = new RoundResult(100, 0);
        assertTrue(result.isDeclarerWins());
        assertEquals(0, result.getWinningTeam());
        assertEquals(1, result.getLevelChange());
    }

    @Test
    void testDefenderWins() {
        RoundResult result = new RoundResult(150, 0);
        assertFalse(result.isDeclarerWins());
        assertEquals(1, result.getWinningTeam());
        assertEquals(1, result.getLevelChange());
    }

    @Test
    void testLevelChange() {
        // <120: declarer wins, base +1
        RoundResult r0 = new RoundResult(0, 0);
        assertTrue(r0.isDeclarerWins());
        assertEquals(1, r0.getLevelChange());
        assertEquals(0, r0.getWinningTeam());

        RoundResult r119 = new RoundResult(119, 0);
        assertTrue(r119.isDeclarerWins());
        assertEquals(1, r119.getLevelChange());

        // >=120: defender wins, base +1
        RoundResult r120 = new RoundResult(120, 0);
        assertFalse(r120.isDeclarerWins());
        assertEquals(1, r120.getLevelChange());
        assertEquals(1, r120.getWinningTeam());

        RoundResult r200 = new RoundResult(200, 0);
        assertFalse(r200.isDeclarerWins());
        assertEquals(1, r200.getLevelChange());
        assertEquals(1, r200.getWinningTeam());
    }

    @Test
    void testLevelChangeWithJokerBonus() {
        // 庄家赢 + 扣大王：1 + 2 = 3级
        RoundResult r100_2bloods = new RoundResult(100, 0, 2);
        assertTrue(r100_2bloods.isDeclarerWins());
        assertEquals(3, r100_2bloods.getLevelChange());

        // 庄家赢 + 扣小王：1 + 1 = 2级
        RoundResult r100_1blood = new RoundResult(100, 0, 1);
        assertEquals(2, r100_1blood.getLevelChange());

        // 庄家赢 + 扣大王小王各一（2+1=3血）：1 + 3 = 4级
        RoundResult r100_3bloods = new RoundResult(100, 0, 3);
        assertEquals(4, r100_3bloods.getLevelChange());
    }

    @Test
    void testLevelChangeDefenderWinsNoJokerBonus() {
        // 闲家赢 + 扣王失败：不加级，只有基础1级
        RoundResult r120_2bloods = new RoundResult(120, 0, 2);
        assertFalse(r120_2bloods.isDeclarerWins());
        assertEquals(1, r120_2bloods.getLevelChange());

        // 闲家赢 + 扣王失败 + 抠底：基础1级 + 抠底1级 = 2级
        RoundResult r120_2bloods_kouDi = new RoundResult(120, 0, 2, true);
        assertEquals(2, r120_2bloods_kouDi.getLevelChange());
    }

    @Test
    void testLevelChangeWithKouDi() {
        // 闲家赢 + 抠底：基础1级 + 额外1级 = 2级
        RoundResult rKouDi = new RoundResult(130, 0, 0, true);
        assertFalse(rKouDi.isDeclarerWins());
        assertEquals(2, rKouDi.getLevelChange());

        // 闲家赢 + 无抠底：基础1级
        RoundResult rNoKouDi = new RoundResult(130, 0, 0, false);
        assertEquals(1, rNoKouDi.getLevelChange());
    }

    @Test
    void testBoundary() {
        // Exactly at 120: defender wins
        RoundResult result = new RoundResult(120, 0);
        assertFalse(result.isDeclarerWins());
        assertEquals(1, result.getLevelChange());
        assertEquals(1, result.getWinningTeam());
    }

    @Test
    void testTributeCountScoreLow() {
        // Score < 80: tribute = (80 - score) / 10
        RoundResult r0 = new RoundResult(0, 0);
        assertEquals(8, r0.getTributeCount());

        RoundResult r70 = new RoundResult(70, 0);
        assertEquals(1, r70.getTributeCount());

        RoundResult r50 = new RoundResult(50, 0);
        assertEquals(3, r50.getTributeCount());
    }

    @Test
    void testTributeCountScoreHigh() {
        // Score > 150: tribute = (score - 150) / 10
        RoundResult r170 = new RoundResult(170, 0);
        assertEquals(2, r170.getTributeCount());

        RoundResult r200 = new RoundResult(200, 0);
        assertEquals(5, r200.getTributeCount());
    }

    @Test
    void testTributeCountNormalRange() {
        // 80-119: no score-based tribute
        RoundResult r80 = new RoundResult(80, 0);
        assertEquals(0, r80.getTributeCount());

        RoundResult r119 = new RoundResult(119, 0);
        assertEquals(0, r119.getTributeCount());

        // 120-150: no score-based tribute
        RoundResult r120 = new RoundResult(120, 0);
        assertEquals(0, r120.getTributeCount());

        RoundResult r150 = new RoundResult(150, 0);
        assertEquals(0, r150.getTributeCount());
    }

    @Test
    void testKittyBloods() {
        // 扣王成功（庄家赢）：王的级数计入升级，不进贡，进贡仅来自分数惩罚
        RoundResult r100_2bloods = new RoundResult(100, 0, 2);
        assertTrue(r100_2bloods.isDeclarerWins());
        assertEquals(0, r100_2bloods.getTributeCount()); // 100 分无分数惩罚，扣王成功不进贡
        assertEquals(2, r100_2bloods.getKittyBloods());

        // 扣王成功（庄家赢）+ 低分：进贡仅为分数惩罚，不含扣王血
        RoundResult r50_3bloods = new RoundResult(50, 0, 3);
        assertTrue(r50_3bloods.isDeclarerWins());
        assertEquals(3, r50_3bloods.getTributeCount()); // (80-50)/10 = 3，扣王成功不额外进贡

        // 扣王失败（闲家≥120 但庄家扣了王）：分数惩罚 + 扣王血
        RoundResult r170_1blood = new RoundResult(170, 0, 1);
        assertFalse(r170_1blood.isDeclarerWins());
        assertEquals(3, r170_1blood.getTributeCount()); // (170-150)/10 + 1 = 2 + 1

        // 扣王失败：闲家正好 120 分，无分数惩罚，仅扣王血进贡
        RoundResult r120_2bloods = new RoundResult(120, 0, 2);
        assertFalse(r120_2bloods.isDeclarerWins());
        assertEquals(2, r120_2bloods.getTributeCount()); // 0 + 2
    }

    @Test
    void testKittyBloodsNotDoubleCounted() {
        // 回归测试：扣王成功时，王的级数只计入升级，绝不同时计入进贡（避免双重计算）。
        // 庄家赢 + 扣大王（2 血）+ 中间分段（无分数惩罚）
        RoundResult success = new RoundResult(100, 0, 2);
        assertTrue(success.isDeclarerWins());
        assertEquals(3, success.getLevelChange());   // 1 + 2 级
        assertEquals(0, success.getTributeCount());   // 扣王成功不进贡

        // 扣王失败（闲家 125 分）：不加扣王级，仅进贡扣王血
        RoundResult failure = new RoundResult(125, 0, 2);
        assertFalse(failure.isDeclarerWins());
        assertEquals(1, failure.getLevelChange());    // 仅基础 1 级，扣王失败不加级
        assertEquals(2, failure.getTributeCount());   // 分数惩罚 0 + 扣王血 2
    }
}
