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
import com.tlcsdm.game.daliandagunzifx.model.Deck;
import com.tlcsdm.game.daliandagunzifx.model.Rank;
import com.tlcsdm.game.daliandagunzifx.model.Suit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 「大连打滚子」业务规则测试（按 docs/规则.md 逐节组织）。
 * 本类作为规则驱动的可执行规格说明，覆盖牌数构成、发牌、埋底扣王、跟牌花色、
 * 常主、计分、升级、进贡/喝血等核心业务规则，便于后续重构时以规则为准绳回归验证。
 */
class DaGunZiRulesTest {

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

    /** 从玩家手牌中挑选 6 张非王牌作为可合法埋底的底牌。 */
    private static List<Card> selectNonJokerKitty(Player player, int count) {
        return player.getHand().stream()
            .filter(c -> c.getRank() != Rank.SMALL_JOKER && c.getRank() != Rank.BIG_JOKER)
            .limit(count)
            .toList();
    }

    @Nested
    @DisplayName("发牌与牌数（规则：3副牌共162张，每人39张，底牌6张）")
    class DealingRules {

        @Test
        @DisplayName("整副牌共162张：4花色×13点×3副 + 6张王")
        void testTotalCardCount() {
            Deck deck = new Deck();
            List<Card> all = deck.deal(162);
            assertEquals(162, all.size(), "整副牌应为162张");

            long jokers = all.stream()
                .filter(c -> c.getRank() == Rank.SMALL_JOKER || c.getRank() == Rank.BIG_JOKER)
                .count();
            long smallJokers = all.stream().filter(c -> c.getRank() == Rank.SMALL_JOKER).count();
            long bigJokers = all.stream().filter(c -> c.getRank() == Rank.BIG_JOKER).count();
            assertEquals(6, jokers, "应有6张王");
            assertEquals(3, smallJokers, "应有3张小王");
            assertEquals(3, bigJokers, "应有3张大王");
            assertEquals(156, all.size() - jokers, "标准牌应为 4×13×3=156 张");

            assertEquals(0, deck.remaining(), "发完162张后应无剩余");
        }

        @Test
        @DisplayName("单局总分300分：5=5分、10=10分、K=10分")
        void testTotalPointsIs300() {
            int total = 0;
            for (Card c : new Deck().deal(162)) {
                total += c.getPoints();
            }
            assertEquals(300, total, "单局分牌总分应为300");

            assertEquals(5, new Card(Suit.SPADE, Rank.FIVE, 0).getPoints(), "5应为5分");
            assertEquals(10, new Card(Suit.SPADE, Rank.TEN, 0).getPoints(), "10应为10分");
            assertEquals(10, new Card(Suit.SPADE, Rank.KING, 0).getPoints(), "K应为10分");
            assertEquals(0, new Card(Suit.SPADE, Rank.ACE, 0).getPoints(), "A应为0分");
        }

        @Test
        @DisplayName("发牌后每人39张、底牌6张")
        void testDealDistribution() {
            engine.startNewRound();
            for (Player p : players) {
                assertEquals(39, p.getHand().size(), "每人应发到39张");
            }
            assertEquals(6, engine.getKitty().size(), "底牌应为6张");
        }
    }

    @Nested
    @DisplayName("埋底扣王（规则：扣大王=2血，扣小王=1血）")
    class BuryKittyRules {

        @Test
        @DisplayName("扣大王=2血")
        void testBuryBigJokerGivesTwoBloods() {
            engine.startNewRound();
            engine.declareTrump(0, Suit.SPADE);
            List<Card> kitty = new ArrayList<>(selectNonJokerKitty(players[0], 5));
            kitty.add(new Card(null, Rank.BIG_JOKER, 700));
            players[0].addCards(List.of(kitty.get(kitty.size() - 1)));
            engine.setKitty(kitty);
            assertEquals(2, engine.getKittyBloods(), "扣一张大王应为2血");
        }

