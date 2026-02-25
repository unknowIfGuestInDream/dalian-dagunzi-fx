package com.tlcsdm.game.daliandagunzifx.ai;

import com.tlcsdm.game.daliandagunzifx.engine.GameEngine;
import com.tlcsdm.game.daliandagunzifx.engine.Player;
import com.tlcsdm.game.daliandagunzifx.model.Card;
import com.tlcsdm.game.daliandagunzifx.model.Suit;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EasyAITest {

    @Test
    void testChooseCardReturnsValidCard() {
        Player[] players = new Player[]{
            new Player(0, "P0", false),
            new Player(1, "P1", false),
            new Player(2, "P2", false),
            new Player(3, "P3", false)
        };
        GameEngine engine = new GameEngine(players);
        engine.startNewRound();
        engine.declareTrump(0, Suit.SPADE);
        List<Card> kittyCards = List.copyOf(players[0].getHand().subList(0, 8));
        engine.setKitty(kittyCards);

        EasyAI ai = new EasyAI();
        Player currentPlayer = players[engine.getCurrentPlayerIndex()];
        Card chosen = ai.chooseCard(currentPlayer, engine);

        assertNotNull(chosen);
        assertTrue(currentPlayer.getHand().contains(chosen));
        assertTrue(engine.isValidPlay(currentPlayer.getId(), chosen));
    }
}
