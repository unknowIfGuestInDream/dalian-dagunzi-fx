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

public class RoundResult {

    private final int defenderPoints;
    private final int declarerTeam;
    private final int defenderTeam;
    private final boolean declarerWins;
    private final int levelChange;
    private final int winningTeam;

    public RoundResult(int defenderPoints, int declarerTeam) {
        this.defenderPoints = defenderPoints;
        this.declarerTeam = declarerTeam;
        this.defenderTeam = 1 - declarerTeam;
        this.declarerWins = defenderPoints < 80;

        if (defenderPoints == 0) {
            this.levelChange = 3;
            this.winningTeam = declarerTeam;
        } else if (defenderPoints < 40) {
            this.levelChange = 2;
            this.winningTeam = declarerTeam;
        } else if (defenderPoints < 80) {
            this.levelChange = 1;
            this.winningTeam = declarerTeam;
        } else if (defenderPoints == 80) {
            this.levelChange = 0;
            this.winningTeam = -1;
        } else if (defenderPoints < 120) {
            this.levelChange = 1;
            this.winningTeam = defenderTeam;
        } else if (defenderPoints < 160) {
            this.levelChange = 2;
            this.winningTeam = defenderTeam;
        } else {
            this.levelChange = 3;
            this.winningTeam = defenderTeam;
        }
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
}
