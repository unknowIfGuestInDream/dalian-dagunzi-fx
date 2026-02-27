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
    void testTwoRanksAboveAceForNonTrump() {
        // Use a trump rank other than TWO so TWO is treated as a normal card
        TrumpInfo threesTrump = new TrumpInfo(Suit.HEART, Rank.THREE);

        Card nonTrumpTwo = new Card(Suit.SPADE, Rank.TWO, 300);
        Card nonTrumpAce = new Card(Suit.SPADE, Rank.ACE, 301);
        Card nonTrumpKing = new Card(Suit.SPADE, Rank.KING, 302);

        // Da Gunzi: 2 > A > K
        assertTrue(threesTrump.getCardStrength(nonTrumpTwo)
            > threesTrump.getCardStrength(nonTrumpAce));
        assertTrue(threesTrump.getCardStrength(nonTrumpAce)
            > threesTrump.getCardStrength(nonTrumpKing));
    }

    @Test
    void testTwoRanksAboveAceInTrumpSuit() {
        // Trump suit HEART, trump rank THREE
        TrumpInfo threesTrump = new TrumpInfo(Suit.HEART, Rank.THREE);

        Card trumpSuitTwo = new Card(Suit.HEART, Rank.TWO, 310);
        Card trumpSuitAce = new Card(Suit.HEART, Rank.ACE, 311);
        Card trumpSuitKing = new Card(Suit.HEART, Rank.KING, 312);

        // In trump suit, 2 should still be above A and K
        assertTrue(threesTrump.getCardStrength(trumpSuitTwo)
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
