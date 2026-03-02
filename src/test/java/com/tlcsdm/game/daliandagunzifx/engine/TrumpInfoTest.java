/*
 * Copyright (c) 2025 unknowIfGuestInDream.
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

    private final TrumpInfo trumpInfo = new TrumpInfo(Suit.HEART, Rank.TWO);

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
        // Trump rank is TWO; a TWO of spades should be trump
        Card twoSpades = new Card(Suit.SPADE, Rank.TWO, 20);
        assertTrue(trumpInfo.isTrump(twoSpades));
    }

    @Test
    void testNonTrumpCard() {
        Card aceSpades = new Card(Suit.SPADE, Rank.ACE, 30);
        assertFalse(trumpInfo.isTrump(aceSpades));
    }

    @Test
    void testCardStrengthOrder() {
        Card bigJoker = new Card(null, Rank.BIG_JOKER, 200);
        Card smallJoker = new Card(null, Rank.SMALL_JOKER, 201);
        Card trumpRankInTrumpSuit = new Card(Suit.HEART, Rank.TWO, 202);
        Card trumpRankOtherSuit = new Card(Suit.SPADE, Rank.TWO, 203);
        Card trumpSuitAce = new Card(Suit.HEART, Rank.ACE, 204);
        Card nonTrumpAce = new Card(Suit.SPADE, Rank.ACE, 205);

        assertTrue(trumpInfo.getCardStrength(bigJoker) > trumpInfo.getCardStrength(smallJoker));
        assertTrue(trumpInfo.getCardStrength(smallJoker) > trumpInfo.getCardStrength(trumpRankInTrumpSuit));
        assertTrue(trumpInfo.getCardStrength(trumpRankInTrumpSuit) > trumpInfo.getCardStrength(trumpRankOtherSuit));
        assertTrue(trumpInfo.getCardStrength(trumpRankOtherSuit) > trumpInfo.getCardStrength(trumpSuitAce));
        assertTrue(trumpInfo.getCardStrength(trumpSuitAce) > trumpInfo.getCardStrength(nonTrumpAce));
    }

    @Test
    void testEffectiveSuit() {
        Card bigJoker = new Card(null, Rank.BIG_JOKER, 200);
        Card trumpSuitCard = new Card(Suit.HEART, Rank.ACE, 204);
        Card trumpRankCard = new Card(Suit.SPADE, Rank.TWO, 203);
        Card nonTrumpCard = new Card(Suit.SPADE, Rank.ACE, 205);

        assertNull(trumpInfo.getEffectiveSuit(bigJoker));
        assertNull(trumpInfo.getEffectiveSuit(trumpSuitCard));
        assertNull(trumpInfo.getEffectiveSuit(trumpRankCard));
        assertEquals(Suit.SPADE, trumpInfo.getEffectiveSuit(nonTrumpCard));
    }

    @Test
    void testTwoAlwaysTrump() {
        // TWO is always trump, even when trump rank is not TWO
        TrumpInfo threesTrump = new TrumpInfo(Suit.HEART, Rank.THREE);

        Card twoSpades = new Card(Suit.SPADE, Rank.TWO, 300);
        Card twoClubs = new Card(Suit.CLUB, Rank.TWO, 303);

        // All 2s should be trump regardless of suit
        assertTrue(threesTrump.isTrump(twoSpades));
        assertTrue(threesTrump.isTrump(twoClubs));
        assertNull(threesTrump.getEffectiveSuit(twoSpades));
        assertNull(threesTrump.getEffectiveSuit(twoClubs));

        // 2 should still be stronger than non-trump A and K
        Card nonTrumpAce = new Card(Suit.SPADE, Rank.ACE, 301);
        Card nonTrumpKing = new Card(Suit.SPADE, Rank.KING, 302);
        assertTrue(threesTrump.getCardStrength(twoSpades)
            > threesTrump.getCardStrength(nonTrumpAce));
        assertTrue(threesTrump.getCardStrength(nonTrumpAce)
            > threesTrump.getCardStrength(nonTrumpKing));
    }

    @Test
    void testTwoRanksAboveAceInTrumpSuit() {
        // Trump suit HEART, trump rank THREE; 2 of trump suit ranks higher than 2 of other suits
        TrumpInfo threesTrump = new TrumpInfo(Suit.HEART, Rank.THREE);

        Card trumpSuitTwo = new Card(Suit.HEART, Rank.TWO, 310);
        Card otherSuitTwo = new Card(Suit.SPADE, Rank.TWO, 313);
        Card trumpSuitAce = new Card(Suit.HEART, Rank.ACE, 311);
        Card trumpSuitKing = new Card(Suit.HEART, Rank.KING, 312);

        // 2 of trump suit > 2 of other suits > trump suit A > trump suit K
        assertTrue(threesTrump.getCardStrength(trumpSuitTwo)
            > threesTrump.getCardStrength(otherSuitTwo));
        assertTrue(threesTrump.getCardStrength(otherSuitTwo)
            > threesTrump.getCardStrength(trumpSuitAce));
        assertTrue(threesTrump.getCardStrength(trumpSuitAce)
            > threesTrump.getCardStrength(trumpSuitKing));
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
