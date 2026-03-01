package com.tlcsdm.game.daliandagunzifx.ai;

import com.tlcsdm.game.daliandagunzifx.engine.GameEngine;
import com.tlcsdm.game.daliandagunzifx.engine.Player;
import com.tlcsdm.game.daliandagunzifx.model.Card;
import com.tlcsdm.game.daliandagunzifx.model.PlayType;
import com.tlcsdm.game.daliandagunzifx.model.Rank;
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
        List<Card> kittyCards = players[0].getHand().stream()
            .filter(c -> c.getRank() != Rank.SMALL_JOKER && c.getRank() != Rank.BIG_JOKER)
            .limit(6)
            .toList();
        engine.setKitty(kittyCards);

        EasyAI ai = new EasyAI();
        Player currentPlayer = players[engine.getCurrentPlayerIndex()];
        Card chosen = ai.chooseCard(currentPlayer, engine);

        assertNotNull(chosen);
        assertTrue(currentPlayer.getHand().contains(chosen));
        assertTrue(engine.isValidPlay(currentPlayer.getId(), chosen));
    }

    @Test
    void testLeadAvoidsPointCards() {
        Player[] players = new Player[]{
            new Player(0, "P0", false),
            new Player(1, "P1", false),
            new Player(2, "P2", false),
            new Player(3, "P3", false)
        };
        GameEngine engine = new GameEngine(players);
        engine.startNewRound();
        engine.declareTrump(0, Suit.HEART);
        List<Card> kittyCards = players[0].getHand().stream()
            .filter(c -> c.getRank() != Rank.SMALL_JOKER && c.getRank() != Rank.BIG_JOKER)
            .limit(6)
            .toList();
        engine.setKitty(kittyCards);

        Player leader = players[engine.getCurrentPlayerIndex()];
        leader.getHand().clear();
        // Give the player only 10, 5, and Q of spades - non-trump cards
        Card spade10 = new Card(Suit.SPADE, Rank.TEN, 800);
        Card spade5 = new Card(Suit.SPADE, Rank.FIVE, 801);
        Card spadeQ = new Card(Suit.SPADE, Rank.QUEEN, 802);
        leader.addCards(List.of(spade10, spade5, spadeQ));

        EasyAI ai = new EasyAI();
        Card chosen = ai.chooseCard(leader, engine);

        // AI should lead with Q (non-point) instead of 10 or 5 (point cards)
        assertEquals(Rank.QUEEN, chosen.getRank(),
            "AI should avoid leading with point cards (10, 5) when non-point cards are available");
    }

    @Test
    void testLeadWithBangWhenAvailable() {
        Player[] players = new Player[]{
            new Player(0, "P0", false),
            new Player(1, "P1", false),
            new Player(2, "P2", false),
            new Player(3, "P3", false)
        };
        GameEngine engine = new GameEngine(players);
        engine.startNewRound();
        engine.declareTrump(0, Suit.HEART);
        List<Card> kittyCards = players[0].getHand().stream()
            .filter(c -> c.getRank() != Rank.SMALL_JOKER && c.getRank() != Rank.BIG_JOKER)
            .limit(6)
            .toList();
        engine.setKitty(kittyCards);

        Player leader = players[engine.getCurrentPlayerIndex()];
        leader.getHand().clear();
        // 给出两张相同花色点数的牌（构成棒子）+ 一张单牌
        Card spadeJ1 = new Card(Suit.SPADE, Rank.JACK, 800);
        Card spadeJ2 = new Card(Suit.SPADE, Rank.JACK, 801);
        Card spadeQ = new Card(Suit.SPADE, Rank.QUEEN, 802);
        leader.addCards(List.of(spadeJ1, spadeJ2, spadeQ));

        EasyAI ai = new EasyAI();
        List<Card> chosen = ai.chooseCards(leader, engine);

        // AI应该主动出棒子（对子）
        assertEquals(2, chosen.size(), "AI应主动出对子(棒子)");
        PlayType playType = engine.determinePlayType(chosen);
        assertEquals(PlayType.BANG, playType, "出的牌应该是棒子(BANG)类型");
    }

    @Test
    void testLeadWithGunziWhenAvailable() {
        Player[] players = new Player[]{
            new Player(0, "P0", false),
            new Player(1, "P1", false),
            new Player(2, "P2", false),
            new Player(3, "P3", false)
        };
        GameEngine engine = new GameEngine(players);
        engine.startNewRound();
        engine.declareTrump(0, Suit.HEART);
        List<Card> kittyCards = players[0].getHand().stream()
            .filter(c -> c.getRank() != Rank.SMALL_JOKER && c.getRank() != Rank.BIG_JOKER)
            .limit(6)
            .toList();
        engine.setKitty(kittyCards);

        Player leader = players[engine.getCurrentPlayerIndex()];
        leader.getHand().clear();
        // 给出三张相同花色点数的牌（构成滚子）+ 一张单牌
        Card spadeJ1 = new Card(Suit.SPADE, Rank.JACK, 800);
        Card spadeJ2 = new Card(Suit.SPADE, Rank.JACK, 801);
        Card spadeJ3 = new Card(Suit.SPADE, Rank.JACK, 803);
        Card spadeQ = new Card(Suit.SPADE, Rank.QUEEN, 802);
        leader.addCards(List.of(spadeJ1, spadeJ2, spadeJ3, spadeQ));

        EasyAI ai = new EasyAI();
        List<Card> chosen = ai.chooseCards(leader, engine);

        // AI应该主动出滚子
        assertEquals(3, chosen.size(), "AI应主动出滚子");
        PlayType playType = engine.determinePlayType(chosen);
        assertEquals(PlayType.GUNZI, playType, "出的牌应该是滚子(GUNZI)类型");
    }

    @Test
    void testLeadAvoidsKingPointCard() {
        Player[] players = new Player[]{
            new Player(0, "P0", false),
            new Player(1, "P1", false),
            new Player(2, "P2", false),
            new Player(3, "P3", false)
        };
        GameEngine engine = new GameEngine(players);
        engine.startNewRound();
        engine.declareTrump(0, Suit.HEART);
        List<Card> kittyCards = players[0].getHand().stream()
            .filter(c -> c.getRank() != Rank.SMALL_JOKER && c.getRank() != Rank.BIG_JOKER)
            .limit(6)
            .toList();
        engine.setKitty(kittyCards);

        Player leader = players[engine.getCurrentPlayerIndex()];
        leader.getHand().clear();
        // K(分牌) 和 J(非分牌)
        Card spadeK = new Card(Suit.SPADE, Rank.KING, 800);
        Card spadeJ = new Card(Suit.SPADE, Rank.JACK, 801);
        leader.addCards(List.of(spadeK, spadeJ));

        EasyAI ai = new EasyAI();
        Card chosen = ai.chooseCard(leader, engine);

        // AI不应该主动出K（分牌），应该出J
        assertEquals(Rank.JACK, chosen.getRank(),
            "AI不应该主动出K(分牌)，应该选非分牌J");
    }
}
