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

        // 庄家赢 + 扣大王小王各一：1 + 3 = 4级
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
        // Kitty bloods add to tribute count
        RoundResult r100_2bloods = new RoundResult(100, 0, 2);
        assertEquals(2, r100_2bloods.getTributeCount());
        assertEquals(2, r100_2bloods.getKittyBloods());

        // Score-based + kitty bloods
        RoundResult r50_3bloods = new RoundResult(50, 0, 3);
        assertEquals(6, r50_3bloods.getTributeCount()); // (80-50)/10 + 3 = 3 + 3

        RoundResult r170_1blood = new RoundResult(170, 0, 1);
        assertEquals(3, r170_1blood.getTributeCount()); // (170-150)/10 + 1 = 2 + 1
    }
}
