package com.tlcsdm.game.daliandagunzifx.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CardTest {

    @Test
    void testCardPoints() {
        Card fiveHearts = new Card(Suit.HEART, Rank.FIVE, 1);
        Card tenSpades = new Card(Suit.SPADE, Rank.TEN, 2);
        Card kingClubs = new Card(Suit.CLUB, Rank.KING, 3);
        Card threeDiamonds = new Card(Suit.DIAMOND, Rank.THREE, 4);

        assertEquals(5, fiveHearts.getPoints());
        assertEquals(10, tenSpades.getPoints());
        assertEquals(10, kingClubs.getPoints());
        assertEquals(0, threeDiamonds.getPoints());
    }

    @Test
    void testCardDisplayName() {
        Card aceSpades = new Card(Suit.SPADE, Rank.ACE, 1);
        assertEquals("♠A", aceSpades.getDisplayName());

        Card smallJoker = new Card(null, Rank.SMALL_JOKER, 100);
        assertEquals("小王", smallJoker.getDisplayName());

        Card bigJoker = new Card(null, Rank.BIG_JOKER, 101);
        assertEquals("大王", bigJoker.getDisplayName());
    }

    @Test
    void testCardEquals() {
        Card card1 = new Card(Suit.HEART, Rank.ACE, 5);
        Card card2 = new Card(Suit.HEART, Rank.ACE, 5);
        assertEquals(card1, card2);
    }

    @Test
    void testCardNotEquals() {
        Card card1 = new Card(Suit.HEART, Rank.ACE, 5);
        Card card2 = new Card(Suit.HEART, Rank.ACE, 6);
        assertNotEquals(card1, card2);
    }
}
