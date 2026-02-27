package com.tlcsdm.game.daliandagunzifx.engine;

import com.tlcsdm.game.daliandagunzifx.model.Card;
import com.tlcsdm.game.daliandagunzifx.model.Rank;
import com.tlcsdm.game.daliandagunzifx.model.Suit;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GameEngineCopyTest {

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
    void testCopyPreservesPhase() {
        GameEngine engine = setupGame();
        GameEngine copy = engine.copy();
        assertEquals(engine.getPhase(), copy.getPhase());
    }

    @Test
    void testCopyPreservesTrumpInfo() {
        GameEngine engine = setupGame();
        GameEngine copy = engine.copy();
        assertNotNull(copy.getTrumpInfo());
        assertEquals(engine.getTrumpInfo().getTrumpSuit(), copy.getTrumpInfo().getTrumpSuit());
        assertEquals(engine.getTrumpInfo().getTrumpRank(), copy.getTrumpInfo().getTrumpRank());
    }

    @Test
    void testCopyPreservesPlayerHands() {
        GameEngine engine = setupGame();
        GameEngine copy = engine.copy();
        for (int i = 0; i < 4; i++) {
            assertEquals(engine.getPlayers()[i].getHand().size(),
                copy.getPlayers()[i].getHand().size());
            assertEquals(engine.getPlayers()[i].getId(),
                copy.getPlayers()[i].getId());
        }
    }

    @Test
    void testCopyIsIndependent() {
        GameEngine engine = setupGame();
        GameEngine copy = engine.copy();

        // Play a card in the copy
        Player copyPlayer = copy.getPlayers()[copy.getCurrentPlayerIndex()];
        Card card = copyPlayer.getHand().get(0);
        if (copy.isValidPlay(copyPlayer.getId(), card)) {
            copy.playCard(copyPlayer.getId(), card);
        }

        // Original should be unaffected
        assertEquals(0, engine.getTrickCardsPlayed());
    }

    @Test
    void testCopyPreservesCurrentPlayerIndex() {
        GameEngine engine = setupGame();
        GameEngine copy = engine.copy();
        assertEquals(engine.getCurrentPlayerIndex(), copy.getCurrentPlayerIndex());
    }

    @Test
    void testCopyPreservesDealerAndTrickLeader() {
        GameEngine engine = setupGame();
        GameEngine copy = engine.copy();
        assertEquals(engine.getDealerIndex(), copy.getDealerIndex());
        assertEquals(engine.getCurrentTrickLeader(), copy.getCurrentTrickLeader());
    }

    @Test
    void testCopyPreservesDefenderPoints() {
        GameEngine engine = setupGame();
        GameEngine copy = engine.copy();
        assertEquals(engine.getDefenderPoints(), copy.getDefenderPoints());
    }

    @Test
    void testCopyAfterPlayPreservesTrickState() {
        GameEngine engine = setupGame();

        // Play one card
        Player player = engine.getPlayers()[engine.getCurrentPlayerIndex()];
        Card card = player.getHand().stream()
            .filter(c -> engine.isValidPlay(player.getId(), c))
            .findFirst().orElseThrow();
        engine.playCard(player.getId(), card);

        // Copy after a card has been played
        GameEngine copy = engine.copy();
        assertEquals(1, copy.getTrickCardsPlayed());
        assertEquals(engine.getCurrentTrickPlayType(), copy.getCurrentTrickPlayType());
    }

    @Test
    void testPlayerCopyIsIndependent() {
        Player original = new Player(0, "Test", false);
        original.addCards(List.of(new Card(Suit.HEART, Rank.ACE, 0)));

        Player copy = original.copy();
        assertEquals(1, copy.getHand().size());

        // Modify copy
        copy.getHand().clear();
        assertEquals(1, original.getHand().size(), "Original should not be affected");
    }
}
