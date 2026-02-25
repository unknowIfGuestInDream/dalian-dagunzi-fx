package com.tlcsdm.game.daliandagunzifx.tracker;

import com.tlcsdm.game.daliandagunzifx.model.Card;
import com.tlcsdm.game.daliandagunzifx.model.Rank;
import com.tlcsdm.game.daliandagunzifx.model.Suit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CardTrackerTest {

    @Test
    void testInitialState() {
        CardTracker tracker = new CardTracker();
        assertEquals(108, tracker.getRemainingCardCount());
        assertEquals(0, tracker.getPlayedCards().size());
    }

    @Test
    void testCardPlayed() {
        CardTracker tracker = new CardTracker();
        Card card = new Card(Suit.HEART, Rank.ACE, 0);
        tracker.cardPlayed(card, 0);
        assertEquals(107, tracker.getRemainingCardCount());
    }

    @Test
    void testVoidSuit() {
        CardTracker tracker = new CardTracker();
        assertFalse(tracker.isVoid(1, Suit.DIAMOND));
        tracker.markVoidSuit(1, Suit.DIAMOND);
        assertTrue(tracker.isVoid(1, Suit.DIAMOND));
    }

    @Test
    void testReset() {
        CardTracker tracker = new CardTracker();
        Card card = new Card(Suit.HEART, Rank.ACE, 0);
        tracker.cardPlayed(card, 0);
        tracker.markVoidSuit(1, Suit.DIAMOND);

        tracker.reset();
        assertEquals(108, tracker.getRemainingCardCount());
        assertEquals(0, tracker.getPlayedCards().size());
        assertFalse(tracker.isVoid(1, Suit.DIAMOND));
    }
}
