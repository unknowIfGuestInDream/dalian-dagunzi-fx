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

package com.tlcsdm.game.daliandagunzifx.ai;

import com.tlcsdm.game.daliandagunzifx.engine.GameEngine;
import com.tlcsdm.game.daliandagunzifx.engine.Player;
import com.tlcsdm.game.daliandagunzifx.engine.TrumpInfo;
import com.tlcsdm.game.daliandagunzifx.model.Card;
import com.tlcsdm.game.daliandagunzifx.model.PlayType;
import com.tlcsdm.game.daliandagunzifx.model.Rank;
import com.tlcsdm.game.daliandagunzifx.model.Suit;

import java.util.*;
import java.util.stream.Collectors;

public class EasyAI implements AIStrategy {

    private final Random random = new Random();

    @Override
    public Suit chooseTrumpSuit(Player player, Rank trumpRank) {
        Map<Suit, Integer> counts = new EnumMap<>(Suit.class);
        for (Card card : player.getHand()) {
            if (card.getRank() == trumpRank && card.getSuit() != null) {
                counts.merge(card.getSuit(), 1, Integer::sum);
            }
        }
        List<Suit> candidates = counts.entrySet().stream()
            .filter(e -> e.getValue() >= 2)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
        if (candidates.isEmpty()) {
            return null;
        }
        return candidates.get(random.nextInt(candidates.size()));
    }

    @Override
    public List<Card> chooseKittyCards(Player player, List<Card> kitty, TrumpInfo trumpInfo) {
        List<Card> hand = new ArrayList<>(player.getHand());
        // Prefer discarding non-trump, non-point cards
        List<Card> candidates = hand.stream()
            .filter(c -> !trumpInfo.isTrump(c) && c.getPoints() == 0
                && c.getRank() != Rank.SMALL_JOKER && c.getRank() != Rank.BIG_JOKER)
            .collect(Collectors.toList());
        Collections.shuffle(candidates, random);

        List<Card> result = new ArrayList<>();
        for (Card card : candidates) {
            if (result.size() >= 10) break;
            result.add(card);
        }
        // If not enough, add remaining cards (excluding jokers)
        if (result.size() < 10) {
            List<Card> remaining = hand.stream()
                .filter(c -> !result.contains(c)
                    && c.getRank() != Rank.SMALL_JOKER && c.getRank() != Rank.BIG_JOKER)
                .collect(Collectors.toList());
            Collections.shuffle(remaining, random);
            for (Card card : remaining) {
                if (result.size() >= 10) break;
                result.add(card);
            }
        }
        return result;
    }

    @Override
    public Card chooseCard(Player player, GameEngine engine) {
        List<Card> validCards = getValidCards(player, engine);
        return validCards.get(random.nextInt(validCards.size()));
    }

    @Override
    public List<Card> chooseCards(Player player, GameEngine engine) {
        PlayType trickType = engine.getCurrentTrickPlayType();
        if (trickType == null) {
            // Leader: play a single card
            return List.of(chooseCard(player, engine));
        }
        int requiredCount = switch (trickType) {
            case SINGLE -> 1;
            case PAIR, BANG -> 2;
        };
        if (requiredCount == 1) {
            return List.of(chooseCard(player, engine));
        }
        // Need to play a pair - find valid cards of the lead suit
        List<Card> validCards = getValidCards(player, engine);
        List<Card> result = new ArrayList<>();
        for (Card card : validCards) {
            result.add(card);
            if (result.size() >= requiredCount) break;
        }
        return result;
    }

    protected List<Card> getValidCards(Player player, GameEngine engine) {
        List<Card> valid = new ArrayList<>();
        for (Card card : player.getHand()) {
            if (engine.isValidPlay(player.getId(), card)) {
                valid.add(card);
            }
        }
        return valid;
    }
}
