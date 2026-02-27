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
import com.tlcsdm.game.daliandagunzifx.model.Deck;
import com.tlcsdm.game.daliandagunzifx.model.PlayType;
import com.tlcsdm.game.daliandagunzifx.model.Rank;
import com.tlcsdm.game.daliandagunzifx.model.Suit;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class GameEngine {

    private final Player[] players;
    private TrumpInfo trumpInfo;
    private GamePhase phase;
    private int currentPlayerIndex;
    private int dealerIndex;
    private List<Card> kitty;
    @SuppressWarnings("unchecked")
    private final List<Card>[] currentTrickCards = new List[4];
    private PlayType currentTrickPlayType;
    private int currentTrickLeader;
    private int trickCardsPlayed;
    private int totalCardsPlayed;
    private int defenderPoints;
    private final Rank[] teamLevels;
    private int roundNumber;
    private int previousWinningTeam;
    private int previousTributeCount;

    public GameEngine(Player[] players) {
        if (players.length != 4) {
            throw new IllegalArgumentException("Exactly 4 players required");
        }
        this.players = players;
        this.teamLevels = new Rank[]{Rank.THREE, Rank.THREE};
        this.kitty = new ArrayList<>();
        this.roundNumber = 0;
        this.phase = GamePhase.ROUND_END;
        this.previousWinningTeam = -1;
        this.previousTributeCount = 0;
    }

    public void startNewRound() {
        roundNumber++;
        defenderPoints = 0;
        totalCardsPlayed = 0;
        trickCardsPlayed = 0;
        currentTrickPlayType = null;
        kitty = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            currentTrickCards[i] = null;
            players[i].getHand().clear();
        }

        Deck deck = new Deck();
        deck.shuffle();

        // Deal 39 cards to each player
        for (int i = 0; i < 4; i++) {
            players[i].addCards(deck.deal(39));
        }
        // Remaining 6 cards go to kitty
        kitty = deck.deal(6);

        phase = GamePhase.DEALING;
    }

    public boolean isTributeRequired() {
        return previousTributeCount > 0;
    }

    public void performTribute(int giverIndex, Card tributeCard, Card returnCard) {
        if (!isTributeRequired()) {
            throw new IllegalStateException("Tribute is not required");
        }
        Player giver = players[giverIndex];
        if (giver.getTeam() == previousWinningTeam) {
            throw new IllegalArgumentException("Winning team cannot give tribute");
        }
        if (!giver.hasCards(List.of(tributeCard))) {
            throw new IllegalArgumentException("Giver does not have the tribute card");
        }

        // Find a player on the winning team to receive tribute
        int receiverIndex = -1;
        for (int i = 0; i < 4; i++) {
            if (players[i].getTeam() == previousWinningTeam) {
                receiverIndex = i;
                break;
            }
        }
        Player receiver = players[receiverIndex];
        if (!receiver.hasCards(List.of(returnCard))) {
            throw new IllegalArgumentException("Receiver does not have the return card");
        }

        giver.removeCards(List.of(tributeCard));
        receiver.addCards(List.of(tributeCard));
        receiver.removeCards(List.of(returnCard));
        giver.addCards(List.of(returnCard));
    }

    public void finishTribute() {
        phase = GamePhase.DEALING;
    }

    public int getPreviousWinningTeam() {
        return previousWinningTeam;
    }

    public int getPreviousTributeCount() {
        return previousTributeCount;
    }

    public String performAutoTribute() {
        if (!isTributeRequired()) {
            return null;
        }
        int losingTeam = 1 - previousWinningTeam;
        StringBuilder messages = new StringBuilder();

        for (int t = 0; t < previousTributeCount; t++) {
            // Find the losing team player with the highest card
            Player giver = null;
            Card bestCard = null;
            for (Player p : players) {
                if (p.getTeam() == losingTeam) {
                    for (Card c : p.getHand()) {
                        if (bestCard == null || cardSortValue(c) > cardSortValue(bestCard)) {
                            bestCard = c;
                            giver = p;
                        }
                    }
                }
            }
            if (giver == null || bestCard == null) break;

            // Find a winning team player to receive
            Player receiver = null;
            for (Player p : players) {
                if (p.getTeam() == previousWinningTeam) {
                    receiver = p;
                    break;
                }
            }
            if (receiver == null) break;

            // Receiver returns their lowest non-joker card
            Card returnCard = null;
            for (Card c : receiver.getHand()) {
                if (c.getRank() != Rank.SMALL_JOKER && c.getRank() != Rank.BIG_JOKER) {
                    if (returnCard == null || cardSortValue(c) < cardSortValue(returnCard)) {
                        returnCard = c;
                    }
                }
            }
            if (returnCard == null) {
                returnCard = receiver.getHand().stream()
                    .min(Comparator.comparingInt(this::cardSortValue))
                    .orElse(null);
            }
            if (returnCard == null) break;

            giver.removeCards(List.of(bestCard));
            receiver.addCards(List.of(bestCard));
            receiver.removeCards(List.of(returnCard));
            giver.addCards(List.of(returnCard));

            if (!messages.isEmpty()) messages.append("；");
            messages.append(giver.getName()).append(" 进贡 ").append(bestCard.getDisplayName())
                .append("，").append(receiver.getName()).append(" 还 ").append(returnCard.getDisplayName());
        }

        phase = GamePhase.DEALING;
        return messages.isEmpty() ? null : messages.toString();
    }

    private int cardSortValue(Card card) {
        if (card.getRank() == Rank.BIG_JOKER) return 1000;
        if (card.getRank() == Rank.SMALL_JOKER) return 999;
        return TrumpInfo.effectiveRankStrength(card.getRank());
    }

    public void declareTrump(int playerIndex, Suit suit) {
        if (phase != GamePhase.DEALING && phase != GamePhase.DECLARING_TRUMP) {
            throw new IllegalStateException("Cannot declare trump in phase: " + phase);
        }
        dealerIndex = playerIndex;
        Rank currentLevel = teamLevels[players[playerIndex].getTeam()];
        trumpInfo = new TrumpInfo(suit, currentLevel);
        phase = GamePhase.PREPARING_KITTY;

        // Dealer picks up the kitty
        players[dealerIndex].addCards(new ArrayList<>(kitty));
        // Sort all hands
        for (Player player : players) {
            player.sortHand(trumpInfo);
        }
    }

    /**
     * Declares trump with a randomly chosen suit for the given player.
     * Used in the first round where Big Joker holders declare without choosing a suit.
     *
     * @return the randomly chosen suit
     */
    public Suit declareTrumpRandomSuit(int playerIndex) {
        Suit[] suits = Suit.values();
        Suit randomSuit = suits[java.util.concurrent.ThreadLocalRandom.current().nextInt(suits.length)];
        declareTrump(playerIndex, randomSuit);
        return randomSuit;
    }

    /**
     * Check if a player has at least one Big Joker card.
     */
    public boolean playerHasBigJoker(int playerIndex) {
        return players[playerIndex].getHand().stream()
            .anyMatch(c -> c.getRank() == Rank.BIG_JOKER);
    }

    /**
     * For the first round, when no one declares trump, determine trump from the kitty.
     * The suit with the fewest non-joker cards in the kitty becomes trump.
     * A random player is assigned as dealer.
     *
     * @return the index of the player chosen as dealer
     */
    public int declareTrumpFromKitty() {
        if (phase != GamePhase.DEALING && phase != GamePhase.DECLARING_TRUMP) {
            throw new IllegalStateException("Cannot declare trump from kitty in phase: " + phase);
        }

        // Count suits in kitty (non-joker cards only)
        java.util.Map<Suit, Integer> suitCounts = new java.util.EnumMap<>(Suit.class);
        for (Suit s : Suit.values()) {
            suitCounts.put(s, 0);
        }
        for (Card card : kitty) {
            if (card.getSuit() != null) {
                suitCounts.merge(card.getSuit(), 1, Integer::sum);
            }
        }

        // Find the suit with the minimum count (smallest suit)
        Suit minSuit = Suit.SPADE;
        int minCount = Integer.MAX_VALUE;
        for (Suit s : Suit.values()) {
            if (suitCounts.get(s) < minCount) {
                minCount = suitCounts.get(s);
                minSuit = s;
            }
        }

        // Pick a random dealer
        dealerIndex = new java.util.Random().nextInt(4);
        Rank currentLevel = teamLevels[players[dealerIndex].getTeam()];
        trumpInfo = new TrumpInfo(minSuit, currentLevel);
        phase = GamePhase.PREPARING_KITTY;

        // Dealer picks up the kitty
        players[dealerIndex].addCards(new ArrayList<>(kitty));
        // Sort all hands
        for (Player player : players) {
            player.sortHand(trumpInfo);
        }

        return dealerIndex;
    }

    public boolean isFirstRound() {
        return roundNumber == 1;
    }

    public void setKitty(List<Card> kittyCards) {
        if (phase != GamePhase.PREPARING_KITTY) {
            throw new IllegalStateException("Cannot set kitty in phase: " + phase);
        }
        if (kittyCards.size() != 6) {
            throw new IllegalArgumentException("Kitty must contain exactly 6 cards");
        }
        if (!players[dealerIndex].hasCards(kittyCards)) {
            throw new IllegalArgumentException("Dealer does not have all specified kitty cards");
        }

        players[dealerIndex].removeCards(kittyCards);
        kitty = new ArrayList<>(kittyCards);

        // Sort all hands after kitty is set
        for (Player player : players) {
            player.sortHand(trumpInfo);
        }

        // Start playing phase, dealer leads the first trick
        currentTrickLeader = dealerIndex;
        currentPlayerIndex = dealerIndex;
        phase = GamePhase.PLAYING;
    }

    public boolean isValidPlay(int playerIndex, Card card) {
        return isValidPlay(playerIndex, List.of(card));
    }

    public boolean isValidPlay(int playerIndex, List<Card> cards) {
        if (phase != GamePhase.PLAYING) {
            return false;
        }
        if (playerIndex != currentPlayerIndex) {
            return false;
        }
        if (cards == null || cards.isEmpty()) {
            return false;
        }
        if (!players[playerIndex].hasCards(cards)) {
            return false;
        }

        // Determine play type
        PlayType playType = determinePlayType(cards);
        if (playType == null) {
            return false;
        }

        // Leader can play any valid type
        if (trickCardsPlayed == 0) {
            return true;
        }

        // Follower must match the lead play type (same number of cards)
        if (currentTrickPlayType != null && playType != currentTrickPlayType) {
            // Check if player has enough cards to form the required play type
            // If not, they can play any cards of the required count
            int requiredCount = getPlayTypeCardCount(currentTrickPlayType);
            if (cards.size() != requiredCount) {
                return false;
            }
        }

        // Must follow the lead suit if possible
        List<Card> leadCards = currentTrickCards[currentTrickLeader];
        Card leadCard = leadCards.get(0);
        Suit leadSuit = trumpInfo.getEffectiveSuit(leadCard);

        // Check if all cards follow suit
        for (Card card : cards) {
            Suit cardSuit = trumpInfo.getEffectiveSuit(card);
            List<Card> suitCards = players[playerIndex].getCardsOfSuit(leadSuit, trumpInfo);
            if (!suitCards.isEmpty()) {
                if (cardSuit != leadSuit) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Determines the PlayType for a given list of cards.
     * Returns null if the cards don't form a valid play type.
     */
    public PlayType determinePlayType(List<Card> cards) {
        if (cards == null || cards.isEmpty()) {
            return null;
        }
        if (cards.size() == 1) {
            return PlayType.SINGLE;
        }
        if (cards.size() == 2) {
            Card a = cards.get(0);
            Card b = cards.get(1);
            // BANG (棒): two cards of the same rank and same suit (from different decks)
            if (a.getRank() == b.getRank()
                && java.util.Objects.equals(a.getSuit(), b.getSuit())
                && a.getId() != b.getId()) {
                return PlayType.BANG;
            }
        }
        if (cards.size() == 3) {
            Card a = cards.get(0);
            Card b = cards.get(1);
            Card c = cards.get(2);
            // GUNZI (滚子): three cards of the same rank and same suit (from different decks)
            if (a.getRank() == b.getRank() && b.getRank() == c.getRank()
                && java.util.Objects.equals(a.getSuit(), b.getSuit())
                && java.util.Objects.equals(b.getSuit(), c.getSuit())
                && a.getId() != b.getId() && b.getId() != c.getId() && a.getId() != c.getId()) {
                return PlayType.GUNZI;
            }
        }
        return null;
    }

    private int getPlayTypeCardCount(PlayType playType) {
        return switch (playType) {
            case SINGLE -> 1;
            case PAIR, BANG -> 2;
            case GUNZI -> 3;
        };
    }

    public void playCard(int playerIndex, Card card) {
        playCards(playerIndex, List.of(card));
    }

    public void playCards(int playerIndex, List<Card> cards) {
        if (!isValidPlay(playerIndex, cards)) {
            throw new IllegalArgumentException("Invalid play by player " + playerIndex);
        }

        if (trickCardsPlayed == 0) {
            currentTrickPlayType = determinePlayType(cards);
        }

        currentTrickCards[playerIndex] = new ArrayList<>(cards);
        players[playerIndex].removeCards(cards);
        trickCardsPlayed++;
        totalCardsPlayed += cards.size();

        if (trickCardsPlayed < 4) {
            currentPlayerIndex = (currentPlayerIndex + 1) % 4;
        }
    }

    public int evaluateTrick() {
        if (trickCardsPlayed != 4) {
            throw new IllegalStateException("Trick is not complete");
        }

        List<Card> leadCards = currentTrickCards[currentTrickLeader];
        Card leadCard = leadCards.get(0);
        Suit leadSuit = trumpInfo.getEffectiveSuit(leadCard);

        int winnerIndex = currentTrickLeader;
        int highestStrength = -1;

        for (int i = 0; i < 4; i++) {
            List<Card> playerCards = currentTrickCards[i];
            // Use the strongest card from each player's play for comparison
            int bestStrength = -1;
            boolean canCompete = false;
            for (Card card : playerCards) {
                Suit cardSuit = trumpInfo.getEffectiveSuit(card);
                int strength = trumpInfo.getCardStrength(card);

                boolean cardCanCompete;
                if (trumpInfo.isTrump(card)) {
                    cardCanCompete = true;
                } else if (cardSuit == leadSuit) {
                    cardCanCompete = true;
                } else {
                    cardCanCompete = false;
                }

                if (cardCanCompete && strength > bestStrength) {
                    bestStrength = strength;
                    canCompete = true;
                }
            }

            if (canCompete && bestStrength > highestStrength) {
                highestStrength = bestStrength;
                winnerIndex = i;
            }
        }

        // Calculate points in the trick
        int trickPoints = 0;
        for (int i = 0; i < 4; i++) {
            for (Card card : currentTrickCards[i]) {
                trickPoints += card.getPoints();
            }
        }

        // Total cards dealt to players = 39 * 4 = 156
        boolean isLastTrick = totalCardsPlayed >= 156;

        // Last trick: kitty points go to winner doubled
        if (isLastTrick) {
            int kittyPoints = 0;
            for (Card card : kitty) {
                kittyPoints += card.getPoints();
            }
            // Kitty points are doubled and awarded to the last trick winner
            trickPoints += kittyPoints * 2;
        }

        // Add points to defender total if winner is on defending team
        int declarerTeam = players[dealerIndex].getTeam();
        int winnerTeam = players[winnerIndex].getTeam();
        if (winnerTeam != declarerTeam) {
            defenderPoints += trickPoints;
        }

        // Prepare for next trick
        currentTrickLeader = winnerIndex;
        currentPlayerIndex = winnerIndex;
        trickCardsPlayed = 0;
        currentTrickPlayType = null;
        for (int i = 0; i < 4; i++) {
            currentTrickCards[i] = null;
        }

        if (isLastTrick) {
            phase = GamePhase.ROUND_END;
        }

        return winnerIndex;
    }

    private boolean allHandsEmpty() {
        for (Player player : players) {
            if (!player.getHand().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public boolean isRoundOver() {
        return phase == GamePhase.ROUND_END;
    }

    public RoundResult calculateRoundResult() {
        if (phase != GamePhase.ROUND_END) {
            throw new IllegalStateException("Round is not over");
        }
        int declarerTeam = players[dealerIndex].getTeam();
        int bloods = getKittyBloods();
        RoundResult result = new RoundResult(defenderPoints, declarerTeam, bloods);
        previousWinningTeam = result.getWinningTeam();
        previousTributeCount = result.getTributeCount();
        return result;
    }

    public int getKittyBloods() {
        int bloods = 0;
        for (Card card : kitty) {
            if (card.getRank() == Rank.BIG_JOKER) bloods += 2;
            else if (card.getRank() == Rank.SMALL_JOKER) bloods += 1;
        }
        return bloods;
    }

    public int getCurrentPlayerIndex() {
        return currentPlayerIndex;
    }

    public GamePhase getPhase() {
        return phase;
    }

    public Player[] getPlayers() {
        return players;
    }

    public TrumpInfo getTrumpInfo() {
        return trumpInfo;
    }

    public int getDealerIndex() {
        return dealerIndex;
    }

    public List<Card> getKitty() {
        return kitty;
    }

    /**
     * Returns the current trick as a Card array for backward compatibility.
     * Each element is the first (or only) card played by that player, or null.
     */
    public Card[] getCurrentTrick() {
        Card[] result = new Card[4];
        for (int i = 0; i < 4; i++) {
            if (currentTrickCards[i] != null && !currentTrickCards[i].isEmpty()) {
                result[i] = currentTrickCards[i].get(0);
            }
        }
        return result;
    }

    /**
     * Returns the full list of cards played by each player in the current trick.
     */
    public List<Card>[] getCurrentTrickCards() {
        return currentTrickCards;
    }

    public PlayType getCurrentTrickPlayType() {
        return currentTrickPlayType;
    }

    public int getCurrentTrickLeader() {
        return currentTrickLeader;
    }

    public int getTrickCardsPlayed() {
        return trickCardsPlayed;
    }

    public int getDefenderPoints() {
        return defenderPoints;
    }

    public Rank[] getTeamLevels() {
        return teamLevels;
    }

    public int getRoundNumber() {
        return roundNumber;
    }
}
