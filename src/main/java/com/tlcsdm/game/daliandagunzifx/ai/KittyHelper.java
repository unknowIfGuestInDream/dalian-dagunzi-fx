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

import com.tlcsdm.game.daliandagunzifx.engine.TrumpInfo;
import com.tlcsdm.game.daliandagunzifx.model.Card;
import com.tlcsdm.game.daliandagunzifx.model.Rank;

import java.util.ArrayList;
import java.util.List;

/**
 * 庄家扣牌的公共逻辑。除了常规的扣最小牌外，还允许庄家在主牌足够强时考虑“扣王”
 * （扣小王换取额外升级），即问题中要求的“庄家扣牌时允许考虑扣王”。
 */
final class KittyHelper {

    /**
     * 庄家主牌数量达到该阈值时，认为主牌足够强、不依赖小王，可考虑扣小王搏一个升级。
     */
    private static final int STRONG_TRUMP_THRESHOLD = 16;

    private KittyHelper() {
    }

    /**
     * 从按“保留优先级升序”排好序的手牌中选出 6 张底牌。
     * 优先扣最小的非王牌；当主牌足够强时，考虑扣小王（扣王）。大王价值过高，从不主动扣。
     *
     * @param handSortedByKeepAsc 已按保留优先级升序排列的手牌（最先丢弃的在前）
     * @param trumpInfo           主牌信息
     * @return 选出的 6 张底牌（手牌不足 6 张时返回全部）
     */
    static List<Card> selectKitty(List<Card> handSortedByKeepAsc, TrumpInfo trumpInfo) {
        List<Card> result = new ArrayList<>();

        // 庄家扣牌时考虑扣王：主牌足够强时，扣小王（1 血/1 级）搏一个升级。
        if (shouldKouWang(handSortedByKeepAsc, trumpInfo)) {
            for (Card card : handSortedByKeepAsc) {
                if (card.getRank() == Rank.SMALL_JOKER) {
                    result.add(card);
                    break;
                }
            }
        }

        for (Card card : handSortedByKeepAsc) {
            if (result.size() >= 6) {
                break;
            }
            if (card.getRank() != Rank.SMALL_JOKER && card.getRank() != Rank.BIG_JOKER) {
                result.add(card);
            }
        }
        return result;
    }

    /**
     * 判断庄家是否考虑扣小王：手中必须有小王，且主牌数量达到阈值（主牌足够强，不依赖小王）。
     */
    private static boolean shouldKouWang(List<Card> hand, TrumpInfo trumpInfo) {
        boolean hasSmallJoker = false;
        int trumpCount = 0;
        for (Card card : hand) {
            if (card.getRank() == Rank.SMALL_JOKER) {
                hasSmallJoker = true;
                // 小王本身也是主牌，但判断“是否依赖小王”时应将其排除
                continue;
            }
            if (trumpInfo.isTrump(card)) {
                trumpCount++;
            }
        }
        return hasSmallJoker && trumpCount >= STRONG_TRUMP_THRESHOLD;
    }
}