        @Test
        @DisplayName("扣小王=1血")
        void testBurySmallJokerGivesOneBlood() {
            engine.startNewRound();
            engine.declareTrump(0, Suit.SPADE);
            List<Card> kitty = new ArrayList<>(selectNonJokerKitty(players[0], 5));
            Card smallJoker = new Card(null, Rank.SMALL_JOKER, 701);
            kitty.add(smallJoker);
            players[0].addCards(List.of(smallJoker));
            engine.setKitty(kitty);
            assertEquals(1, engine.getKittyBloods(), "扣一张小王应为1血");
        }

        @Test
        @DisplayName("不扣王=0血")
        void testBuryNoJokerGivesZeroBlood() {
            engine.startNewRound();
            engine.declareTrump(0, Suit.SPADE);
            engine.setKitty(selectNonJokerKitty(players[0], 6));
            assertEquals(0, engine.getKittyBloods(), "不扣王应为0血");
        }
    }

    @Nested
    @DisplayName("出牌与跟花色（规则：必须跟领出花色，无该花色才能垫/毙；常主2恒为主）")
    class FollowSuitRules {

        @Test
        @DisplayName("有领出花色时必须跟，不能垫其它副牌")
        void testMustFollowLeadSuit() {
            engine.startNewRound();
            engine.declareTrump(0, Suit.SPADE); // 黑桃为主
            engine.setKitty(selectNonJokerKitty(players[0], 6));

            // 玩家0 领出红桃副牌，玩家1 持有红桃与梅花
            players[0].getHand().clear();
            Card heart7 = new Card(Suit.HEART, Rank.SEVEN, 800);
            players[0].addCards(List.of(heart7));
            engine.playCard(0, heart7);

            players[1].getHand().clear();
            Card heart9 = new Card(Suit.HEART, Rank.NINE, 801);
            Card club8 = new Card(Suit.CLUB, Rank.EIGHT, 802);
            players[1].addCards(List.of(heart9, club8));

            assertTrue(engine.isValidPlay(1, heart9), "有红桃应允许跟红桃");
            assertFalse(engine.isValidPlay(1, club8), "有红桃时不能垫梅花");
        }

        @Test
        @DisplayName("无领出花色时可垫任意牌")
        void testCanDiscardWhenVoid() {
            engine.startNewRound();
            engine.declareTrump(0, Suit.SPADE);
            engine.setKitty(selectNonJokerKitty(players[0], 6));

            players[0].getHand().clear();
            Card heart7 = new Card(Suit.HEART, Rank.SEVEN, 810);
            players[0].addCards(List.of(heart7));
            engine.playCard(0, heart7);

            players[1].getHand().clear();
            Card club8 = new Card(Suit.CLUB, Rank.EIGHT, 811);
            players[1].addCards(List.of(club8));
            assertTrue(engine.isValidPlay(1, club8), "缺红桃时可垫任意牌");
        }

        @Test
        @DisplayName("2 恒为主牌（常主），不随级牌变化")
        void testTwoAlwaysTrump() {
            engine.startNewRound();
            engine.declareTrump(0, Suit.HEART); // 红桃为主，级牌默认3
            engine.setKitty(selectNonJokerKitty(players[0], 6));
            TrumpInfo trumpInfo = engine.getTrumpInfo();
            // 四门花色的 2 都应为主牌
            for (Suit s : Suit.values()) {
                assertTrue(trumpInfo.isTrump(new Card(s, Rank.TWO, 0)),
                    s.getDisplayName() + "2 应为主牌");
            }
        }

        @Test
        @DisplayName("主牌最大者赢墩：常主2可管上主花K")
        void testTwoBeatsTrumpSuitKing() {
            engine.startNewRound();
            engine.declareTrump(0, Suit.HEART);
            engine.setKitty(selectNonJokerKitty(players[0], 6));
            TrumpInfo trumpInfo = engine.getTrumpInfo();
            Card trumpTwo = new Card(Suit.SPADE, Rank.TWO, 0);     // 常主2（副花）
            Card trumpKing = new Card(Suit.HEART, Rank.KING, 0);   // 主花K
            assertTrue(trumpInfo.getCardStrength(trumpTwo) > trumpInfo.getCardStrength(trumpKing),
                "常主2应比主花K大");
        }
    }

