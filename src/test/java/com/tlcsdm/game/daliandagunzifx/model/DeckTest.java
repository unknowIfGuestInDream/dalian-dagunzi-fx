package com.tlcsdm.game.daliandagunzifx.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DeckTest {

    @Test
    void testDeckHas162Cards() {
        Deck deck = new Deck();
        assertEquals(162, deck.remaining());
    }

    @Test
    void testDeckHasCorrectJokers() {
        Deck deck = new Deck();
        List<Card> all = deck.deal(162);
        long smallJokers = all.stream()
            .filter(c -> c.getRank() == Rank.SMALL_JOKER)
            .count();
        long bigJokers = all.stream()
            .filter(c -> c.getRank() == Rank.BIG_JOKER)
            .count();
        assertEquals(3, smallJokers);
        assertEquals(3, bigJokers);
    }

    @Test
    void testDeckDealReducesRemaining() {
        Deck deck = new Deck();
        deck.deal(38);
        assertEquals(124, deck.remaining());
    }

    @Test
    void testDeckShuffle() {
        Deck deck1 = new Deck();
        List<Card> unshuffled = deck1.deal(162);

        Deck deck2 = new Deck();
        deck2.shuffle();
        List<Card> shuffled = deck2.deal(162);

        // Statistically, shuffled order should differ from unshuffled
        boolean different = false;
        for (int i = 0; i < 162; i++) {
            if (unshuffled.get(i).getId() != shuffled.get(i).getId()) {
                different = true;
                break;
            }
        }
        assertTrue(different, "Shuffled deck should differ from unshuffled deck");
    }
}
