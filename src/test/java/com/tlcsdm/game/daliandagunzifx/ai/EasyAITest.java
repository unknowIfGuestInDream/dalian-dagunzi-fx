/*
 * Copyright (c) 2026 unknowIfGuestInDream.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *     * Neither the name of unknowIfGuestInDream, any associated website, nor the
 * names of its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UNKNOWIFGUESTINDREAM BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
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
        // 给定两张相同花色点数的A牌（构成强副牌棒子）+ 一张单牌
        Card spadeA1 = new Card(Suit.SPADE, Rank.ACE, 800);
        Card spadeA2 = new Card(Suit.SPADE, Rank.ACE, 801);
        Card spadeQ = new Card(Suit.SPADE, Rank.QUEEN, 802);
        leader.addCards(List.of(spadeA1, spadeA2, spadeQ));

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
        // 给定三张相同花色点数的A牌（构成强副牌滚子）+ 一张单牌
        Card spadeA1 = new Card(Suit.SPADE, Rank.ACE, 800);
        Card spadeA2 = new Card(Suit.SPADE, Rank.ACE, 801);
        Card spadeA3 = new Card(Suit.SPADE, Rank.ACE, 803);
        Card spadeQ = new Card(Suit.SPADE, Rank.QUEEN, 802);
        leader.addCards(List.of(spadeA1, spadeA2, spadeA3, spadeQ));

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

    @Test
    void testPlayPointsForPartnerPrefersHighestPoints() {
        EasyAI ai = new EasyAI();
        com.tlcsdm.game.daliandagunzifx.engine.TrumpInfo trumpInfo =
            new com.tlcsdm.game.daliandagunzifx.engine.TrumpInfo(Suit.HEART, Rank.THREE);

        // K(10分), 10(10分), 5(5分), J(0分)
        Card spadeK = new Card(Suit.SPADE, Rank.KING, 800);
        Card spade10 = new Card(Suit.SPADE, Rank.TEN, 801);
        Card spade5 = new Card(Suit.SPADE, Rank.FIVE, 802);
        Card spadeJ = new Card(Suit.SPADE, Rank.JACK, 803);

        // 有分牌时，应该返回分值最高的牌（K或10，都是10分）
        Card chosen = ai.playPointsForPartner(List.of(spadeJ, spade5, spade10, spadeK), trumpInfo);
        assertTrue(chosen.getPoints() == 10,
            "队友赢时应优先出分值最高的牌(K/10=10分)，实际出了: " + chosen.getRank());

        // 只有非分牌时，应该返回最小的牌
        Card chosenNoPoints = ai.playPointsForPartner(List.of(spadeJ,
            new Card(Suit.SPADE, Rank.QUEEN, 804)), trumpInfo);
        assertEquals(Rank.JACK, chosenNoPoints.getRank(),
            "无分牌时应出最小的牌");
    }

    @Test
    void testVoidInSuitPrefersNonTrump() {
        // Bug 1: 缺门时应垫非主牌，不应出高价值主牌
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

        // Player 0 leads with ♠A
        players[0].getHand().clear();
        Card spadeA = new Card(Suit.SPADE, Rank.ACE, 900);
        players[0].addCards(List.of(spadeA));
        engine.playCard(0, spadeA);

        // Player 1 has NO spades, but has clubs (non-trump) and some trumps
        players[1].getHand().clear();
        Card club4 = new Card(Suit.CLUB, Rank.FOUR, 901);
        Card club7 = new Card(Suit.CLUB, Rank.SEVEN, 902);
        Card heart10 = new Card(Suit.HEART, Rank.TEN, 903);  // 主牌10
        Card smallJoker = new Card(null, Rank.SMALL_JOKER, 904); // 小王
        players[1].addCards(List.of(club4, club7, heart10, smallJoker));

        EasyAI ai = new EasyAI();
        Card chosen = ai.chooseCard(players[1], engine);

        // AI应该出非主牌(club4或club7)，不应浪费主牌10或小王
        assertFalse(engine.getTrumpInfo().isTrump(chosen),
            "缺门时有非主牌可垫，不应出主牌。实际出了: " + chosen.getRank() + " " + chosen.getSuit());
    }

    @Test
    void testFollowTrumpSinglePreservesDiamondTwo() {
        // 方片是主时，跟主牌不应用♦2赢低分墩（♦2是大牌，应保留）
        Player[] players = new Player[]{
            new Player(0, "P0", false),
            new Player(1, "P1", false),
            new Player(2, "P2", false),
            new Player(3, "P3", false)
        };
        GameEngine engine = new GameEngine(players);
        engine.startNewRound();
        // 方片是主，打3级（默认级牌THREE）
        engine.declareTrump(0, Suit.DIAMOND);
        List<Card> kittyCards = players[0].getHand().stream()
            .filter(c -> c.getRank() != Rank.SMALL_JOKER && c.getRank() != Rank.BIG_JOKER)
            .limit(6)
            .toList();
        engine.setKitty(kittyCards);

        // Player 0 leads with ♦10（主牌，10分）
        players[0].getHand().clear();
        Card diamond10 = new Card(Suit.DIAMOND, Rank.TEN, 920);
        players[0].addCards(List.of(diamond10));
        engine.playCard(0, diamond10);

        // Player 1 has ♦2（特殊主牌）、♦4、♦7
        // ♦4(904) 和 ♦7(907) 都打不过 ♦10(910)，只有 ♦2(996) 能赢
        // 但 ♦2 是大牌，不应该为了10分就打出去
        players[1].getHand().clear();
        Card diamond2 = new Card(Suit.DIAMOND, Rank.TWO, 921);
        Card diamond4 = new Card(Suit.DIAMOND, Rank.FOUR, 922);
        Card diamond7 = new Card(Suit.DIAMOND, Rank.SEVEN, 923);
        players[1].addCards(List.of(diamond2, diamond4, diamond7));

        EasyAI ai = new EasyAI();
        Card chosen = ai.chooseCard(players[1], engine);

        // 应出♦4（最小的非特殊主牌），而不是♦2
        assertFalse(ai.isSpecialTrump(chosen, engine.getTrumpInfo()),
            "跟主牌时有非特殊主牌可出，不应出♦2等特殊主牌。实际出了: " + chosen.getRank() + " " + chosen.getSuit());
        assertEquals(Rank.FOUR, chosen.getRank(),
            "应出方片4（最小的非特殊主牌），比如再打3，方片4是最小牌");
    }

    @Test
    void testFollowTrumpBangPlaysSmallTrumps() {
        // Bug 2: 跟主牌棒子时应出小主牌，不应出2/王等特殊主牌
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

        // Player 0 leads with trump BANG (two ♥4)
        players[0].getHand().clear();
        Card heart4a = new Card(Suit.HEART, Rank.FOUR, 910);
        Card heart4b = new Card(Suit.HEART, Rank.FOUR, 911);
        players[0].addCards(List.of(heart4a, heart4b));
        engine.playCards(0, List.of(heart4a, heart4b));

        // Player 1 has a mix of small heart trumps and special trumps
        players[1].getHand().clear();
        Card heart6 = new Card(Suit.HEART, Rank.SIX, 912);
        Card heart8 = new Card(Suit.HEART, Rank.EIGHT, 913);
        Card twoClub = new Card(Suit.CLUB, Rank.TWO, 914);  // 特殊主牌 2
        Card smallJoker = new Card(null, Rank.SMALL_JOKER, 915); // 特殊主牌 小王
        players[1].addCards(List.of(heart6, heart8, twoClub, smallJoker));

        EasyAI ai = new EasyAI();
        List<Card> chosen = ai.chooseCards(players[1], engine);

        assertEquals(2, chosen.size(), "应该出2张牌");
        // 应该出小主牌(heart6, heart8)，不应出2或小王
        for (Card card : chosen) {
            assertFalse(ai.isSpecialTrump(card, engine.getTrumpInfo()),
                "跟主牌棒子有非特殊主牌时，不应出特殊主牌(2/王等)。实际出了: " + card.getRank());
        }
    }
}
