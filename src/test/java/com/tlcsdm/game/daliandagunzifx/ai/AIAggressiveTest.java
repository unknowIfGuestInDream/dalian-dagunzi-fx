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
import com.tlcsdm.game.daliandagunzifx.model.Rank;
import com.tlcsdm.game.daliandagunzifx.model.Suit;
import com.tlcsdm.game.daliandagunzifx.tracker.CardTracker;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 冒险出牌策略（赌博心理）相关测试：开启后 AI 应主动争墩、用主牌毙分，
 * 而非保守地掉主弃墩；关闭时保持原有保守行为。
 */
class AIAggressiveTest {

    private Player[] newPlayers() {
        return new Player[]{
            new Player(0, "P0", true),
            new Player(1, "P1", false),
            new Player(2, "P2", true),
            new Player(3, "P3", false)
        };
    }

    private GameEngine newEngineHeartTrump(Player[] players) {
        GameEngine engine = new GameEngine(players);
        engine.startNewRound();
        engine.declareTrump(0, Suit.HEART);
        List<Card> kittyCards = players[0].getHand().stream()
            .filter(c -> c.getRank() != Rank.SMALL_JOKER && c.getRank() != Rank.BIG_JOKER)
            .limit(6)
            .toList();
        engine.setKitty(kittyCards);
        return engine;
    }

    @Test
    void testAggressiveLeadPlaysHighestNonPointSideCard() {
        Player[] players = newPlayers();
        GameEngine engine = newEngineHeartTrump(players);

        Player leader = players[engine.getCurrentPlayerIndex()];
        leader.getHand().clear();
        // 短套 ♦9（0分）与长套 ♠Q/♠7/♠8（0分，♠Q 牌力最大）
        Card diamond9 = new Card(Suit.DIAMOND, Rank.NINE, 800);
        Card spadeQ = new Card(Suit.SPADE, Rank.QUEEN, 801);
        Card spade7 = new Card(Suit.SPADE, Rank.SEVEN, 802);
        Card spade8 = new Card(Suit.SPADE, Rank.EIGHT, 803);
        leader.addCards(List.of(diamond9, spadeQ, spade7, spade8));

        // 保守：制造缺门，掉短套小牌 ♦9
        EasyAI conservative = new EasyAI();
        assertEquals(Rank.NINE, conservative.chooseCard(leader, engine).getRank(),
            "保守策略应领出短套小牌 ♦9");

        // 冒险：主动领出最大的非分副牌 ♠Q 争夺控制权
        EasyAI aggressive = new EasyAI();
        aggressive.setAggressive(true);
        assertEquals(Rank.QUEEN, aggressive.chooseCard(leader, engine).getRank(),
            "冒险策略应领出最大的非分副牌 ♠Q");
    }

    @Test
    void testAggressiveSuitFollowContestsZeroPointTrick() {
        Player[] players = newPlayers();
        GameEngine engine = newEngineHeartTrump(players);

        // P0 领出 ♠9（0分）
        players[0].getHand().clear();
        Card spade9 = new Card(Suit.SPADE, Rank.NINE, 900);
        players[0].addCards(List.of(spade9));
        engine.playCard(0, spade9);

        // P1（对手）有 ♠7 与 ♠J，本墩无分
        players[1].getHand().clear();
        Card spade7 = new Card(Suit.SPADE, Rank.SEVEN, 901);
        Card spadeJ = new Card(Suit.SPADE, Rank.JACK, 902);
        players[1].addCards(List.of(spade7, spadeJ));

        players[2].getHand().clear();
        players[2].addCards(List.of(new Card(Suit.DIAMOND, Rank.FOUR, 910)));
        players[3].getHand().clear();
        players[3].addCards(List.of(new Card(Suit.DIAMOND, Rank.SIX, 911)));

        // 保守：无分墩不争，掉小牌 ♠7
        EasyAI conservative = new EasyAI();
        assertEquals(Rank.SEVEN, conservative.chooseCard(players[1], engine).getRank(),
            "保守策略在无分墩应掉小牌 ♠7");

        // 冒险：主动争墩夺取控制权，出能赢的最小牌 ♠J
        EasyAI aggressive = new EasyAI();
        aggressive.setAggressive(true);
        assertEquals(Rank.JACK, aggressive.chooseCard(players[1], engine).getRank(),
            "冒险策略应主动争墩出 ♠J");
    }

    @Test
    void testAggressiveOffSuitRuffsPointTrick() {
        Player[] players = newPlayers();
        GameEngine engine = newEngineHeartTrump(players);

        // P0 领出 ♠K（10分）
        players[0].getHand().clear();
        Card spadeK = new Card(Suit.SPADE, Rank.KING, 900);
        players[0].addCards(List.of(spadeK));
        engine.playCard(0, spadeK);

        // P1（对手）无 ♠ 可跟：有安全垫牌 ♣3（0分）与非特殊主牌 ♥4
        players[1].getHand().clear();
        Card club7 = new Card(Suit.CLUB, Rank.SEVEN, 901);
        Card heart4 = new Card(Suit.HEART, Rank.FOUR, 902);
        players[1].addCards(List.of(club7, heart4));

        players[2].getHand().clear();
        players[2].addCards(List.of(new Card(Suit.DIAMOND, Rank.FOUR, 910)));
        players[3].getHand().clear();
        players[3].addCards(List.of(new Card(Suit.DIAMOND, Rank.SIX, 911)));

        TrumpInfo trumpInfo = engine.getTrumpInfo();

        // 保守：有安全无分非主牌可垫，垫 ♣3（不争分）
        EasyAI conservative = new EasyAI();
        Card consChosen = conservative.chooseCard(players[1], engine);
        assertFalse(trumpInfo.isTrump(consChosen),
            "保守策略应垫非主牌 ♣7，实际: " + consChosen.getDisplayName());

        // 冒险：本墩有分，主动用主牌 ♥4 毙分
        EasyAI aggressive = new EasyAI();
        aggressive.setAggressive(true);
        Card aggChosen = aggressive.chooseCard(players[1], engine);
        assertTrue(trumpInfo.isTrump(aggChosen),
            "冒险策略应用主牌毙分，实际: " + aggChosen.getDisplayName());
    }

    @Test
    void testMediumAiAggressiveLeadPlaysHighestNonPointSideCard() {
        Player[] players = newPlayers();
        GameEngine engine = newEngineHeartTrump(players);

        Player leader = players[engine.getCurrentPlayerIndex()];
        leader.getHand().clear();
        Card diamond9 = new Card(Suit.DIAMOND, Rank.NINE, 800);
        Card spadeQ = new Card(Suit.SPADE, Rank.QUEEN, 801);
        Card spade7 = new Card(Suit.SPADE, Rank.SEVEN, 802);
        Card spade8 = new Card(Suit.SPADE, Rank.EIGHT, 803);
        leader.addCards(List.of(diamond9, spadeQ, spade7, spade8));

        MediumAI aggressive = new MediumAI(new CardTracker());
        aggressive.setAggressive(true);
        assertEquals(Rank.QUEEN, aggressive.chooseCard(leader, engine).getRank(),
            "MediumAI 冒险策略应领出最大的非分副牌 ♠Q");
    }

    @Test
    void testAggressiveDefaultsOff() {
        EasyAI easyAI = new EasyAI();
        assertFalse(easyAI.isAggressive(), "EasyAI 冒险策略默认关闭");
        easyAI.setAggressive(true);
        assertTrue(easyAI.isAggressive());
    }
}
