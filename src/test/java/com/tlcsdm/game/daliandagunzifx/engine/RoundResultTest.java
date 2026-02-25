package com.tlcsdm.game.daliandagunzifx.engine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RoundResultTest {

    @Test
    void testDeclarerWins() {
        RoundResult result = new RoundResult(40, 0);
        assertTrue(result.isDeclarerWins());
        assertEquals(0, result.getWinningTeam());
    }

    @Test
    void testDefenderWins() {
        RoundResult result = new RoundResult(100, 0);
        assertFalse(result.isDeclarerWins());
        assertEquals(1, result.getWinningTeam());
    }

    @Test
    void testLevelChange() {
        // 0 points: declarer wins, +3
        RoundResult r0 = new RoundResult(0, 0);
        assertTrue(r0.isDeclarerWins());
        assertEquals(3, r0.getLevelChange());
        assertEquals(0, r0.getWinningTeam());

        // <40: declarer wins, +2
        RoundResult r35 = new RoundResult(35, 0);
        assertTrue(r35.isDeclarerWins());
        assertEquals(2, r35.getLevelChange());

        // <80: declarer wins, +1
        RoundResult r79 = new RoundResult(79, 0);
        assertTrue(r79.isDeclarerWins());
        assertEquals(1, r79.getLevelChange());

        // <120: defender wins, +1
        RoundResult r100 = new RoundResult(100, 0);
        assertFalse(r100.isDeclarerWins());
        assertEquals(1, r100.getLevelChange());
        assertEquals(1, r100.getWinningTeam());

        // <160: defender wins, +2
        RoundResult r150 = new RoundResult(150, 0);
        assertEquals(2, r150.getLevelChange());

        // >=160: defender wins, +3
        RoundResult r200 = new RoundResult(200, 0);
        assertEquals(3, r200.getLevelChange());
    }

    @Test
    void testTie() {
        RoundResult result = new RoundResult(80, 0);
        assertFalse(result.isDeclarerWins());
        assertEquals(0, result.getLevelChange());
        assertEquals(-1, result.getWinningTeam());
    }
}
