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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TrumpInfoTest {

    // 主花色 HEART，主牌级 THREE（实际对局中级别从 3 开始，2 永远不会是主牌级）。
    private final TrumpInfo trumpInfo = new TrumpInfo(Suit.HEART, Rank.THREE);

    @Test
    void testJokersAreTrump() {
        Card bigJoker = new Card(null, Rank.BIG_JOKER, 200);
        Card smallJoker = new Card(null, Rank.SMALL_JOKER, 201);
        assertTrue(trumpInfo.isTrump(bigJoker));
        assertTrue(trumpInfo.isTrump(smallJoker));
    }

    @Test
    void testTrumpSuitIsTrump() {
        Card heartAce = new Card(Suit.HEART, Rank.ACE, 10);
        assertTrue(trumpInfo.isTrump(heartAce));
    }

    @Test
    void testTrumpRankIsTrump() {
        // Trump rank is THREE; a THREE of any suit should be trump
        Card threeSpades = new Card(Suit.SPADE, Rank.THREE, 20);
        assertTrue(trumpInfo.isTrump(threeSpades));
    }

    @Test
    void testNonTrumpCard() {
        Card aceSpades = new Card(Suit.SPADE, Rank.ACE, 30);
        assertFalse(trumpInfo.isTrump(aceSpades));
    }

    @Test
    void testTwoIsNotTrumpUnlessTrumpSuit() {
        // 在大连打滚子中，2 不是常主，只是最大的普通牌。
        Card twoSpades = new Card(Suit.SPADE, Rank.TWO, 40);
        Card twoHearts = new Card(Suit.HEART, Rank.TWO, 41);

        // 副花色的 2 不是主牌
        assertFalse(trumpInfo.isTrump(twoSpades));
        assertEquals(Suit.SPADE, trumpInfo.getEffectiveSuit(twoSpades));

        // 主花色的 2 才是主牌（属于主花色）
        assertTrue(trumpInfo.isTrump(twoHearts));
        assertNull(trumpInfo.getEffectiveSuit(twoHearts));
    }

    @Test
    void testCardStrengthOrder() {
        Card bigJoker = new Card(null, Rank.BIG_JOKER, 200);
        Card smallJoker = new Card(null, Rank.SMALL_JOKER, 201);
        Card trumpRankInTrumpSuit = new Card(Suit.HEART, Rank.THREE, 202);
        Card trumpRankOtherSuit = new Card(Suit.SPADE, Rank.THREE, 203);
        Card trumpSuitTwo = new Card(Suit.HEART, Rank.TWO, 204);
        Card trumpSuitAce = new Card(Suit.HEART, Rank.ACE, 205);
        Card nonTrumpTwo = new Card(Suit.SPADE, Rank.TWO, 206);
        Card nonTrumpAce = new Card(Suit.SPADE, Rank.ACE, 207);

        assertTrue(trumpInfo.getCardStrength(bigJoker) > trumpInfo.getCardStrength(smallJoker));
        assertTrue(trumpInfo.getCardStrength(smallJoker) > trumpInfo.getCardStrength(trumpRankInTrumpSuit));
        assertTrue(trumpInfo.getCardStrength(trumpRankInTrumpSuit) > trumpInfo.getCardStrength(trumpRankOtherSuit));
        // 主花色的 2 是最大的主花色普通牌（高于主花色 A），但低于主牌级
        assertTrue(trumpInfo.getCardStrength(trumpRankOtherSuit) > trumpInfo.getCardStrength(trumpSuitTwo));
        assertTrue(trumpInfo.getCardStrength(trumpSuitTwo) > trumpInfo.getCardStrength(trumpSuitAce));
        // 任意主牌都大于副牌
        assertTrue(trumpInfo.getCardStrength(trumpSuitAce) > trumpInfo.getCardStrength(nonTrumpTwo));
        // 副牌中 2 最大，高于 A
        assertTrue(trumpInfo.getCardStrength(nonTrumpTwo) > trumpInfo.getCardStrength(nonTrumpAce));
    }

    @Test
    void testEffectiveSuit() {
        Card bigJoker = new Card(null, Rank.BIG_JOKER, 200);
        Card trumpSuitCard = new Card(Suit.HEART, Rank.ACE, 204);
        Card trumpRankCard = new Card(Suit.SPADE, Rank.THREE, 203);
        Card nonTrumpCard = new Card(Suit.SPADE, Rank.ACE, 205);
        Card nonTrumpTwo = new Card(Suit.SPADE, Rank.TWO, 208);

        assertNull(trumpInfo.getEffectiveSuit(bigJoker));
        assertNull(trumpInfo.getEffectiveSuit(trumpSuitCard));
        assertNull(trumpInfo.getEffectiveSuit(trumpRankCard));
        assertEquals(Suit.SPADE, trumpInfo.getEffectiveSuit(nonTrumpCard));
        // 副花色的 2 跟普通副牌一样，按本花色跟牌
        assertEquals(Suit.SPADE, trumpInfo.getEffectiveSuit(nonTrumpTwo));
    }

    @Test
    void testTwoOfTrumpSuitIsHighestTrumpSuitCard() {
        // 主花色 HEART，主牌级 THREE；主花色的 2 是最大的主花色普通牌
        Card trumpSuitTwo = new Card(Suit.HEART, Rank.TWO, 310);
        Card trumpSuitAce = new Card(Suit.HEART, Rank.ACE, 311);
        Card trumpSuitKing = new Card(Suit.HEART, Rank.KING, 312);

        assertTrue(trumpInfo.getCardStrength(trumpSuitTwo)
            > trumpInfo.getCardStrength(trumpSuitAce));
        assertTrue(trumpInfo.getCardStrength(trumpSuitAce)
            > trumpInfo.getCardStrength(trumpSuitKing));
    }

    @Test
    void testEffectiveRankStrengthTwoAboveAce() {
        // Da Gunzi ranking: 2 > A > K > Q > ...
        assertTrue(TrumpInfo.effectiveRankStrength(Rank.TWO)
            > TrumpInfo.effectiveRankStrength(Rank.ACE));
        assertTrue(TrumpInfo.effectiveRankStrength(Rank.ACE)
            > TrumpInfo.effectiveRankStrength(Rank.KING));
        assertTrue(TrumpInfo.effectiveRankStrength(Rank.KING)
            > TrumpInfo.effectiveRankStrength(Rank.THREE));
    }
}
