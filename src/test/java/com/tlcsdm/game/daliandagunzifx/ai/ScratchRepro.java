package com.tlcsdm.game.daliandagunzifx.ai;

import com.tlcsdm.game.daliandagunzifx.engine.GameEngine;
import com.tlcsdm.game.daliandagunzifx.engine.Player;
import com.tlcsdm.game.daliandagunzifx.model.Card;
import com.tlcsdm.game.daliandagunzifx.model.Rank;
import com.tlcsdm.game.daliandagunzifx.model.Suit;
import com.tlcsdm.game.daliandagunzifx.tracker.CardTracker;
import org.junit.jupiter.api.Test;

import java.util.List;

class ScratchRepro {

    private static Player[] mkPlayers() {
        return new Player[]{
            new Player(0, "P0", false),
            new Player(1, "P1", false),
            new Player(2, "P2", false),
            new Player(3, "P3", false)
        };
    }

    private static GameEngine mkEngine(Player[] players, Suit trump) {
        GameEngine engine = new GameEngine(players);
        engine.startNewRound();
        engine.declareTrump(0, trump);
        List<Card> kittyCards = players[0].getHand().stream()
            .filter(c -> c.getRank() != Rank.SMALL_JOKER && c.getRank() != Rank.BIG_JOKER)
            .limit(6).toList();
        engine.setKitty(kittyCards);
        return engine;
    }

    // item 5: 打3(级牌=3), 领出副牌, 前三小副, 第四家(AI)缺门须用主牌管, 看出什么主牌
    @Test
    void item5_mediumVoidRuffCardChoice() {
        Player[] players = mkPlayers();
        GameEngine engine = mkEngine(players, Suit.HEART); // trumpRank=THREE
        // P0 领 ♠6, P1 ♠7, P2 ♠8（都 0 分，P2 赢着）
        players[0].getHand().clear();
        Card s6 = new Card(Suit.SPADE, Rank.SIX, 600);
        players[0].addCards(List.of(s6));
        engine.playCard(0, s6);
        players[1].getHand().clear();
        Card s7 = new Card(Suit.SPADE, Rank.SEVEN, 601);
        players[1].addCards(List.of(s7));
        engine.playCard(1, s7);
        players[2].getHand().clear();
        Card s8 = new Card(Suit.SPADE, Rank.EIGHT, 602);
        players[2].addCards(List.of(s8));
        engine.playCard(2, s8);
        // P3 (AI): 无黑桃, 持 ♥3(级牌/主牌3), ♥4(小主), ♥5(主牌分), 无副牌可垫
        players[3].getHand().clear();
        Card h3 = new Card(Suit.HEART, Rank.THREE, 603);
        Card h4 = new Card(Suit.HEART, Rank.FOUR, 604);
        Card h5 = new Card(Suit.HEART, Rank.FIVE, 605);
        players[3].addCards(List.of(h3, h4, h5));

        MediumAI ai = new MediumAI(new CardTracker());
        Card chosen = ai.chooseCard(players[3], engine);
        System.out.println("[item5-medium-noDiscard] chosen=" + chosen.getRank() + " " + chosen.getSuit());
    }

    @Test
    void item5_mediumVoidRuffWithDiscardAvailable() {
        Player[] players = mkPlayers();
        GameEngine engine = mkEngine(players, Suit.HEART);
        players[0].getHand().clear();
        Card s6 = new Card(Suit.SPADE, Rank.SIX, 600);
        players[0].addCards(List.of(s6));
        engine.playCard(0, s6);
        players[1].getHand().clear();
        Card s7 = new Card(Suit.SPADE, Rank.SEVEN, 601);
        players[1].addCards(List.of(s7));
        engine.playCard(1, s7);
        players[2].getHand().clear();
        Card s8 = new Card(Suit.SPADE, Rank.EIGHT, 602);
        players[2].addCards(List.of(s8));
        engine.playCard(2, s8);
        // P3 (AI): 无黑桃, 持 ♥3(级牌), ♥4, 以及 ♣7(无分副牌可垫)
        players[3].getHand().clear();
        Card h3 = new Card(Suit.HEART, Rank.THREE, 603);
        Card h4 = new Card(Suit.HEART, Rank.FOUR, 604);
        Card c7 = new Card(Suit.CLUB, Rank.SEVEN, 605);
        players[3].addCards(List.of(h3, h4, c7));

        MediumAI ai = new MediumAI(new CardTracker());
        Card chosen = ai.chooseCard(players[3], engine);
        System.out.println("[item5-medium-canDiscard] chosen=" + chosen.getRank() + " " + chosen.getSuit());
    }

