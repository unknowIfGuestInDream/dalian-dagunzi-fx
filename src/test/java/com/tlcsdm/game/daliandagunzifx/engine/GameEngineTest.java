package com.tlcsdm.game.daliandagunzifx.engine;

import com.tlcsdm.game.daliandagunzifx.model.Card;
import com.tlcsdm.game.daliandagunzifx.model.Rank;
import com.tlcsdm.game.daliandagunzifx.model.Suit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameEngineTest {

    private GameEngine engine;
    private Player[] players;

    @BeforeEach
    void setUp() {
        players = new Player[]{
            new Player(0, "P0", false),
            new Player(1, "P1", false),
            new Player(2, "P2", false),
            new Player(3, "P3", false)
        };
        engine = new GameEngine(players);
    }

    @Test
    void testStartNewRound() {
        engine.startNewRound();
        for (int i = 0; i < 4; i++) {
            assertEquals(38, players[i].getHand().size(),
                "Player " + i + " should have 38 cards");
        }
        assertEquals(10, engine.getKitty().size());
    }

    @Test
    void testDeclareTrump() {
        engine.startNewRound();
        engine.declareTrump(0, Suit.SPADE);
        assertNotNull(engine.getTrumpInfo());
        assertEquals(Suit.SPADE, engine.getTrumpInfo().getTrumpSuit());
        assertEquals(Rank.THREE, engine.getTrumpInfo().getTrumpRank());
    }

    @Test
    void testSetKitty() {
        engine.startNewRound();
        engine.declareTrump(0, Suit.SPADE);
        // After declareTrump, dealer picks up 10 kitty cards -> 48 cards
        assertEquals(48, players[0].getHand().size());

        // Select 10 non-joker cards for the kitty
        List<Card> kittyToSet = players[0].getHand().stream()
            .filter(c -> c.getRank() != Rank.SMALL_JOKER && c.getRank() != Rank.BIG_JOKER)
            .limit(10)
            .toList();
        engine.setKitty(kittyToSet);
        assertEquals(38, players[0].getHand().size());
    }

    @Test
    void testSetKittyAllowsJokersWithBloodPenalty() {
        engine.startNewRound();
        engine.declareTrump(0, Suit.SPADE);

        // Find jokers in hand
        List<Card> hand = players[0].getHand();
        List<Card> jokers = hand.stream()
            .filter(c -> c.getRank() == Rank.SMALL_JOKER || c.getRank() == Rank.BIG_JOKER)
            .toList();

        if (!jokers.isEmpty()) {
            // Build a kitty with one joker + 9 regular cards
            List<Card> kittyWithJoker = new ArrayList<>();
            kittyWithJoker.add(jokers.get(0));
            hand.stream()
                .filter(c -> c.getRank() != Rank.SMALL_JOKER && c.getRank() != Rank.BIG_JOKER)
                .limit(9)
                .forEach(kittyWithJoker::add);
            // Should not throw - jokers are allowed but incur blood penalty
            assertDoesNotThrow(() -> engine.setKitty(kittyWithJoker));
            assertTrue(engine.getKittyBloods() > 0);
        }
    }

    @Test
    void testValidPlayLeader() {
        engine.startNewRound();
        engine.declareTrump(0, Suit.SPADE);
        List<Card> kittyCards = selectNonJokerKitty(players[0], 10);
        engine.setKitty(kittyCards);

        // Dealer (player 0) is the leader and can play any card
        Card anyCard = players[0].getHand().get(0);
        assertTrue(engine.isValidPlay(0, anyCard));
    }

    @Test
    void testValidPlayFollowSuit() {
        engine.startNewRound();
        engine.declareTrump(0, Suit.SPADE);
        List<Card> kittyCards = selectNonJokerKitty(players[0], 10);
        engine.setKitty(kittyCards);

        // Player 0 leads
        Card leadCard = players[0].getHand().get(0);
        engine.playCard(0, leadCard);

        // Player 1 must follow effective suit if able
        Suit leadSuit = engine.getTrumpInfo().getEffectiveSuit(leadCard);
        List<Card> suitCards = players[1].getCardsOfSuit(leadSuit, engine.getTrumpInfo());
        if (!suitCards.isEmpty()) {
            Card followCard = suitCards.get(0);
            assertTrue(engine.isValidPlay(1, followCard));
            // A card of a different effective suit should be invalid
            for (Card c : players[1].getHand()) {
                Suit cs = engine.getTrumpInfo().getEffectiveSuit(c);
                if ((leadSuit == null && cs != null) || (leadSuit != null && cs != leadSuit)) {
                    assertFalse(engine.isValidPlay(1, c));
                    break;
                }
            }
        }
    }

    @Test
    void testValidPlayCannotFollowSuit() {
        engine.startNewRound();
        engine.declareTrump(0, Suit.HEART);
        List<Card> kittyCards = selectNonJokerKitty(players[0], 10);
        engine.setKitty(kittyCards);

        // Manually set up player 1 to have no cards of a specific suit
        players[1].getHand().clear();
        // Give player 1 only club cards (non-trump, non-heart)
        for (int i = 0; i < 38; i++) {
            players[1].addCards(List.of(new Card(Suit.CLUB, Rank.FOUR, 500 + i)));
        }

        // Player 0 leads with a diamond
        Card diamondCard = null;
        for (Card c : players[0].getHand()) {
            if (engine.getTrumpInfo().getEffectiveSuit(c) == Suit.DIAMOND) {
                diamondCard = c;
                break;
            }
        }
        if (diamondCard != null) {
            engine.playCard(0, diamondCard);
            // Player 1 has no diamonds, can play any card
            Card anyClub = players[1].getHand().get(0);
            assertTrue(engine.isValidPlay(1, anyClub));
        }
    }

    @Test
    void testEvaluateTrick() {
        engine.startNewRound();
        engine.declareTrump(0, Suit.HEART);
        List<Card> kittyCards = selectNonJokerKitty(players[0], 10);
        engine.setKitty(kittyCards);

        // Replace all hands with controlled cards
        for (Player p : players) {
            p.getHand().clear();
        }

        // Player 0 leads with ♠A (non-trump, high)
        Card spadeA = new Card(Suit.SPADE, Rank.ACE, 300);
        // Player 1 plays ♠3 (trump rank in other suit)
        Card spade3 = new Card(Suit.SPADE, Rank.THREE, 301);
        // Player 2 plays ♥3 (trump rank in trump suit, highest trump)
        Card heart3 = new Card(Suit.HEART, Rank.THREE, 302);
        // Player 3 plays ♠K (follows suit)
        Card spadeK = new Card(Suit.SPADE, Rank.KING, 303);

        players[0].addCards(List.of(spadeA));
        players[1].addCards(List.of(spade3));
        players[2].addCards(List.of(heart3));
        players[3].addCards(List.of(spadeK));

        engine.playCard(0, spadeA);
        engine.playCard(1, spade3);
        engine.playCard(2, heart3);
        engine.playCard(3, spadeK);

        int winner = engine.evaluateTrick();
        // Player 2 played trump, should win
        assertEquals(2, winner);
    }

    @Test
    void testPointScoring() {
        engine.startNewRound();
        engine.declareTrump(0, Suit.HEART);
        List<Card> kittyCards = selectNonJokerKitty(players[0], 10);
        engine.setKitty(kittyCards);

        for (Player p : players) {
            p.getHand().clear();
        }

        // Players 0 and 2 are team 0 (declarer team since dealer=0)
        // Players 1 and 3 are team 1 (defender team)
        // Player 0 leads with ♠5 (5 points)
        Card spade5 = new Card(Suit.SPADE, Rank.FIVE, 400);
        Card spade4 = new Card(Suit.SPADE, Rank.FOUR, 401);
        Card spade6 = new Card(Suit.SPADE, Rank.SIX, 402);
        // Player 3 plays ♠A (highest of lead suit, wins)
        Card spadeA = new Card(Suit.SPADE, Rank.ACE, 403);

        players[0].addCards(List.of(spade5));
        players[1].addCards(List.of(spade4));
        players[2].addCards(List.of(spade6));
        players[3].addCards(List.of(spadeA));

        engine.playCard(0, spade5);
        engine.playCard(1, spade4);
        engine.playCard(2, spade6);
        engine.playCard(3, spadeA);

        int winner = engine.evaluateTrick();
        // Player 3 wins (highest spade), team 1 = defender
        assertEquals(3, winner);
        // 5 points from ♠5, defender gets them
        assertEquals(5, engine.getDefenderPoints());
    }

    private List<Card> selectNonJokerKitty(Player player, int count) {
        List<Card> result = new ArrayList<>();
        for (Card card : player.getHand()) {
            if (card.getRank() != Rank.SMALL_JOKER && card.getRank() != Rank.BIG_JOKER) {
                result.add(card);
                if (result.size() >= count) break;
            }
        }
        return result;
    }
}
