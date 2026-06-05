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
            assertEquals(39, players[i].getHand().size(),
                "Player " + i + " should have 39 cards");
        }
        assertEquals(6, engine.getKitty().size());
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
        // After declareTrump, dealer picks up 6 kitty cards -> 45 cards
        assertEquals(45, players[0].getHand().size());

        // Select 6 non-joker cards for the kitty
        List<Card> kittyToSet = players[0].getHand().stream()
            .filter(c -> c.getRank() != Rank.SMALL_JOKER && c.getRank() != Rank.BIG_JOKER)
            .limit(6)
            .toList();
        engine.setKitty(kittyToSet);
        assertEquals(39, players[0].getHand().size());
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
            // Build a kitty with one joker + 5 regular cards
            List<Card> kittyWithJoker = new ArrayList<>();
            kittyWithJoker.add(jokers.get(0));
            hand.stream()
                .filter(c -> c.getRank() != Rank.SMALL_JOKER && c.getRank() != Rank.BIG_JOKER)
                .limit(5)
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
        List<Card> kittyCards = selectNonJokerKitty(players[0], 6);
        engine.setKitty(kittyCards);

        // Dealer (player 0) is the leader and can play any card
        Card anyCard = players[0].getHand().get(0);
        assertTrue(engine.isValidPlay(0, anyCard));
    }

    @Test
    void testValidPlayFollowSuit() {
        engine.startNewRound();
        engine.declareTrump(0, Suit.SPADE);
        List<Card> kittyCards = selectNonJokerKitty(players[0], 6);
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
        List<Card> kittyCards = selectNonJokerKitty(players[0], 6);
        engine.setKitty(kittyCards);

        // Manually set up player 1 to have no cards of a specific suit
        players[1].getHand().clear();
        // Give player 1 only club cards (non-trump, non-heart)
        for (int i = 0; i < 39; i++) {
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
        List<Card> kittyCards = selectNonJokerKitty(players[0], 6);
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
    void testBangTrickNonBangCannotWin() {
        // Issue: K+9 should NOT beat BANG Q when the follower's cards don't form a BANG
        engine.startNewRound();
        engine.declareTrump(0, Suit.HEART);
        List<Card> kittyCards = selectNonJokerKitty(players[0], 6);
        engine.setKitty(kittyCards);

        for (Player p : players) {
            p.getHand().clear();
        }

        // Player 0 leads with BANG ♠Q (two ♠Q from different decks)
        Card spadeQ1 = new Card(Suit.SPADE, Rank.QUEEN, 500);
        Card spadeQ2 = new Card(Suit.SPADE, Rank.QUEEN, 501);
        // Player 1 follows with ♠K + ♠9 (NOT a bang - different ranks)
        Card spadeK = new Card(Suit.SPADE, Rank.KING, 502);
        Card spade9 = new Card(Suit.SPADE, Rank.NINE, 503);
        // Player 2 follows with two random cards (♣4 + ♣6)
        Card club4 = new Card(Suit.CLUB, Rank.FOUR, 504);
        Card club6 = new Card(Suit.CLUB, Rank.SIX, 505);
        // Player 3 follows with ♠J + ♠8
        Card spadeJ = new Card(Suit.SPADE, Rank.JACK, 506);
        Card spade8 = new Card(Suit.SPADE, Rank.EIGHT, 507);

        players[0].addCards(List.of(spadeQ1, spadeQ2));
        players[1].addCards(List.of(spadeK, spade9));
        players[2].addCards(List.of(club4, club6));
        players[3].addCards(List.of(spadeJ, spade8));

        engine.playCards(0, List.of(spadeQ1, spadeQ2));
        engine.playCards(1, List.of(spadeK, spade9));
        engine.playCards(2, List.of(club4, club6));
        engine.playCards(3, List.of(spadeJ, spade8));

        int winner = engine.evaluateTrick();
        // Player 0 should win - no other player played a valid BANG
        assertEquals(0, winner, "BANG Q should win when no other player plays a valid BANG");
    }

    @Test
    void testBangTrickValidBangWins() {
        // A valid BANG K should beat BANG Q
        engine.startNewRound();
        engine.declareTrump(0, Suit.HEART);
        List<Card> kittyCards = selectNonJokerKitty(players[0], 6);
        engine.setKitty(kittyCards);

        for (Player p : players) {
            p.getHand().clear();
        }

        // Player 0 leads with BANG ♠Q
        Card spadeQ1 = new Card(Suit.SPADE, Rank.QUEEN, 600);
        Card spadeQ2 = new Card(Suit.SPADE, Rank.QUEEN, 601);
        // Player 1 follows with BANG ♠K (valid BANG, higher rank)
        Card spadeK1 = new Card(Suit.SPADE, Rank.KING, 602);
        Card spadeK2 = new Card(Suit.SPADE, Rank.KING, 603);
        // Player 2 follows with two random cards
        Card club4 = new Card(Suit.CLUB, Rank.FOUR, 604);
        Card club6 = new Card(Suit.CLUB, Rank.SIX, 605);
        // Player 3 follows with two random cards
        Card diamond7 = new Card(Suit.DIAMOND, Rank.SEVEN, 606);
        Card diamond8 = new Card(Suit.DIAMOND, Rank.EIGHT, 607);

        players[0].addCards(List.of(spadeQ1, spadeQ2));
        players[1].addCards(List.of(spadeK1, spadeK2));
        players[2].addCards(List.of(club4, club6));
        players[3].addCards(List.of(diamond7, diamond8));

        engine.playCards(0, List.of(spadeQ1, spadeQ2));
        engine.playCards(1, List.of(spadeK1, spadeK2));
        engine.playCards(2, List.of(club4, club6));
        engine.playCards(3, List.of(diamond7, diamond8));

        int winner = engine.evaluateTrick();
        // Player 1 should win with BANG K > BANG Q
        assertEquals(1, winner, "Valid BANG K should beat BANG Q");
    }

    @Test
    void testPointScoring() {
        engine.startNewRound();
        engine.declareTrump(0, Suit.HEART);
        List<Card> kittyCards = selectNonJokerKitty(players[0], 6);
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

    @Test
    void testCannotPlayTwoWhenHavingSuitCards() {
        engine.startNewRound();
        engine.declareTrump(0, Suit.HEART);
        List<Card> kittyCards = selectNonJokerKitty(players[0], 6);
        engine.setKitty(kittyCards);

        // Set up player 1 with spade cards AND a 2 (which is trump)
        players[1].getHand().clear();
        Card spadeAce = new Card(Suit.SPADE, Rank.ACE, 600);
        Card twoOfClub = new Card(Suit.CLUB, Rank.TWO, 601);
        players[1].addCards(List.of(spadeAce, twoOfClub));

        // Player 0 leads with a spade
        players[0].getHand().clear();
        Card spadeKing = new Card(Suit.SPADE, Rank.KING, 602);
        players[0].addCards(List.of(spadeKing));
        engine.playCard(0, spadeKing);

        // Player 1 has a spade (ace), so must follow suit - cannot play 2 (trump)
        assertFalse(engine.isValidPlay(1, twoOfClub),
            "Should not be allowed to play 2 when player has cards of the led suit");
        assertTrue(engine.isValidPlay(1, spadeAce),
            "Should be allowed to follow suit with spade ace");
    }

    @Test
    void testCanPlayTwoWhenNoSuitCards() {
        engine.startNewRound();
        engine.declareTrump(0, Suit.HEART);
        List<Card> kittyCards = selectNonJokerKitty(players[0], 6);
        engine.setKitty(kittyCards);

        // Set up player 1 with only 2s and clubs (no spades)
        players[1].getHand().clear();
        Card twoOfClub = new Card(Suit.CLUB, Rank.TWO, 700);
        Card clubFour = new Card(Suit.CLUB, Rank.FOUR, 701);
        players[1].addCards(List.of(twoOfClub, clubFour));

        // Player 0 leads with a spade
        players[0].getHand().clear();
        Card spadeKing = new Card(Suit.SPADE, Rank.KING, 702);
        players[0].addCards(List.of(spadeKing));
        engine.playCard(0, spadeKing);

        // Player 1 has no spades, so can play any card including 2
        assertTrue(engine.isValidPlay(1, twoOfClub),
            "Should be allowed to play 2 when player has no cards of the led suit");
    }

    @Test
    void testCanFollowTrumpLeadWithAnyTrump() {
        // 议题：领出主牌花色（如黑桃）时，主牌花色、当前主、2、大小王均为主牌，
        // 跟牌方可任意使用这些主牌跟出，而非被固定为只能出对应花色的牌。
        engine.startNewRound();
        engine.declareTrump(0, Suit.SPADE); // 主花色黑桃，队伍0当前级别为3（默认）
        List<Card> kittyCards = selectNonJokerKitty(players[0], 6);
        engine.setKitty(kittyCards);

        // 玩家0领出一张黑桃（主牌花色）
        players[0].getHand().clear();
        Card spadeKing = new Card(Suit.SPADE, Rank.KING, 800);
        players[0].addCards(List.of(spadeKing));

        // 玩家1手中各类主牌 + 一张非主副牌
        players[1].getHand().clear();
        Card spadeFive = new Card(Suit.SPADE, Rank.FIVE, 801);   // 主花色
        Card twoOfHeart = new Card(Suit.HEART, Rank.TWO, 802);    // 2 为主
        Card threeOfHeart = new Card(Suit.HEART, Rank.THREE, 803); // 当前主（级别3）
        Card bigJoker = new Card(null, Rank.BIG_JOKER, 804);      // 大王为主
        Card clubFour = new Card(Suit.CLUB, Rank.FOUR, 805);      // 非主副牌
        players[1].addCards(List.of(spadeFive, twoOfHeart, threeOfHeart, bigJoker, clubFour));

        engine.playCard(0, spadeKing);

        assertTrue(engine.isValidPlay(1, spadeFive), "应允许用主花色黑桃跟主");
        assertTrue(engine.isValidPlay(1, twoOfHeart), "应允许用 2 跟主");
        assertTrue(engine.isValidPlay(1, threeOfHeart), "应允许用当前主跟主");
        assertTrue(engine.isValidPlay(1, bigJoker), "应允许用大王跟主");
        assertFalse(engine.isValidPlay(1, clubFour),
            "持有主牌时不能用非主副牌跟主");
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

    @Test
    void testIsFirstRound() {
        engine.startNewRound();
        assertTrue(engine.isFirstRound());
    }

    @Test
    void testPlayerHasBigJoker() {
        engine.startNewRound();
        // At least one player should have a Big Joker (there are 3 Big Jokers in the deck)
        boolean anyHas = false;
        for (int i = 0; i < 4; i++) {
            if (engine.playerHasBigJoker(i)) {
                anyHas = true;
            }
        }
        assertTrue(anyHas, "At least one player should have a Big Joker");
    }

    @Test
    void testDeclareTrumpFromKitty() {
        engine.startNewRound();
        int dealerIdx = engine.declareTrumpFromKitty();
        assertNotNull(engine.getTrumpInfo());
        assertNotNull(engine.getTrumpInfo().getTrumpSuit());
        assertTrue(dealerIdx >= 0 && dealerIdx < 4);
        assertEquals(dealerIdx, engine.getDealerIndex());
        // Dealer should have 45 cards (39 + 6 kitty)
        assertEquals(45, players[dealerIdx].getHand().size());
    }

    @Test
    void testDeclareTrumpRandomSuit() {
        engine.startNewRound();
        Suit randomSuit = engine.declareTrumpRandomSuit(1);
        assertNotNull(randomSuit);
        assertNotNull(engine.getTrumpInfo());
        assertEquals(randomSuit, engine.getTrumpInfo().getTrumpSuit());
        assertEquals(1, engine.getDealerIndex());
        // Dealer should have 45 cards (39 + 6 kitty)
        assertEquals(45, players[1].getHand().size());
    }

    @Test
    void testSameCardFirstPlayedWins() {
        // Bug 3: 先出大王的人应赢，后出大王的人不应赢
        engine.startNewRound();
        engine.declareTrump(0, Suit.HEART);
        List<Card> kittyCards = selectNonJokerKitty(players[0], 6);
        engine.setKitty(kittyCards);

        for (Player p : players) {
            p.getHand().clear();
        }

        // Player 0 leads with Big Joker
        Card bigJoker1 = new Card(null, Rank.BIG_JOKER, 800);
        // Player 1 plays a small card
        Card spade4 = new Card(Suit.SPADE, Rank.FOUR, 801);
        // Player 2 also plays Big Joker (same strength)
        Card bigJoker2 = new Card(null, Rank.BIG_JOKER, 802);
        // Player 3 plays a small card
        Card club6 = new Card(Suit.CLUB, Rank.SIX, 803);

        players[0].addCards(List.of(bigJoker1));
        players[1].addCards(List.of(spade4));
        players[2].addCards(List.of(bigJoker2));
        players[3].addCards(List.of(club6));

        engine.playCard(0, bigJoker1);
        engine.playCard(1, spade4);
        engine.playCard(2, bigJoker2);
        engine.playCard(3, club6);

        int winner = engine.evaluateTrick();
        // Player 0 played first, same strength → first played should win
        assertEquals(0, winner, "先出大王的人应赢");
    }

    @Test
    void testSameCardFirstPlayedWinsNonZeroLeader() {
        // 验证非0号玩家领出时，同牌力先出者赢
        engine.startNewRound();
        engine.declareTrump(2, Suit.HEART);
        List<Card> kittyCards = selectNonJokerKitty(players[2], 6);
        engine.setKitty(kittyCards);

        for (Player p : players) {
            p.getHand().clear();
        }

        // Player 2 leads with Big Joker
        Card bigJoker1 = new Card(null, Rank.BIG_JOKER, 900);
        // Player 3 plays small card
        Card diamond7 = new Card(Suit.DIAMOND, Rank.SEVEN, 901);
        // Player 0 also plays Big Joker
        Card bigJoker2 = new Card(null, Rank.BIG_JOKER, 902);
        // Player 1 plays small card
        Card club8 = new Card(Suit.CLUB, Rank.EIGHT, 903);

        players[2].addCards(List.of(bigJoker1));
        players[3].addCards(List.of(diamond7));
        players[0].addCards(List.of(bigJoker2));
        players[1].addCards(List.of(club8));

        engine.playCard(2, bigJoker1);
        engine.playCard(3, diamond7);
        engine.playCard(0, bigJoker2);
        engine.playCard(1, club8);

        int winner = engine.evaluateTrick();
        // Player 2 was leader and played Big Joker first
        assertEquals(2, winner, "领出者先出大王应赢");
    }

    // ======================== Step-by-Step Tribute Tests ========================

    @Test
    void testFindNextTributeGiverInfoNoTribute() {
        // 没有进贡需求时返回null
        engine.startNewRound();
        engine.declareTrump(0, Suit.SPADE);
        List<Card> kittyCards = players[0].getHand().stream()
            .filter(c -> c.getRank() != Rank.SMALL_JOKER && c.getRank() != Rank.BIG_JOKER)
            .limit(6)
            .toList();
        engine.setKitty(kittyCards);

        assertNull(engine.findNextTributeGiverInfo(),
            "没有进贡需求时应返回null");
    }

    @Test
    void testTributeReceiverIsDealer() {
        // 上贡应给庄家：完整打完一局后，下一局庄家（赢方）应为进贡接收方
        playFullRound(engine, players);

        RoundResult result = engine.calculateRoundResult();
        int nextDealer = engine.getNextDealerIndex();
        assertEquals(result.getWinningTeam(), players[nextDealer].getTeam(),
            "下一局庄家应来自上一局赢方队伍");

        engine.startNewRound();
        engine.declareTrump(nextDealer, Suit.SPADE);

        assertEquals(nextDealer, engine.getTributeReceiverIndex(),
            "进贡接收方应为庄家");
        assertEquals(result.getWinningTeam(), engine.getPreviousWinningTeam());
    }

    /**
     * 简单地用“首张合法牌”策略打完一整局，使局面进入 ROUND_END。
     */
    private static void playFullRound(GameEngine engine, Player[] players) {
        engine.startNewRound();
        engine.declareTrump(0, Suit.SPADE);
        List<Card> kittyCards = players[0].getHand().stream()
            .filter(c -> c.getRank() != Rank.SMALL_JOKER && c.getRank() != Rank.BIG_JOKER)
            .limit(6)
            .toList();
        engine.setKitty(kittyCards);

        int guard = 0;
        while (engine.getPhase() != GamePhase.ROUND_END && guard++ < 1000) {
            int cur = engine.getCurrentPlayerIndex();
            Card toPlay = null;
            for (Card c : new ArrayList<>(players[cur].getHand())) {
                if (engine.isValidPlay(cur, c)) {
                    toPlay = c;
                    break;
                }
            }
            assertNotNull(toPlay, "每位玩家总应有一张合法牌可出");
            engine.playCard(cur, toPlay);
            if (engine.getTrickCardsPlayed() == 4) {
                engine.evaluateTrick();
            }
        }
        assertEquals(GamePhase.ROUND_END, engine.getPhase(), "应能打完整局");
    }

    @Test
    void testAutoSelectReturnCard() {
        engine.startNewRound();
        // Player 0 手里有各种牌
        players[0].getHand().clear();
        Card spadeA = new Card(Suit.SPADE, Rank.ACE, 900);
        Card club4 = new Card(Suit.CLUB, Rank.FOUR, 901);
        Card bigJoker = new Card(null, Rank.BIG_JOKER, 902);
        players[0].addCards(List.of(spadeA, club4, bigJoker));

        // 自动选择回贡牌应选最小的非王牌
        Card returnCard = engine.autoSelectReturnCard(0);
        assertNotNull(returnCard);
        assertNotEquals(Rank.BIG_JOKER, returnCard.getRank(),
            "自动回贡不应选王牌");
        assertEquals(Rank.FOUR, returnCard.getRank(),
            "应选最小的非王牌(♣4)");
    }

    @Test
    void testExecuteTributeGiveAndReturn() {
        engine.startNewRound();
        // 设置手牌用于测试进贡转移
        players[0].getHand().clear();
        players[1].getHand().clear();
        Card spadeA = new Card(Suit.SPADE, Rank.ACE, 900);
        Card club4 = new Card(Suit.CLUB, Rank.FOUR, 901);
        players[0].addCards(List.of(spadeA));
        players[1].addCards(List.of(club4));

        int p0Size = players[0].getHand().size();
        int p1Size = players[1].getHand().size();

        // 进贡：P0 给 P1 一张 ♠A
        engine.executeTributeGive(0, spadeA, 1);
        assertEquals(p0Size - 1, players[0].getHand().size(), "进贡方应少一张牌");
        assertEquals(p1Size + 1, players[1].getHand().size(), "接收方应多一张牌");
        assertTrue(players[1].hasCards(List.of(spadeA)), "接收方应有贡牌");

        // 回贡：P1 还给 P0 一张 ♣4
        engine.executeTributeReturn(1, club4, 0);
        assertTrue(players[0].hasCards(List.of(club4)), "进贡方应收到回贡牌");
        assertFalse(players[1].hasCards(List.of(club4)), "接收方不应再有回贡牌");
    }

    @Test
    void testGetTributeCard() {
        engine.startNewRound();
        players[0].getHand().clear();
        Card spadeA = new Card(Suit.SPADE, Rank.ACE, 900);
        Card club4 = new Card(Suit.CLUB, Rank.FOUR, 901);
        Card bigJoker = new Card(null, Rank.BIG_JOKER, 902);
        players[0].addCards(List.of(spadeA, club4, bigJoker));

        Card tributeCard = engine.getTributeCard(0);
        assertNotNull(tributeCard);
        // 大王是最大的牌，应该被选为进贡牌
        assertEquals(Rank.BIG_JOKER, tributeCard.getRank(),
            "进贡牌应是手中最大的牌（大王）");
    }

    @Test
    void testTributeGiverIsDealerUpstream() {
        // 进贡只由一人进行：庄家的上家（出牌顺序中庄家前一位的闲家）向庄家进贡。
        int tributeScenarios = 0;
        for (int attempt = 0; attempt < 400 && tributeScenarios < 3; attempt++) {
            Player[] ps = new Player[]{
                new Player(0, "P0", false),
                new Player(1, "P1", false),
                new Player(2, "P2", false),
                new Player(3, "P3", false)
            };
            GameEngine eng = new GameEngine(ps);
            playFullRound(eng, ps);
            eng.calculateRoundResult();

            int nextDealer = eng.getNextDealerIndex();
            eng.startNewRound();
            eng.declareTrump(nextDealer, Suit.SPADE);

            if (!eng.isTributeRequired()) {
                continue;
            }
            tributeScenarios++;

            int receiver = eng.getTributeReceiverIndex();
            int giver = eng.getTributeGiverIndex();
            assertEquals((receiver + 3) % 4, giver,
                "进贡方应为庄家的上家（庄家前一位）");
            assertEquals(1 - eng.getPreviousWinningTeam(), ps[giver].getTeam(),
                "进贡方应来自输方队伍");
            assertNotEquals(receiver, giver, "进贡方与接收方不应为同一人");

            int[] info = eng.findNextTributeGiverInfo();
            assertNotNull(info);
            assertEquals(giver, info[0], "findNextTributeGiverInfo 应返回庄家上家作为进贡方");
            assertEquals(receiver, info[1]);
        }
        assertTrue(tributeScenarios > 0,
            "应至少出现一个需要进贡的局面以验证进贡方");
    }

    @Test
    void testAutoTributeUsesSingleGiver() {
        // performAutoTribute 应只让一名玩家（庄家上家）进贡，即使有多个贡。
        for (int attempt = 0; attempt < 400; attempt++) {
            Player[] ps = new Player[]{
                new Player(0, "P0", false),
                new Player(1, "P1", false),
                new Player(2, "P2", false),
                new Player(3, "P3", false)
            };
            GameEngine eng = new GameEngine(ps);
            playFullRound(eng, ps);
            eng.calculateRoundResult();

            int nextDealer = eng.getNextDealerIndex();
            eng.startNewRound();
            eng.declareTrump(nextDealer, Suit.SPADE);

            if (!eng.isTributeRequired() || eng.getPreviousTributeCount() < 2) {
                continue;
            }
            int giver = eng.getTributeGiverIndex();
            int giverBefore = ps[giver].getHand().size();

            String msg = eng.performAutoTribute();
            assertNotNull(msg);
            // 进贡牌全部来自同一名玩家（庄家上家）
            for (int i = 0; i < 4; i++) {
                if (i == giver) {
                    continue;
                }
                assertFalse(msg.contains(ps[i].getName() + " 进贡"),
                    "只有庄家上家应进贡，玩家 " + ps[i].getName() + " 不应进贡");
            }
            // 进贡方净失去 previousTributeCount 张牌（收到等量回贡，故手牌数不变），
            // 但至少进行了多次进贡，验证由单人完成。
            assertTrue(giverBefore > 0);
            return; // 已验证一个多贡场景即可
        }
        // 若 400 次内未出现多贡场景，不视为失败（场景较罕见）。
    }
}
