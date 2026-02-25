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
        // <120: declarer wins, +1
        RoundResult r0 = new RoundResult(0, 0);
        assertTrue(r0.isDeclarerWins());
        assertEquals(1, r0.getLevelChange());
        assertEquals(0, r0.getWinningTeam());

        RoundResult r119 = new RoundResult(119, 0);
        assertTrue(r119.isDeclarerWins());
        assertEquals(1, r119.getLevelChange());

        // >=120: defender wins, +1
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
    void testBoundary() {
        // Exactly at 120: defender wins
        RoundResult result = new RoundResult(120, 0);
        assertFalse(result.isDeclarerWins());
        assertEquals(1, result.getLevelChange());
        assertEquals(1, result.getWinningTeam());
    }
}
