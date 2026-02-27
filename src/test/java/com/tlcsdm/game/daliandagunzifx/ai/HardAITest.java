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

class HardAITest {

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
    void testHardAIChooseCardReturnsValidCard() {
        GameEngine engine = setupGame();
        CardTracker tracker = new CardTracker();
        HardAI ai = new HardAI(tracker);

        Player currentPlayer = engine.getPlayers()[engine.getCurrentPlayerIndex()];
        Card chosen = ai.chooseCard(currentPlayer, engine);

        assertNotNull(chosen);
        assertTrue(currentPlayer.getHand().contains(chosen));
        assertTrue(engine.isValidPlay(currentPlayer.getId(), chosen));
    }

    @Test
    void testHardAIChooseCardsReturnsValidPlay() {
        GameEngine engine = setupGame();
        CardTracker tracker = new CardTracker();
        HardAI ai = new HardAI(tracker);

        Player currentPlayer = engine.getPlayers()[engine.getCurrentPlayerIndex()];
        List<Card> chosen = ai.chooseCards(currentPlayer, engine);

        assertNotNull(chosen);
        assertFalse(chosen.isEmpty());
        assertTrue(currentPlayer.getHand().containsAll(chosen));
        assertTrue(engine.isValidPlay(currentPlayer.getId(), chosen));
    }

    @Test
    void testHardAIChooseCardAsFollower() {
        GameEngine engine = setupGame();
        CardTracker tracker = new CardTracker();
        HardAI ai = new HardAI(tracker);

        // Leader plays a card
        Player leader = engine.getPlayers()[engine.getCurrentPlayerIndex()];
        Card leaderCard = leader.getHand().stream()
            .filter(c -> engine.isValidPlay(leader.getId(), c))
            .findFirst().orElseThrow();
        engine.playCard(leader.getId(), leaderCard);

        // AI follows
        Player follower = engine.getPlayers()[engine.getCurrentPlayerIndex()];
        Card chosen = ai.chooseCard(follower, engine);

        assertNotNull(chosen);
        assertTrue(follower.getHand().contains(chosen));
        assertTrue(engine.isValidPlay(follower.getId(), chosen));
    }

    @Test
    void testHardAIDoesNotModifyOriginalEngine() {
        GameEngine engine = setupGame();
        CardTracker tracker = new CardTracker();
        HardAI ai = new HardAI(tracker);

        Player currentPlayer = engine.getPlayers()[engine.getCurrentPlayerIndex()];
        int originalHandSize = currentPlayer.getHand().size();
        int originalTrickCardsPlayed = engine.getTrickCardsPlayed();

        ai.chooseCard(currentPlayer, engine);

        // Verify engine state is unchanged after AI decision
        assertEquals(originalHandSize, currentPlayer.getHand().size());
        assertEquals(originalTrickCardsPlayed, engine.getTrickCardsPlayed());
    }

    @Test
    void testHardAIChooseTrumpSuit() {
        Player player = new Player(0, "Test", false);
        // Give player cards with 2 spade-3s (making spade declarable)
        player.addCards(List.of(
            new Card(Suit.SPADE, Rank.THREE, 0),
            new Card(Suit.SPADE, Rank.THREE, 1),
            new Card(Suit.SPADE, Rank.ACE, 2),
            new Card(Suit.HEART, Rank.FOUR, 3)
        ));

        CardTracker tracker = new CardTracker();
        HardAI ai = new HardAI(tracker);
        Suit suit = ai.chooseTrumpSuit(player, Rank.THREE);
        assertEquals(Suit.SPADE, suit);
    }

    @Test
    void testHardAIChooseKittyCards() {
        GameEngine engine = setupGame();
        CardTracker tracker = new CardTracker();
        HardAI ai = new HardAI(tracker);

        Player dealer = engine.getPlayers()[engine.getDealerIndex()];
        List<Card> kittyCards = ai.chooseKittyCards(dealer, engine.getKitty(), engine.getTrumpInfo());

        assertNotNull(kittyCards);
        assertEquals(6, kittyCards.size());
        // All kitty cards should be from dealer's hand
        assertTrue(dealer.getHand().containsAll(kittyCards));
    }
}