    // item 4: 跟单张, 手里有 ♣4♣4 棒子 + ♣8 单张, 领出 ♣9(0分), 看是否拆棒出 ♣4
    @Test
    void item4_followDoesNotBreakPair() {
        Player[] players = mkPlayers();
        GameEngine engine = mkEngine(players, Suit.HEART);
        players[0].getHand().clear();
        Card c9 = new Card(Suit.CLUB, Rank.NINE, 700);
        players[0].addCards(List.of(c9));
        engine.playCard(0, c9);
        // P1 (AI): ♣4 ♣4 (棒子) + ♣8 (单张) → 应跟 ♣8 保棒, 不拆 ♣4
        players[1].getHand().clear();
        Card c4a = new Card(Suit.CLUB, Rank.FOUR, 701);
        Card c4b = new Card(Suit.CLUB, Rank.FOUR, 702);
        Card c8 = new Card(Suit.CLUB, Rank.EIGHT, 703);
        players[1].addCards(List.of(c4a, c4b, c8));
        MediumAI ai = new MediumAI(new CardTracker());
        Card chosen = ai.chooseCard(players[1], engine);
        System.out.println("[item4-medium] chosen=" + chosen.getRank() + " " + chosen.getSuit()
            + " (期望 CLUB EIGHT, 不拆 ♣4 棒)");

        EasyAI easy = new EasyAI();
        Card chosenE = easy.chooseCard(players[1], engine);
        System.out.println("[item4-easy] chosen=" + chosenE.getRank() + " " + chosenE.getSuit()
            + " (期望 CLUB EIGHT)");
    }

    // item 5 对照: EasyAI 4家(最后)缺门, 墩面0分, 有安全副牌可垫, 是否垫牌而非拆主
    @Test
    void item5_easyVoidNoPointsCanDiscard() {
        Player[] players = mkPlayers();
        GameEngine engine = mkEngine(players, Suit.HEART);
        players[0].getHand().clear();
        Card s6 = new Card(Suit.SPADE, Rank.SIX, 600);
        players[0].addCards(List.of(s6));
        engine.playCard(0, s6);
        players[1].getHand().clear();
        Card s7 = new Card(Suit.SPADE, Rank.SEVEN, 601);
        players[1].addCards(List.of(s7));
        engine.playCard(1, s7);
        players[2].getHand().clear();
        Card s8 = new Card(Suit.SPADE, Rank.EIGHT, 602);
        players[2].addCards(List.of(s8));
        engine.playCard(2, s8);
        players[3].getHand().clear();
        Card h4 = new Card(Suit.HEART, Rank.FOUR, 604);
        Card c7 = new Card(Suit.CLUB, Rank.SEVEN, 605);
        players[3].addCards(List.of(h4, c7));
        EasyAI easy = new EasyAI();
        Card chosen = easy.chooseCard(players[3], engine);
        System.out.println("[item5-easy-canDiscard] chosen=" + chosen.getRank() + " " + chosen.getSuit()
            + " (期望 CLUB SEVEN 垫牌)");
    }

    // item 3: 3家(AI)为本队最后防线, 队友(1家)已出小副且未赢, 2家(对手)赢着, 墩面 0 分,
    //         AI 持该副 boss(A) + 小牌, 看是否盖牌防 4 家关门得分
    @Test
    void item3_thirdHandCovers() {
        Player[] players = mkPlayers();
        GameEngine engine = mkEngine(players, Suit.HEART);
        CardTracker tracker = new CardTracker();
        // P0(AI队友) 领 ♠4 (0分)
        players[0].getHand().clear();
        Card s4 = new Card(Suit.SPADE, Rank.FOUR, 800);
        players[0].addCards(List.of(s4));
        engine.playCard(0, s4);
        // P1(对手) ♠9 (0分, 赢着)
        players[1].getHand().clear();
        Card s9 = new Card(Suit.SPADE, Rank.NINE, 801);
        players[1].addCards(List.of(s9));
        engine.playCard(1, s9);
        // P2(AI, 第3家): 持 ♠A(boss) + ♠3?(no, 3是级牌→trump). 用 ♠6 小牌
        players[2].getHand().clear();
        Card sA = new Card(Suit.SPADE, Rank.ACE, 802);
        Card s6 = new Card(Suit.SPADE, Rank.SIX, 803);
        players[2].addCards(List.of(sA, s6));
        MediumAI ai = new MediumAI(tracker);
        Card chosen = ai.chooseCard(players[2], engine);
        System.out.println("[item3-medium] chosen=" + chosen.getRank() + " " + chosen.getSuit()
            + " (期望 SPADE ACE 盖牌防关门)");
    }
}
