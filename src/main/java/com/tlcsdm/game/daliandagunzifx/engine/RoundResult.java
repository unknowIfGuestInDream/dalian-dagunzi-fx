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

public class RoundResult {

    private final int defenderPoints;
    private final int declarerTeam;
    private final int defenderTeam;
    private final boolean declarerWins;
    private final int levelChange;
    private final int winningTeam;
    private final int tributeCount;
    private final int kittyBloods;
    private final boolean lastTrickCapturedByDefender;

    public RoundResult(int defenderPoints, int declarerTeam) {
        this(defenderPoints, declarerTeam, 0, false);
    }

    public RoundResult(int defenderPoints, int declarerTeam, int kittyBloods) {
        this(defenderPoints, declarerTeam, kittyBloods, false);
    }

    public RoundResult(int defenderPoints, int declarerTeam, int kittyBloods, boolean lastTrickCapturedByDefender) {
        this.defenderPoints = defenderPoints;
        this.declarerTeam = declarerTeam;
        this.defenderTeam = 1 - declarerTeam;
        this.declarerWins = defenderPoints < 120;
        this.kittyBloods = kittyBloods;
        this.lastTrickCapturedByDefender = lastTrickCapturedByDefender;

        if (defenderPoints < 120) {
            // 庄家赢：基础1级 + 扣王加成（扣王成功，级数叠加给胜利方）
            this.levelChange = 1 + kittyBloods;
            this.winningTeam = declarerTeam;
        } else {
            // 闲家赢：基础1级 + 抠底额外1级（扣王失败，不加级）
            this.levelChange = 1 + (lastTrickCapturedByDefender ? 1 : 0);
            this.winningTeam = defenderTeam;
        }

        // 进贡计算：分数惩罚 + 扣王血（无论扣王成功或失败，血都计入进贡）
        int scoreTribute = 0;
        if (defenderPoints < 80) {
            scoreTribute = (80 - defenderPoints) / 10;
        } else if (defenderPoints > 150) {
            scoreTribute = (defenderPoints - 150) / 10;
        }
        this.tributeCount = scoreTribute + kittyBloods;
    }

    public int getDefenderPoints() {
        return defenderPoints;
    }

    public int getDeclarerTeam() {
        return declarerTeam;
    }

    public int getDefenderTeam() {
        return defenderTeam;
    }

    public boolean isDeclarerWins() {
        return declarerWins;
    }

    public int getLevelChange() {
        return levelChange;
    }

    public int getWinningTeam() {
        return winningTeam;
    }

    public int getTributeCount() {
        return tributeCount;
    }

    public int getKittyBloods() {
        return kittyBloods;
    }

    public boolean isLastTrickCapturedByDefender() {
        return lastTrickCapturedByDefender;
    }
}
