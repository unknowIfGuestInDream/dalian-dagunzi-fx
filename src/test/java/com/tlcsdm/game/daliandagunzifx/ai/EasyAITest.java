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
import com.tlcsdm.game.daliandagunzifx.engine.TrumpInfo;
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

    @Test
    void testChooseKittyCardsKeepsAces() {
        // 庄家扣底牌时不应把A扣下去，A是最大的非主牌
        Player[] players = new Player[]{
            new Player(0, "P0", false),
            new Player(1, "P1", false),
            new Player(2, "P2", false),
            new Player(3, "P3", false)
        };
        GameEngine engine = new GameEngine(players);
        engine.startNewRound();
        engine.declareTrump(0, Suit.HEART);

        // 手动设置手牌：包含A和低牌
        players[0].getHand().clear();
        Card spadeA = new Card(Suit.SPADE, Rank.ACE, 800);
        Card clubA = new Card(Suit.CLUB, Rank.ACE, 801);
        Card spade4 = new Card(Suit.SPADE, Rank.FOUR, 802);
        Card spade6 = new Card(Suit.SPADE, Rank.SIX, 803);
        Card club7 = new Card(Suit.CLUB, Rank.SEVEN, 804);
        Card club8 = new Card(Suit.CLUB, Rank.EIGHT, 805);
        Card diamond4 = new Card(Suit.DIAMOND, Rank.FOUR, 806);
        Card diamond6 = new Card(Suit.DIAMOND, Rank.SIX, 807);
        Card diamond9 = new Card(Suit.DIAMOND, Rank.NINE, 808);
        Card heart4 = new Card(Suit.HEART, Rank.FOUR, 809);
        players[0].addCards(List.of(spadeA, clubA, spade4, spade6, club7, club8,
            diamond4, diamond6, diamond9, heart4));

        var trumpInfo = engine.getTrumpInfo();
        EasyAI ai = new EasyAI();
        List<Card> kitty = ai.chooseKittyCards(players[0], List.of(), trumpInfo);

        assertEquals(6, kitty.size(), "应该扣6张牌");
        // A不应在扣的牌中
        for (Card card : kitty) {
            assertNotEquals(Rank.ACE, card.getRank(),
                "A是最大的非主牌，不应被扣进底牌。实际扣了: " + card.getDisplayName());
        }
    }

    @Test
    void testPlayLowProtectsSpecialTrumps() {
        // playLow应该优先出非特殊主牌的小牌
        var trumpInfo = new TrumpInfo(Suit.HEART, Rank.THREE);
        EasyAI ai = new EasyAI();

        Card heart4 = new Card(Suit.HEART, Rank.FOUR, 900);
        Card heart6 = new Card(Suit.HEART, Rank.SIX, 901);
        Card heart2 = new Card(Suit.HEART, Rank.TWO, 902);
        Card smallJoker = new Card(null, Rank.SMALL_JOKER, 903);

        // 有非特殊主牌时，应出非特殊的最小牌
        Card result = ai.playLow(List.of(heart4, heart6, heart2, smallJoker), trumpInfo);
        assertEquals(heart4, result,
            "有非特殊主牌(♥4, ♥6)时，应出♥4而不是特殊主牌(♥2/小王)。实际出了: " + result.getDisplayName());

        // 只有特殊主牌时，出最小的特殊主牌
        Card result2 = ai.playLow(List.of(heart2, smallJoker), trumpInfo);
        assertEquals(heart2, result2,
            "只有特殊主牌时，应出最小的(♥2)。实际出了: " + result2.getDisplayName());
    }

    @Test
    void testPartnerFollowPlaysLowWhenPartnerWinning() {
        // Issue 2: 队友出A时，我方跟牌应出最小牌，不应跟着出A
        Player[] players = new Player[]{
            new Player(0, "P0", true),
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

        // Player 1 follows with ♠8
        players[1].getHand().clear();
        Card spade8 = new Card(Suit.SPADE, Rank.EIGHT, 901);
        players[1].addCards(List.of(spade8));
        engine.playCard(1, spade8);

        // Player 2 (搭档) has ♠A, ♠4, ♠6 - should play smallest(♠4), not A
        // 注意：trumpRank默认THREE，♠3是主牌，所以用♠4和♠6
        players[2].getHand().clear();
        Card spadeA2 = new Card(Suit.SPADE, Rank.ACE, 902);
        Card spade4 = new Card(Suit.SPADE, Rank.FOUR, 903);
        Card spade6 = new Card(Suit.SPADE, Rank.SIX, 904);
        players[2].addCards(List.of(spadeA2, spade4, spade6));

        EasyAI ai = new EasyAI();
        Card chosen = ai.chooseCard(players[2], engine);

        // 队友(P0)已出A且赢着，P2应出最小的牌(♠4)，不应出A
        assertNotEquals(Rank.ACE, chosen.getRank(),
            "队友已出A且赢着时，不应跟出A，应出小牌。实际出了: " + chosen.getDisplayName());
        assertEquals(Rank.FOUR, chosen.getRank(),
            "应出最小的非分牌(♠4)。实际出了: " + chosen.getDisplayName());
    }

    @Test
    void testOpponentAvoidsDumpingPointsWhenCantWin() {
        // Issue 4: 对手无法赢墩时不应出K等分牌
        Player[] players = new Player[]{
            new Player(0, "P0", true),
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

        // Player 1 (对手) has ♠K, ♠8, ♠4 - should play smallest non-point, not K
        players[1].getHand().clear();
        Card spadeK = new Card(Suit.SPADE, Rank.KING, 901);
        Card spade8 = new Card(Suit.SPADE, Rank.EIGHT, 902);
        Card spade4 = new Card(Suit.SPADE, Rank.FOUR, 903);
        players[1].addCards(List.of(spadeK, spade8, spade4));

        EasyAI ai = new EasyAI();
        Card chosen = ai.chooseCard(players[1], engine);

        // 对手无法赢A，应出最小的非分牌，不应出K(10分)
        assertEquals(0, chosen.getPoints(),
            "无法赢墩时不应出分牌(K=10分)。实际出了: " + chosen.getDisplayName());
        assertEquals(Rank.FOUR, chosen.getRank(),
            "应出最小的非分牌(♠4)。实际出了: " + chosen.getDisplayName());
    }

    @Test
    void testMultiCardFollowPrefersNonTrumpWhenCantWin() {
        // Issue 3: 跟棒子时无法用主牌管上，应垫非主牌而非浪费主牌
        Player[] players = new Player[]{
            new Player(0, "P0", true),
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

        // Player 0 leads with ♠A棒子 (2张♠A)
        players[0].getHand().clear();
        Card spadeA1 = new Card(Suit.SPADE, Rank.ACE, 900);
        Card spadeA2 = new Card(Suit.SPADE, Rank.ACE, 901);
        players[0].addCards(List.of(spadeA1, spadeA2));
        engine.playCards(0, List.of(spadeA1, spadeA2));

        // Player 1 has NO spades, has clubs (non-trump) and individual trump cards (not a pair)
        players[1].getHand().clear();
        Card club4 = new Card(Suit.CLUB, Rank.FOUR, 910);
        Card club7 = new Card(Suit.CLUB, Rank.SEVEN, 911);
        Card heart5 = new Card(Suit.HEART, Rank.FIVE, 912);  // 主牌5
        Card heart8 = new Card(Suit.HEART, Rank.EIGHT, 913);  // 主牌8
        players[1].addCards(List.of(club4, club7, heart5, heart8));

        // 确保Player 2和3有足够的牌
        players[2].getHand().clear();
        players[2].addCards(List.of(
            new Card(Suit.DIAMOND, Rank.FOUR, 920),
            new Card(Suit.DIAMOND, Rank.SIX, 921)));
        players[3].getHand().clear();
        players[3].addCards(List.of(
            new Card(Suit.DIAMOND, Rank.SEVEN, 922),
            new Card(Suit.DIAMOND, Rank.EIGHT, 923)));

        EasyAI ai = new EasyAI();
        List<Card> chosen = ai.chooseCards(players[1], engine);

        assertEquals(2, chosen.size(), "应出2张牌跟棒子");

        // 没有能组成棒子管上A棒子的主牌，应优先垫非主牌
        TrumpInfo trumpInfo = engine.getTrumpInfo();
        for (Card card : chosen) {
            assertFalse(trumpInfo.isTrump(card),
                "无法用主牌管上时应垫非主牌，不应浪费主牌。实际出了: " + card.getDisplayName());
        }
    }

    @Test
    void testFollowSuitOnlyPointCardsShouldTryToWin() {
        // 对方赢时，若手中只有分牌可跟，应尝试赢墩而非白送分给对方
        Player[] players = new Player[]{
            new Player(0, "P0", true),
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

        // Player 0 (庄家) leads with ♠9（0分）
        players[0].getHand().clear();
        Card spade9 = new Card(Suit.SPADE, Rank.NINE, 900);
        players[0].addCards(List.of(spade9));
        engine.playCard(0, spade9);

        // Player 1 (闲家) has only point spades: ♠5(5分), ♠K(10分)
        // 对方(P0)赢着，手中只有分牌 → 应尝试用♠K赢墩
        players[1].getHand().clear();
        Card spade5 = new Card(Suit.SPADE, Rank.FIVE, 901);
        Card spadeK = new Card(Suit.SPADE, Rank.KING, 902);
        players[1].addCards(List.of(spade5, spadeK));

        // 确保其他玩家有牌
        players[2].getHand().clear();
        players[2].addCards(List.of(new Card(Suit.DIAMOND, Rank.FOUR, 910)));
        players[3].getHand().clear();
        players[3].addCards(List.of(new Card(Suit.DIAMOND, Rank.SIX, 911)));

        EasyAI ai = new EasyAI();
        Card chosen = ai.chooseCard(players[1], engine);

        // K♠能赢9♠，应出K♠赢墩，而非出5♠白给对方5分
        assertEquals(Rank.KING, chosen.getRank(),
            "只有分牌可跟且能赢时，应尝试赢墩而非白送分。实际出了: " + chosen.getDisplayName());
    }

    @Test
    void testNoSuitOnlyPointNonTrumpShouldUseTrumpToWin() {
        // 无花色可跟、非主牌全是分牌时，应用主牌赢墩避免送分
        Player[] players = new Player[]{
            new Player(0, "P0", true),
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

        // Player 0 leads with ♠9
        players[0].getHand().clear();
        Card spade9 = new Card(Suit.SPADE, Rank.NINE, 900);
        players[0].addCards(List.of(spade9));
        engine.playCard(0, spade9);

        // Player 1: 无♠可跟，非主牌只有分牌(♣5, ♦K)，有非特殊主牌(♥4)
        // 应用♥4(主牌)赢墩，而非垫♣5送分给对方
        players[1].getHand().clear();
        Card club5 = new Card(Suit.CLUB, Rank.FIVE, 901);
        Card diamondK = new Card(Suit.DIAMOND, Rank.KING, 902);
        Card heart4 = new Card(Suit.HEART, Rank.FOUR, 903);
        players[1].addCards(List.of(club5, diamondK, heart4));

        // 确保其他玩家有牌
        players[2].getHand().clear();
        players[2].addCards(List.of(new Card(Suit.DIAMOND, Rank.FOUR, 910)));
        players[3].getHand().clear();
        players[3].addCards(List.of(new Card(Suit.DIAMOND, Rank.SIX, 911)));

        EasyAI ai = new EasyAI();
        TrumpInfo trumpInfo = engine.getTrumpInfo();
        Card chosen = ai.chooseCard(players[1], engine);

        // 应出主牌赢墩，避免送分
        assertTrue(trumpInfo.isTrump(chosen),
            "非主牌全是分牌时，应用主牌赢墩而非送分给对方。实际出了: " + chosen.getDisplayName());
    }
}
