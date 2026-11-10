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
package com.tlcsdm.game.daliandagunzifx.tracker;

import com.tlcsdm.game.daliandagunzifx.model.Card;
import com.tlcsdm.game.daliandagunzifx.model.Rank;
import com.tlcsdm.game.daliandagunzifx.model.Suit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CardTrackerTest {

    @Test
    void testInitialState() {
        CardTracker tracker = new CardTracker();
        assertEquals(162, tracker.getRemainingCardCount());
        assertEquals(0, tracker.getPlayedCards().size());
    }

    @Test
    void testCardPlayed() {
        CardTracker tracker = new CardTracker();
        Card card = new Card(Suit.HEART, Rank.ACE, 0);
        tracker.cardPlayed(card, 0);
        assertEquals(161, tracker.getRemainingCardCount());
    }

    @Test
    void testVoidSuit() {
        CardTracker tracker = new CardTracker();
        assertFalse(tracker.isVoid(1, Suit.DIAMOND));
        tracker.markVoidSuit(1, Suit.DIAMOND);
        assertTrue(tracker.isVoid(1, Suit.DIAMOND));
    }

    @Test
    void testReset() {
        CardTracker tracker = new CardTracker();
        Card card = new Card(Suit.HEART, Rank.ACE, 0);
        tracker.cardPlayed(card, 0);
        tracker.markVoidSuit(1, Suit.DIAMOND);

        tracker.reset();
        assertEquals(162, tracker.getRemainingCardCount());
        assertEquals(0, tracker.getPlayedCards().size());
        assertFalse(tracker.isVoid(1, Suit.DIAMOND));
    }

    /** 将记牌器中指定花色+点数的全部副牌标记为已打出，使用真实卡牌保证 id 匹配。 */
    private static void playAllOfSuit(CardTracker tracker, Suit suit, Rank rank) {
        for (Card c : tracker.getSuitRemainingCards(suit)) {
            if (c.getRank() == rank) {
                tracker.cardPlayed(c, 0);
            }
        }
    }

    @Test
    void testGetRemainingCountBySuit() {
        CardTracker tracker = new CardTracker();
        // 3 副牌：每花色 13 点 × 3 副 = 39 张
        assertEquals(39, tracker.getRemainingCount(Suit.SPADE));
        playAllOfSuit(tracker, Suit.SPADE, Rank.ACE);
        assertEquals(36, tracker.getRemainingCount(Suit.SPADE), "出掉 3 张黑桃A后剩 36 张黑桃");
        // 其它花色不受影响
        assertEquals(39, tracker.getRemainingCount(Suit.HEART));
    }

    @Test
    void testGetRemainingCountByRank() {
        CardTracker tracker = new CardTracker();
        // 每点数 4 花色 × 3 副 = 12 张；王各 3 张
        assertEquals(12, tracker.getRemainingCount(Rank.ACE));
        assertEquals(3, tracker.getRemainingCount(Rank.BIG_JOKER));
        playAllOfSuit(tracker, Suit.HEART, Rank.ACE);
        assertEquals(9, tracker.getRemainingCount(Rank.ACE), "出掉 3 张红桃A后剩 9 张A");
    }

    @Test
    void testGetSuitAndRankRemainingCards() {
        CardTracker tracker = new CardTracker();
        assertEquals(39, tracker.getSuitRemainingCards(Suit.CLUB).size());
        assertEquals(12, tracker.getRankRemainingCards(Rank.KING).size());
        playAllOfSuit(tracker, Suit.CLUB, Rank.KING);
        assertEquals(36, tracker.getSuitRemainingCards(Suit.CLUB).size());
        assertTrue(tracker.getSuitRemainingCards(Suit.CLUB).stream()
            .noneMatch(c -> c.getRank() == Rank.KING), "梅花K应已全部出掉");
        assertEquals(9, tracker.getRankRemainingCards(Rank.KING).size());
    }

    @Test
    void testGetPlayedCardsBySuitAndByRank() {
        CardTracker tracker = new CardTracker();
        Card heartAce = tracker.getSuitRemainingCards(Suit.HEART).stream()
            .filter(c -> c.getRank() == Rank.ACE).findFirst().orElseThrow();
        Card spadeKing = tracker.getSuitRemainingCards(Suit.SPADE).stream()
            .filter(c -> c.getRank() == Rank.KING).findFirst().orElseThrow();
        Card bigJoker = tracker.getRankRemainingCards(Rank.BIG_JOKER).get(0);
        tracker.cardPlayed(heartAce, 0);
        tracker.cardPlayed(spadeKing, 1);
        tracker.cardPlayed(bigJoker, 2);

        assertTrue(tracker.getPlayedCardsBySuit().get(Suit.HEART).contains(heartAce));
        assertTrue(tracker.getPlayedCardsBySuit().get(Suit.SPADE).contains(spadeKing));
        // 王无花色，不应出现在按花色分组中
        assertFalse(tracker.getPlayedCardsBySuit().get(Suit.HEART).contains(bigJoker));
        assertTrue(tracker.getPlayedCardsByRank().get(Rank.ACE).contains(heartAce));
        assertTrue(tracker.getPlayedCardsByRank().get(Rank.BIG_JOKER).contains(bigJoker));
    }

    @Test
    void testIsVoidUnknownPlayerReturnsFalse() {
        CardTracker tracker = new CardTracker();
        // 未初始化的玩家下标（voids 为 null 分支）应返回 false
        assertFalse(tracker.isVoid(99, Suit.SPADE));
    }

    @Test
    void testGetPlayedCardsIsUnmodifiable() {
        CardTracker tracker = new CardTracker();
        assertThrows(UnsupportedOperationException.class,
            () -> tracker.getPlayedCards().add(new Card(Suit.HEART, Rank.ACE, 0)));
    }
}
