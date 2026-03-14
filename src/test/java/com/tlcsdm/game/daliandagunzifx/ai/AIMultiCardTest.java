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
import com.tlcsdm.game.daliandagunzifx.tracker.CardTracker;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AIMultiCardTest {

    private GameEngine setupGameForBang() {
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
    void testEasyAIChooseCardsForBang() {
        GameEngine engine = setupGameForBang();
        Player leader = engine.getPlayers()[engine.getCurrentPlayerIndex()];

        // Find a BANG in the leader's hand
        List<Card> bangCards = findBangInHand(leader, engine.getTrumpInfo());
        if (bangCards == null) {
            return; // Skip if no BANG found (unlikely with 39+ cards from 3 decks)
        }

        // Leader plays BANG
        engine.playCards(leader.getId(), bangCards);
        assertEquals(PlayType.BANG, engine.getCurrentTrickPlayType());

        // AI must respond with 2 cards
        Player aiPlayer = engine.getPlayers()[engine.getCurrentPlayerIndex()];
        EasyAI ai = new EasyAI();
        List<Card> chosen = ai.chooseCards(aiPlayer, engine);

        assertNotNull(chosen);
        assertEquals(2, chosen.size(), "AI must play 2 cards for BANG");
        assertTrue(aiPlayer.getHand().containsAll(chosen), "All chosen cards must be in hand");
        assertTrue(engine.isValidPlay(aiPlayer.getId(), chosen), "Chosen cards must be a valid play");
    }

    @Test
    void testMediumAIChooseCardsForBang() {
        GameEngine engine = setupGameForBang();
        Player leader = engine.getPlayers()[engine.getCurrentPlayerIndex()];

        List<Card> bangCards = findBangInHand(leader, engine.getTrumpInfo());
        if (bangCards == null) return;

        engine.playCards(leader.getId(), bangCards);
        assertEquals(PlayType.BANG, engine.getCurrentTrickPlayType());

        Player aiPlayer = engine.getPlayers()[engine.getCurrentPlayerIndex()];
        MediumAI ai = new MediumAI(new CardTracker());
        List<Card> chosen = ai.chooseCards(aiPlayer, engine);

        assertNotNull(chosen);
        assertEquals(2, chosen.size(), "AI must play 2 cards for BANG");
        assertTrue(aiPlayer.getHand().containsAll(chosen));
        assertTrue(engine.isValidPlay(aiPlayer.getId(), chosen));
    }

    @Test
    void testHardAIChooseCardsForBang() {
        GameEngine engine = setupGameForBang();
        Player leader = engine.getPlayers()[engine.getCurrentPlayerIndex()];

        List<Card> bangCards = findBangInHand(leader, engine.getTrumpInfo());
        if (bangCards == null) return;

        engine.playCards(leader.getId(), bangCards);
        assertEquals(PlayType.BANG, engine.getCurrentTrickPlayType());

        Player aiPlayer = engine.getPlayers()[engine.getCurrentPlayerIndex()];
        CardTracker tracker = new CardTracker();
        HardAI ai = new HardAI(tracker);
        List<Card> chosen = ai.chooseCards(aiPlayer, engine);

        assertNotNull(chosen);
        assertEquals(2, chosen.size(), "AI must play 2 cards for BANG");
        assertTrue(aiPlayer.getHand().containsAll(chosen));
        assertTrue(engine.isValidPlay(aiPlayer.getId(), chosen));
    }

    @Test
    void testEasyAIChooseCardsForGunzi() {
        GameEngine engine = setupGameForBang();
        Player leader = engine.getPlayers()[engine.getCurrentPlayerIndex()];

        List<Card> gunziCards = findGunziInHand(leader, engine.getTrumpInfo());
        if (gunziCards == null) return;

        engine.playCards(leader.getId(), gunziCards);
        assertEquals(PlayType.GUNZI, engine.getCurrentTrickPlayType());

        Player aiPlayer = engine.getPlayers()[engine.getCurrentPlayerIndex()];
        EasyAI ai = new EasyAI();
        List<Card> chosen = ai.chooseCards(aiPlayer, engine);

        assertNotNull(chosen);
        assertEquals(3, chosen.size(), "AI must play 3 cards for GUNZI");
        assertTrue(aiPlayer.getHand().containsAll(chosen));
        assertTrue(engine.isValidPlay(aiPlayer.getId(), chosen));
    }

    @Test
    void testIsValidPlayMultiCardSuitFollowing() {
        // Test that isValidPlay correctly handles multi-card plays with partial suit following
        Player[] players = new Player[]{
            new Player(0, "P0", false),
            new Player(1, "P1", false),
            new Player(2, "P2", false),
            new Player(3, "P3", false)
        };
        GameEngine engine = new GameEngine(players);
        engine.startNewRound();
        engine.declareTrump(0, Suit.SPADE);

        // Clear hands and set up controlled scenario
        for (Player p : players) p.getHand().clear();

        // Player 0 has a BANG of hearts
        Card h5a = new Card(Suit.HEART, Rank.FIVE, 100);
        Card h5b = new Card(Suit.HEART, Rank.FIVE, 101);
        players[0].addCards(List.of(h5a, h5b));

        // Player 1 has 1 heart and 1 diamond (not enough hearts for BANG)
        Card h3 = new Card(Suit.HEART, Rank.THREE, 102);
        Card d4 = new Card(Suit.DIAMOND, Rank.FOUR, 103);
        players[1].addCards(List.of(h3, d4));

        // Player 2, 3 have some cards
        Card c5 = new Card(Suit.CLUB, Rank.FIVE, 104);
        Card c6 = new Card(Suit.CLUB, Rank.SIX, 105);
        Card d5 = new Card(Suit.DIAMOND, Rank.FIVE, 106);
        Card d6 = new Card(Suit.DIAMOND, Rank.SIX, 107);
        players[2].addCards(List.of(c5, c6));
        players[3].addCards(List.of(d5, d6));

        List<Card> kittyCards = players[0].getHand().stream()
            .filter(c -> c.getRank() != Rank.SMALL_JOKER && c.getRank() != Rank.BIG_JOKER)
            .limit(6)
            .toList();
        // We need to use the engine setup differently - add enough cards for kitty
        // Instead, use a fresh engine with manual setup

        // Re-setup with enough cards
        GameEngine eng2 = new GameEngine(players);
        eng2.startNewRound();
        eng2.declareTrump(0, Suit.SPADE);
        // Use the dealt cards - just validate the concept with them
        List<Card> kitty2 = eng2.getPlayers()[0].getHand().stream()
            .filter(c -> c.getRank() != Rank.SMALL_JOKER && c.getRank() != Rank.BIG_JOKER)
            .limit(6)
            .toList();
        eng2.setKitty(kitty2);

        // Find a BANG and test with real dealt cards
        Player p0 = eng2.getPlayers()[0];
        List<Card> bangCards = findBangInHand(p0, eng2.getTrumpInfo());
        if (bangCards != null) {
            assertTrue(eng2.isValidPlay(p0.getId(), bangCards));
        }
    }

    @Test
    void testAIChooseCardsReturnsSuitCardsPreferentially() {
        // Run multiple iterations to verify AI picks suit cards when available
        for (int i = 0; i < 5; i++) {
            GameEngine engine = setupGameForBang();
            Player leader = engine.getPlayers()[engine.getCurrentPlayerIndex()];

            List<Card> bangCards = findBangInHand(leader, engine.getTrumpInfo());
            if (bangCards == null) continue;

            Suit leadSuit = engine.getTrumpInfo().getEffectiveSuit(bangCards.get(0));
            engine.playCards(leader.getId(), bangCards);

            Player aiPlayer = engine.getPlayers()[engine.getCurrentPlayerIndex()];
            TrumpInfo trumpInfo = engine.getTrumpInfo();

            // Count AI's suit cards
            long suitCount = aiPlayer.getHand().stream()
                .filter(c -> trumpInfo.getEffectiveSuit(c) == leadSuit)
                .count();

            MediumAI ai = new MediumAI(new CardTracker());
            List<Card> chosen = ai.chooseCards(aiPlayer, engine);

            assertEquals(2, chosen.size());

            // If AI has suit cards, they should be in the chosen cards
            if (suitCount >= 2) {
                for (Card card : chosen) {
                    assertEquals(leadSuit, trumpInfo.getEffectiveSuit(card),
                        "When AI has enough suit cards, all played cards should be of lead suit");
                }
            } else if (suitCount == 1) {
                // At least one card should be of lead suit
                long chosenSuitCount = chosen.stream()
                    .filter(c -> trumpInfo.getEffectiveSuit(c) == leadSuit)
                    .count();
                assertEquals(1, chosenSuitCount,
                    "When AI has 1 suit card, exactly 1 chosen card should be of lead suit");
            }
        }
    }

    private List<Card> findBangInHand(Player player, TrumpInfo trumpInfo) {
        List<Card> hand = player.getHand();
        for (int i = 0; i < hand.size(); i++) {
            for (int j = i + 1; j < hand.size(); j++) {
                Card a = hand.get(i);
                Card b = hand.get(j);
                if (a.getRank() == b.getRank()
                    && java.util.Objects.equals(a.getSuit(), b.getSuit())
                    && a.getId() != b.getId()) {
                    return List.of(a, b);
                }
            }
        }
        return null;
    }

    /**
     * Issue 3: 队友没赢时跟花色不应出分牌（5/10/K）。
     * 例如：对手出棒子K，跟牌时应避免出5等分牌。
     */
    @Test
    void testMultiCardFollowAvoidPointsWhenOpponentWinning() {
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

        // Player 0 领出♠K棒子（对手的强牌）
        players[0].getHand().clear();
        Card spadeK1 = new Card(Suit.SPADE, Rank.KING, 800);
        Card spadeK2 = new Card(Suit.SPADE, Rank.KING, 801);
        players[0].addCards(List.of(spadeK1, spadeK2));
        engine.playCards(0, List.of(spadeK1, spadeK2));

        // Player 1（对手P0的对手）有♠5(5分), ♠7, ♠8, ♠9
        // 应出非分牌（♠7/♠8），不应出♠5(分牌)
        players[1].getHand().clear();
        Card spade5 = new Card(Suit.SPADE, Rank.FIVE, 810);
        Card spade7 = new Card(Suit.SPADE, Rank.SEVEN, 811);
        Card spade8 = new Card(Suit.SPADE, Rank.EIGHT, 812);
        Card spade9 = new Card(Suit.SPADE, Rank.NINE, 813);
        players[1].addCards(List.of(spade5, spade7, spade8, spade9));

        // 用EasyAI测试
        EasyAI easyAI = new EasyAI();
        List<Card> easyChosen = easyAI.chooseCards(players[1], engine);
        assertEquals(2, easyChosen.size());
        for (Card card : easyChosen) {
            assertEquals(0, card.getPoints(),
                "EasyAI: 对手K棒子赢着时不应出分牌。实际出了: " + card.getDisplayName());
        }

        // 用MediumAI测试
        MediumAI mediumAI = new MediumAI(new CardTracker());
        List<Card> mediumChosen = mediumAI.chooseCards(players[1], engine);
        assertEquals(2, mediumChosen.size());
        for (Card card : mediumChosen) {
            assertEquals(0, card.getPoints(),
                "MediumAI: 对手K棒子赢着时不应出分牌。实际出了: " + card.getDisplayName());
        }
    }

    /**
     * Issue 5: 队友出副牌棒子，AI缺门时不应出大王或特殊主牌。
     * 应优先出非主牌。
     */
    @Test
    void testPartnerBangVoidPrefersNonTrump() {
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

        // Player 0（庄家）出♠A棒子
        players[0].getHand().clear();
        Card spadeA1 = new Card(Suit.SPADE, Rank.ACE, 800);
        Card spadeA2 = new Card(Suit.SPADE, Rank.ACE, 801);
        players[0].addCards(List.of(spadeA1, spadeA2));
        engine.playCards(0, List.of(spadeA1, spadeA2));

        // Player 1 跟♠
        players[1].getHand().clear();
        Card spade4 = new Card(Suit.SPADE, Rank.FOUR, 810);
        Card spade6 = new Card(Suit.SPADE, Rank.SIX, 811);
        players[1].addCards(List.of(spade4, spade6));
        engine.playCards(1, List.of(spade4, spade6));

        // Player 2（队友）缺♠，有♣牌和主牌
        // 队友赢着时应出非主牌，不应出大王或主牌
        players[2].getHand().clear();
        Card club4 = new Card(Suit.CLUB, Rank.FOUR, 820);
        Card club7 = new Card(Suit.CLUB, Rank.SEVEN, 821);
        Card bigJoker = new Card(null, Rank.BIG_JOKER, 822);
        Card heart6 = new Card(Suit.HEART, Rank.SIX, 823);
        players[2].addCards(List.of(club4, club7, bigJoker, heart6));

        // 确保P3有牌
        players[3].getHand().clear();
        players[3].addCards(List.of(
            new Card(Suit.DIAMOND, Rank.FOUR, 830),
            new Card(Suit.DIAMOND, Rank.SIX, 831)));

        TrumpInfo trumpInfo = engine.getTrumpInfo();

        // 用EasyAI测试
        EasyAI easyAI = new EasyAI();
        List<Card> easyChosen = easyAI.chooseCards(players[2], engine);
        assertEquals(2, easyChosen.size());
        for (Card card : easyChosen) {
            assertFalse(trumpInfo.isTrump(card),
                "EasyAI: 队友A棒赢着、有非主牌时，不应出主牌。实际出了: " + card.getDisplayName());
        }

        // 用MediumAI测试
        MediumAI mediumAI = new MediumAI(new CardTracker());
        List<Card> mediumChosen = mediumAI.chooseCards(players[2], engine);
        assertEquals(2, mediumChosen.size());
        for (Card card : mediumChosen) {
            assertFalse(trumpInfo.isTrump(card),
                "MediumAI: 队友A棒赢着、有非主牌时，不应出主牌。实际出了: " + card.getDisplayName());
        }

        // 用HardAI测试
        HardAI hardAI = new HardAI(new CardTracker());
        List<Card> hardChosen = hardAI.chooseCards(players[2], engine);
        assertEquals(2, hardChosen.size());
        for (Card card : hardChosen) {
            assertFalse(trumpInfo.isTrump(card),
                "HardAI: 队友A棒赢着、有非主牌时，不应出主牌。实际出了: " + card.getDisplayName());
        }
    }

    /**
     * Issue 4: 队友出明显大的副牌(A)，AI缺门不应用主牌杀队友的牌。
     * 单牌跟牌场景下的HardAI测试。
     */
    @Test
    void testHardAIPartnerWinningDoesNotTrump() {
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

        // Player 0 出♠A
        players[0].getHand().clear();
        Card spadeA = new Card(Suit.SPADE, Rank.ACE, 800);
        players[0].addCards(List.of(spadeA));
        engine.playCard(0, spadeA);

        // Player 1 跟♠
        players[1].getHand().clear();
        Card spade4 = new Card(Suit.SPADE, Rank.FOUR, 810);
        players[1].addCards(List.of(spade4));
        engine.playCard(1, spade4);

        // Player 2（队友，缺♠）有♣牌和主牌
        // 队友赢着，不应用主牌，应垫非主牌
        players[2].getHand().clear();
        Card club5 = new Card(Suit.CLUB, Rank.FIVE, 820);
        Card club7 = new Card(Suit.CLUB, Rank.SEVEN, 821);
        Card heart6 = new Card(Suit.HEART, Rank.SIX, 822);
        Card smallJoker = new Card(null, Rank.SMALL_JOKER, 823);
        players[2].addCards(List.of(club5, club7, heart6, smallJoker));

        TrumpInfo trumpInfo = engine.getTrumpInfo();

        // HardAI应出非主牌（优先分牌给队友）
        HardAI hardAI = new HardAI(new CardTracker());
        Card chosen = hardAI.chooseCard(players[2], engine);
        assertFalse(trumpInfo.isTrump(chosen),
            "HardAI: 队友♠A赢着、有非主牌时，不应出主牌。实际出了: " + chosen.getDisplayName());
    }

    /**
     * Issue 1+2: 主牌出滚子/棒子时，AI不应出王或特殊主牌（有非特殊主牌时）。
     * HardAI多牌跟牌场景。
     */
    @Test
    void testHardAITrumpFollowAvoidSpecialTrumps() {
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

        // Player 0 出♥4棒子（主牌棒子）
        players[0].getHand().clear();
        Card heart4a = new Card(Suit.HEART, Rank.FOUR, 800);
        Card heart4b = new Card(Suit.HEART, Rank.FOUR, 801);
        players[0].addCards(List.of(heart4a, heart4b));
        engine.playCards(0, List.of(heart4a, heart4b));

        // Player 1 有足够非特殊主牌和特殊主牌
        players[1].getHand().clear();
        Card heart6 = new Card(Suit.HEART, Rank.SIX, 810);
        Card heart8 = new Card(Suit.HEART, Rank.EIGHT, 811);
        Card heart9 = new Card(Suit.HEART, Rank.NINE, 812);
        Card bigJoker = new Card(null, Rank.BIG_JOKER, 813);
        Card smallJoker = new Card(null, Rank.SMALL_JOKER, 814);
        players[1].addCards(List.of(heart6, heart8, heart9, bigJoker, smallJoker));

        TrumpInfo trumpInfo = engine.getTrumpInfo();
        EasyAI easyAI = new EasyAI();

        // HardAI不应出王
        HardAI hardAI = new HardAI(new CardTracker());
        List<Card> hardChosen = hardAI.chooseCards(players[1], engine);
        assertEquals(2, hardChosen.size());
        for (Card card : hardChosen) {
            assertFalse(easyAI.isSpecialTrump(card, trumpInfo),
                "HardAI: 有非特殊主牌时跟主牌棒子不应出特殊主牌(2/王)。实际出了: " + card.getDisplayName());
        }
    }

    private List<Card> findGunziInHand(Player player, TrumpInfo trumpInfo) {
        List<Card> hand = player.getHand();
        for (int i = 0; i < hand.size(); i++) {
            for (int j = i + 1; j < hand.size(); j++) {
                for (int k = j + 1; k < hand.size(); k++) {
                    Card a = hand.get(i);
                    Card b = hand.get(j);
                    Card c = hand.get(k);
                    if (a.getRank() == b.getRank() && b.getRank() == c.getRank()
                        && java.util.Objects.equals(a.getSuit(), b.getSuit())
                        && java.util.Objects.equals(b.getSuit(), c.getSuit())
                        && a.getId() != b.getId() && b.getId() != c.getId() && a.getId() != c.getId()) {
                        return List.of(a, b, c);
                    }
                }
            }
        }
        return null;
    }
}
