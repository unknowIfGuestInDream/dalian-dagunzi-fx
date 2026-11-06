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
package com.tlcsdm.game.daliandagunzifx.ui;

/**
 * 大连打滚子规则文案，供规则界面与游戏内规则弹窗共用。
 *
 * @author unknowIfGuestInDream
 */
public final class RulesContent {

    private RulesContent() {
    }

    public static final String[][] TERMINOLOGY = {
        {"棒子", "一对相同花色的相同牌，如2张红桃5"},
        {"滚子", "3张相同花色的相同牌，如3张红桃5"},
        {"庄家", "收底玩家，第一局谁有大王并亮大王的人是庄，之后叫主的人当庄。赢方互换，赢方若输，则庄家为上局庄的下家。"},
        {"反庄", "抓分方得到超过120分，则下一局反庄"},
        {"扣王", "把王放进底牌，即为扣王，一张大王升2级并且吃2个贡，一张小王升1级并且吃一个贡，扣王则可以看到底牌，用王换掉一张最小的牌"},
        {"升级", "跑分方让抓分方得到低于120分，则升1级，再加上扣王的级，1张大王升2级，1张小王升1级；抓分方抓到超过120分，并且抠底，则升一级，再加上扣王的级，若超过120分，但是没抠底，则不升级，只换庄。"},
        {"喝血", "即为进贡，扣王失败（抓分方扣王但没抓到120分，或者跑分方扣王让抓分方抓到超过120分，即算扣王失败）1扣一张大王要进2张贡，1张小王进1张贡，下一局由扣王玩家进给庄家。另外抓分方得到超过150分，每多10分就多吃一张贡，若抓分方抓到低于80分，则少抓10分多进一张贡出去。"},
    };

    public static final String[][] CARD_TYPES = {
        {"单张", "任意一张牌"},
        {"棒子（对子）", "两张相同花色且相同点数的牌，如两张红桃5"},
        {"滚子（三条）", "三张相同花色且相同点数的牌，如三张红桃5"},
    };

    public static final String[][] GAME_FLOW = {
        {"发牌", "3副牌共162张，每人39张，6张底牌"},
        {"叫主", "第一局需要有大王才能亮牌定庄；之后每局需要有2张或以上当前级别的主牌才能叫主"},
        {"埋底", "庄家从底牌中选6张牌埋入底牌，可以扣王（大王=2血，小王=1血）"},
        {"出牌", "从庄家开始，按顺序出牌。必须跟领出的花色，没有则可以出任意牌。领出棒子（对子）或滚子（三条）时，"
            + "跟牌方手中有同花色对子/三条的必须保持牌组完整。主牌最大者赢得此墩。不能出顺子、拖拉机和泰坦尼克，也不能甩牌。"},
        {"计分", "5分=5分，10分=10分，K=10分，每局共300分。抓分方得分<120分庄家升级，≥120分抓分方升级。"},
        {"升级", "从3开始，打碎10获胜。升级数取决于得分差距和扣王情况。"},
        {"进贡", "得分<80分时，每少10分多进一个贡；得分>150分时，每多10分多吃一个血。"},
    };
}
