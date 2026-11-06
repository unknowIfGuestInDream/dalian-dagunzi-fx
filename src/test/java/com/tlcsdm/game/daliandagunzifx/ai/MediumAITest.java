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
}
