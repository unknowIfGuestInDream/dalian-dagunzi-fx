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
import com.tlcsdm.game.daliandagunzifx.model.Rank;
import com.tlcsdm.game.daliandagunzifx.model.Suit;
import com.tlcsdm.game.daliandagunzifx.tracker.CardTracker;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MediumAITest {

    private static Player[] newPlayers() {
        return new Player[]{
            new Player(0, "P0", false),
            new Player(1, "P1", false),
            new Player(2, "P2", false),
            new Player(3, "P3", false)
        };
    }

    private static GameEngine newEngineWithTrump(Player[] players, Suit trumpSuit) {
        GameEngine engine = new GameEngine(players);
        engine.startNewRound();
        engine.declareTrump(0, trumpSuit);
        List<Card> kittyCards = players[0].getHand().stream()
            .filter(c -> c.getRank() != Rank.SMALL_JOKER && c.getRank() != Rank.BIG_JOKER)
            .limit(6)
            .toList();
        engine.setKitty(kittyCards);
        return engine;
    }

    /**
     * 标记记牌器中某花色指定点数的全部副牌为已打出。
     */
    private static void markRankPlayed(CardTracker tracker, Suit suit, Rank rank) {
        for (Card c : tracker.getSuitRemainingCards(suit)) {
            if (c.getRank() == rank) {
                tracker.cardPlayed(c, 1);
            }
        }
    }

    @Test
    void testCashesBossWhenLeading() {
        Player[] players = newPlayers();
        // 主：红桃，主牌级默认为 3，故黑桃均为副牌（黑桃 2、黑桃 3 除外，属主牌）
        GameEngine engine = newEngineWithTrump(players, Suit.HEART);

        CardTracker tracker = new CardTracker();
        // 3 张黑桃 A 全部打出后，黑桃 K 成为场上最大的黑桃副牌（绝对赢张）
        markRankPlayed(tracker, Suit.SPADE, Rank.ACE);

        Player leader = players[engine.getCurrentPlayerIndex()];
        leader.getHand().clear();
        Card spadeK = new Card(Suit.SPADE, Rank.KING, 900);
        Card spade9 = new Card(Suit.SPADE, Rank.NINE, 901);
        Card spade7 = new Card(Suit.SPADE, Rank.SEVEN, 902);
        Card club6 = new Card(Suit.CLUB, Rank.SIX, 903);
        leader.addCards(List.of(spadeK, spade9, spade7, club6));

        MediumAI ai = new MediumAI(tracker);
        Card chosen = ai.chooseCard(leader, engine);

        assertNotNull(chosen);
        assertEquals(Rank.KING, chosen.getRank(),
            "黑桃 A 已出尽时，AI 应主动兑现黑桃 K 这张绝对赢张");
        assertEquals(Suit.SPADE, chosen.getSuit());
    }

    @Test
    void testDoesNotCashBossWhenOpponentKnownVoid() {
        Player[] players = newPlayers();
        GameEngine engine = newEngineWithTrump(players, Suit.HEART);

        CardTracker tracker = new CardTracker();
        markRankPlayed(tracker, Suit.SPADE, Rank.ACE);

        Player leader = players[engine.getCurrentPlayerIndex()];
        // 下家已知缺黑桃，可能用主牌毙，兑现黑桃 K 不安全
        int opp1 = (leader.getId() + 1) % 4;
        tracker.markVoidSuit(opp1, Suit.SPADE);

        leader.getHand().clear();
        Card spadeK = new Card(Suit.SPADE, Rank.KING, 900);
        Card spade9 = new Card(Suit.SPADE, Rank.NINE, 901);
        Card spade7 = new Card(Suit.SPADE, Rank.SEVEN, 902);
        Card club6 = new Card(Suit.CLUB, Rank.SIX, 903);
        leader.addCards(List.of(spadeK, spade9, spade7, club6));

        MediumAI ai = new MediumAI(tracker);
        Card chosen = ai.chooseCard(leader, engine);

        assertNotNull(chosen);
        assertNotEquals(Rank.KING, chosen.getRank(),
            "对手已知缺黑桃时，不应主动兑现黑桃 K（可能被主牌毙），应改出非分牌");
    }

    @Test
    void testNoBossFallsBackToNormalLead() {
        Player[] players = newPlayers();
        GameEngine engine = newEngineWithTrump(players, Suit.HEART);

        // 记牌器全新，黑桃 A 仍在场上，黑桃 K 不是绝对赢张
        CardTracker tracker = new CardTracker();

        Player leader = players[engine.getCurrentPlayerIndex()];
        leader.getHand().clear();
        Card spadeK = new Card(Suit.SPADE, Rank.KING, 900);
        Card spade9 = new Card(Suit.SPADE, Rank.NINE, 901);
        Card spade7 = new Card(Suit.SPADE, Rank.SEVEN, 902);
        leader.addCards(List.of(spadeK, spade9, spade7));

        MediumAI ai = new MediumAI(tracker);
        Card chosen = ai.chooseCard(leader, engine);

        assertNotNull(chosen);
        assertTrue(leader.getHand().contains(chosen));
        assertEquals(0, chosen.getPoints(),
            "无绝对赢张时应沿用原策略，领出非分牌而非送出 K 的分值");
    }

    @Test
    void testHardAiCashesBossWhenLeading() {
        Player[] players = newPlayers();
        GameEngine engine = newEngineWithTrump(players, Suit.HEART);

        CardTracker tracker = new CardTracker();
        markRankPlayed(tracker, Suit.SPADE, Rank.ACE);

        Player leader = players[engine.getCurrentPlayerIndex()];
        leader.getHand().clear();
        Card spadeK = new Card(Suit.SPADE, Rank.KING, 900);
        Card spade9 = new Card(Suit.SPADE, Rank.NINE, 901);
        Card spade7 = new Card(Suit.SPADE, Rank.SEVEN, 902);
        Card club6 = new Card(Suit.CLUB, Rank.SIX, 903);
        leader.addCards(List.of(spadeK, spade9, spade7, club6));

        HardAI ai = new HardAI(tracker);
        Card chosen = ai.chooseCard(leader, engine);

        assertNotNull(chosen);
        assertEquals(Rank.KING, chosen.getRank(),
            "HardAI 领出时也应兑现绝对赢张黑桃 K");
        assertEquals(Suit.SPADE, chosen.getSuit());
    }

    @Test
    void testFollowGrabsTrumpPointsAsLastDefender() {
        // item 1：MediumAI 作为本队最后一道防线、墩面≥10 分且对手主牌花色赢着时，
        // 应动用常主 ♥2 管上夺分。
        Player[] players = newPlayers();
        GameEngine engine = newEngineWithTrump(players, Suit.HEART);

        // Player 0（对手）领出 ♥K（10 分）
        players[0].getHand().clear();
        Card heartK = new Card(Suit.HEART, Rank.KING, 913);
        players[0].addCards(List.of(heartK));
        engine.playCard(0, heartK);

        // Player 1（AI 队友）跟 ♥5（5 分，压不过 K）
        players[1].getHand().clear();
        Card heart5 = new Card(Suit.HEART, Rank.FIVE, 905);
        players[1].addCards(List.of(heart5));
        engine.playCard(1, heart5);

        // Player 2（对手）跟 ♥Q（压不过 K）
        players[2].getHand().clear();
        Card heartQ = new Card(Suit.HEART, Rank.QUEEN, 912);
        players[2].addCards(List.of(heartQ));
        engine.playCard(2, heartQ);

        // Player 3（AI，最后出牌）：♥2(常主)、♥6、♥9
        players[3].getHand().clear();
        Card heart2 = new Card(Suit.HEART, Rank.TWO, 921);
        Card heart6 = new Card(Suit.HEART, Rank.SIX, 922);
        Card heart9 = new Card(Suit.HEART, Rank.NINE, 923);
        players[3].addCards(List.of(heart2, heart6, heart9));

        MediumAI ai = new MediumAI(new CardTracker());
        Card chosen = ai.chooseCard(players[3], engine);

        assertEquals(Rank.TWO, chosen.getRank(),
            "墩面有 15 分且对手主牌花色赢着、MediumAI 为最后防线时，应出常主 ♥2 夺分");
        assertEquals(Suit.HEART, chosen.getSuit());
    }

    @Test
    void testLeadFromTrumpOnlyKeepsBangIntact() {
        // 只剩主牌被迫领出单张时，MediumAI 不应拆散最小的主牌棒子，且不送分牌
        Player[] players = newPlayers();
        GameEngine engine = newEngineWithTrump(players, Suit.HEART);

        Player leader = players[engine.getCurrentPlayerIndex()];
        leader.getHand().clear();
        // 全主牌手（主为红桃）：最小的 ♥4 为棒子，其余更大的单张；12 张 >10 非残局，退化单张领出
        leader.addCards(List.of(
            new Card(Suit.HEART, Rank.FOUR, 800),
            new Card(Suit.HEART, Rank.FOUR, 801),
            new Card(Suit.HEART, Rank.FIVE, 802),
            new Card(Suit.HEART, Rank.SIX, 803),
            new Card(Suit.HEART, Rank.SEVEN, 804),
            new Card(Suit.HEART, Rank.EIGHT, 805),
            new Card(Suit.HEART, Rank.NINE, 806),
            new Card(Suit.HEART, Rank.TEN, 807),
            new Card(Suit.HEART, Rank.JACK, 808),
            new Card(Suit.HEART, Rank.QUEEN, 809),
            new Card(Suit.HEART, Rank.KING, 810),
            new Card(Suit.HEART, Rank.ACE, 811)));

        MediumAI ai = new MediumAI(new CardTracker());
        List<Card> chosen = ai.chooseCards(leader, engine);

        assertEquals(1, chosen.size(), "非残局全主牌时退化为单张领出");
        Card lead = chosen.get(0);
        assertNotEquals(Rank.FOUR, lead.getRank(),
            "不应拆散最小的 ♥4 棒子，实际领出: " + lead.getRank());
        assertEquals(0, lead.getPoints(),
            "领出的主牌应为非分牌，避免主动送分，实际领出: " + lead.getRank());
        long remainingFours = leader.getHand().stream().filter(c -> c.getRank() == Rank.FOUR).count();
        assertEquals(2, remainingFours, "♥4 棒子应完整保留在手中");
    }

    @Test
    void testVoidRuffsAsLastDefenderMidValue() {
        // item 3：队友领出副牌被对手用小主毙掉、AI(对门)无该花色、墩面有分且 AI 为最后防线时，
        // 应用主牌管上夺分，而不是跑别的花色（修复“对门能管上却跑别的牌”）。
        Player[] players = newPlayers();
        GameEngine engine = newEngineWithTrump(players, Suit.HEART);

        // Player 0（AI 队友）领出 ♠5（副牌，5 分）
        players[0].getHand().clear();
        Card spade5 = new Card(Suit.SPADE, Rank.FIVE, 530);
        players[0].addCards(List.of(spade5));
        engine.playCard(0, spade5);

        // Player 1（对手）用 ♥4（小主）毙掉 → 对手赢着本墩
        players[1].getHand().clear();
        Card heart4 = new Card(Suit.HEART, Rank.FOUR, 531);
        players[1].addCards(List.of(heart4));
        engine.playCard(1, heart4);

        // Player 2（AI，第 3 位出牌，队友已出且未赢）：无黑桃，持 ♥6（可压过 ♥4）与 ♣7（无分副牌）
        players[2].getHand().clear();
        Card heart6 = new Card(Suit.HEART, Rank.SIX, 532);
        Card club7 = new Card(Suit.CLUB, Rank.SEVEN, 533);
        players[2].addCards(List.of(heart6, club7));

        MediumAI ai = new MediumAI(new CardTracker());
        Card chosen = ai.chooseCard(players[2], engine);

        assertEquals(Suit.HEART, chosen.getSuit(),
            "对手小主毙着 5 分、AI 为最后防线时，应用 ♥6 管上夺分，而非垫 ♣7");
        assertEquals(Rank.SIX, chosen.getRank());
    }

    @Test
    void testFollowKeepsPairIntactWhenDiscarding() {
        // item 4：跟副牌出小牌时不应拆散棒子(对子)。
        // 手握 ♣4♣4 棒子 + ♣8 单张，领出 ♣9（0 分、压不过）时应垫 ♣8 保留棒子。
        Player[] players = newPlayers();
        GameEngine engine = newEngineWithTrump(players, Suit.HEART);

        players[0].getHand().clear();
        Card club9 = new Card(Suit.CLUB, Rank.NINE, 700);
        players[0].addCards(List.of(club9));
        engine.playCard(0, club9);

        players[1].getHand().clear();
        Card club4a = new Card(Suit.CLUB, Rank.FOUR, 701);
        Card club4b = new Card(Suit.CLUB, Rank.FOUR, 702);
        Card club8 = new Card(Suit.CLUB, Rank.EIGHT, 703);
        players[1].addCards(List.of(club4a, club4b, club8));

        MediumAI ai = new MediumAI(new CardTracker());
        Card chosen = ai.chooseCard(players[1], engine);

        assertEquals(Rank.EIGHT, chosen.getRank(),
            "应垫单张 ♣8 保留 ♣4 棒子，实际出了: " + chosen.getRank());
        long remainingFours = players[1].getHand().stream()
            .filter(c -> c.getRank() == Rank.FOUR).count();
        assertEquals(2, remainingFours, "♣4 棒子应完整保留在手中");
    }

    @Test
    void testVoidDiscardsInsteadOfRuffingZeroPointTrickAsLast() {
        // item 5：缺门且为最后一位出牌、但墩面 0 分、又有无分副牌可垫时，
        // 不应强行用主牌毙（白白浪费主牌），应安全垫掉副牌。
        Player[] players = newPlayers();
        GameEngine engine = newEngineWithTrump(players, Suit.HEART);

        players[0].getHand().clear();
        Card spade6 = new Card(Suit.SPADE, Rank.SIX, 600);
        players[0].addCards(List.of(spade6));
        engine.playCard(0, spade6);

        players[1].getHand().clear();
        Card spade7 = new Card(Suit.SPADE, Rank.SEVEN, 601);
        players[1].addCards(List.of(spade7));
        engine.playCard(1, spade7);

        players[2].getHand().clear();
        Card spade8 = new Card(Suit.SPADE, Rank.EIGHT, 602);
        players[2].addCards(List.of(spade8));
        engine.playCard(2, spade8);

        // Player 3（AI，最后出牌）：无黑桃，持 ♥4（小主）与 ♣7（无分副牌，应安全垫）
        players[3].getHand().clear();
        Card heart4 = new Card(Suit.HEART, Rank.FOUR, 603);
        Card club7 = new Card(Suit.CLUB, Rank.SEVEN, 604);
        players[3].addCards(List.of(heart4, club7));

        MediumAI ai = new MediumAI(new CardTracker());
        Card chosen = ai.chooseCard(players[3], engine);

        assertFalse(engine.getTrumpInfo().isTrump(chosen),
            "0 分墩且有无分副牌可垫时，不应浪费主牌毙，应垫 ♣7，实际出了: "
                + chosen.getRank() + " " + chosen.getSuit());
        assertEquals(Rank.SEVEN, chosen.getRank());
    }

    @Test
    void testVoidForcedRuffPicksMinNonSpecialTrump() {
        // item 5：打 3（级牌=3）时，缺门且只剩主牌被迫毙，应出最小的非分非特殊主牌 ♥4，
        // 不应浪费级牌 ♥3（打 3 时 ♥3 比常主 ♥2 还大）或送出分牌 ♥5。
        Player[] players = newPlayers();
        GameEngine engine = newEngineWithTrump(players, Suit.HEART);

        players[0].getHand().clear();
        Card spade6 = new Card(Suit.SPADE, Rank.SIX, 600);
        players[0].addCards(List.of(spade6));
        engine.playCard(0, spade6);

        players[1].getHand().clear();
        Card spade7 = new Card(Suit.SPADE, Rank.SEVEN, 601);
        players[1].addCards(List.of(spade7));
        engine.playCard(1, spade7);

        players[2].getHand().clear();
        Card spade8 = new Card(Suit.SPADE, Rank.EIGHT, 602);
        players[2].addCards(List.of(spade8));
        engine.playCard(2, spade8);

        // Player 3（AI）：无黑桃，无副牌可垫，持 ♥3(级牌)、♥4、♥5(分牌)
        players[3].getHand().clear();
        Card heart3 = new Card(Suit.HEART, Rank.THREE, 603);
        Card heart4 = new Card(Suit.HEART, Rank.FOUR, 604);
        Card heart5 = new Card(Suit.HEART, Rank.FIVE, 605);
        players[3].addCards(List.of(heart3, heart4, heart5));

        MediumAI ai = new MediumAI(new CardTracker());
        Card chosen = ai.chooseCard(players[3], engine);

        assertEquals(Rank.FOUR, chosen.getRank(),
            "被迫毙时应出最小的非分非特殊主牌 ♥4，不应出级牌 ♥3 或分牌 ♥5，实际: "
                + chosen.getRank());
    }

    @Test
    void testThirdHandCoversBossToPreventOpponentScoring() {
        // item 3：队友(第1家)领出小副牌、对手(第2家)小副牌赢着、AI 为第3家（非最后一位），
        // 手握该花色绝对赢张(♠A) 且后续对手(第4家)未缺门时，应盖 ♠A 锁定本墩，
        // 避免出小牌后被第4家用 10/K 等大牌关门得分。
        Player[] players = newPlayers();
        GameEngine engine = newEngineWithTrump(players, Suit.HEART);
        CardTracker tracker = new CardTracker();

        // Player 0（AI 队友）领出 ♠4（0 分）
        players[0].getHand().clear();
        Card spade4 = new Card(Suit.SPADE, Rank.FOUR, 800);
        players[0].addCards(List.of(spade4));
        engine.playCard(0, spade4);

        // Player 1（对手）跟 ♠9（0 分，赢着本墩）
        players[1].getHand().clear();
        Card spade9 = new Card(Suit.SPADE, Rank.NINE, 801);
        players[1].addCards(List.of(spade9));
        engine.playCard(1, spade9);

        // Player 2（AI，第3家）：持 ♠A（绝对赢张）+ ♠6（小牌）
        players[2].getHand().clear();
        Card spadeA = new Card(Suit.SPADE, Rank.ACE, 802);
        Card spade6 = new Card(Suit.SPADE, Rank.SIX, 803);
        players[2].addCards(List.of(spadeA, spade6));

        MediumAI ai = new MediumAI(tracker);
        Card chosen = ai.chooseCard(players[2], engine);

        assertEquals(Rank.ACE, chosen.getRank(),
            "第3家应盖 ♠A 锁定本墩防第4家关门得分，实际出了: " + chosen.getRank());
        assertEquals(Suit.SPADE, chosen.getSuit());
    }

    @Test
    void testThirdHandDoesNotCoverWhenSubsequentOpponentVoid() {
        // item 3 对照：第4家对手已知缺该花色（会用主牌毙），此时盖 ♠A 也会被毙掉、徒劳，
        // 故不盖牌，改出小牌。
        Player[] players = newPlayers();
        GameEngine engine = newEngineWithTrump(players, Suit.HEART);
        CardTracker tracker = new CardTracker();

        players[0].getHand().clear();
        Card spade4 = new Card(Suit.SPADE, Rank.FOUR, 800);
        players[0].addCards(List.of(spade4));
        engine.playCard(0, spade4);

        players[1].getHand().clear();
        Card spade9 = new Card(Suit.SPADE, Rank.NINE, 801);
        players[1].addCards(List.of(spade9));
        engine.playCard(1, spade9);

        // 标记第4家（Player 3）缺黑桃
        tracker.markVoidSuit(3, Suit.SPADE);

        players[2].getHand().clear();
        Card spadeA = new Card(Suit.SPADE, Rank.ACE, 802);
        Card spade6 = new Card(Suit.SPADE, Rank.SIX, 803);
        players[2].addCards(List.of(spadeA, spade6));

        MediumAI ai = new MediumAI(tracker);
        Card chosen = ai.chooseCard(players[2], engine);

        assertEquals(Rank.SIX, chosen.getRank(),
            "第4家缺黑桃会用主牌毙，盖 ♠A 徒劳，应出小牌 ♠6，实际出了: " + chosen.getRank());
    }
}