    @Nested
    @DisplayName("升级（规则：闲家<120庄家升级；≥120闲家升级反庄；≥120且抠底额外+1）")
    class UpgradeRules {

        @Test
        @DisplayName("闲家<120：庄家队升级1级")
        void testDeclarerUpgradesWhenDefenderUnder120() {
            RoundResult r = new RoundResult(80, 0);
            assertTrue(r.isDeclarerWins());
            assertEquals(0, r.getWinningTeam(), "庄家队(0)获胜");
            assertEquals(1, r.getLevelChange(), "庄家升1级");
        }

        @Test
        @DisplayName("闲家≥120：闲家队升级（反庄）")
        void testDefenderUpgradesWhenAtLeast120() {
            RoundResult r = new RoundResult(120, 0);
            assertFalse(r.isDeclarerWins());
            assertEquals(1, r.getWinningTeam(), "闲家队(1)获胜");
            assertEquals(1, r.getLevelChange(), "闲家升1级");
        }

        @Test
        @DisplayName("闲家≥120且抠底：闲家在常规基础上额外再升1级")
        void testDefenderExtraUpgradeOnKouDi() {
            RoundResult r = new RoundResult(120, 0, 0, true);
            assertEquals(1, r.getWinningTeam());
            assertEquals(2, r.getLevelChange(), "抠底额外+1，共升2级");
        }
    }

    @Nested
    @DisplayName("进贡/喝血（规则：<80每少10分多进1贡；>150每多10分多吃1血；扣王成功升级+收贡，扣王失败不加级+进贡）")
    class TributeRules {

        @Test
        @DisplayName("闲家<80：每少10分多进1贡（0分=8贡，70分=1贡）")
        void testLowScoreTribute() {
            assertEquals(8, new RoundResult(0, 0).getTributeCount(), "0分进8贡");
            assertEquals(1, new RoundResult(70, 0).getTributeCount(), "70分进1贡");
            assertEquals(0, new RoundResult(80, 0).getTributeCount(), "80分不进贡");
        }

        @Test
        @DisplayName("闲家>150：每多10分多吃1血")
        void testHighScoreTribute() {
            assertEquals(0, new RoundResult(150, 0).getTributeCount(), "150分不吃血");
            assertEquals(2, new RoundResult(170, 0).getTributeCount(), "170分吃2血");
            assertEquals(5, new RoundResult(200, 0).getTributeCount(), "200分吃5血");
        }

        @Test
        @DisplayName("扣王成功（庄家扣王、闲家<120）：既升级又收贡")
        void testKouWangSuccessUpgradeAndTribute() {
            RoundResult r = new RoundResult(100, 0, 2); // 扣大王2血，闲家100分
            assertTrue(r.isDeclarerWins(), "扣王成功，庄家赢");
            assertEquals(3, r.getLevelChange(), "基础1级 + 扣王2级 = 3级");
            assertEquals(2, r.getTributeCount(), "扣王成功仍收2贡（庄家对门下局收贡）");
        }

        @Test
        @DisplayName("扣王失败（庄家扣王但闲家≥120）：不加级，仍按大王2贡/小王1贡进贡")
        void testKouWangFailureNoUpgradeButTribute() {
            RoundResult rBig = new RoundResult(120, 0, 2); // 扣大王2血，闲家120分
            assertFalse(rBig.isDeclarerWins(), "扣王失败，闲家赢");
            assertEquals(1, rBig.getLevelChange(), "扣王失败不加级，仅基础1级");
            assertEquals(2, rBig.getTributeCount(), "扣大王失败进2贡");

            RoundResult rSmall = new RoundResult(120, 0, 1); // 扣小王1血
            assertEquals(1, rSmall.getTributeCount(), "扣小王失败进1贡");
        }
    }
}
