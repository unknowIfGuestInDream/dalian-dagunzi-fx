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
package com.tlcsdm.game.daliandagunzifx.screen;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.tlcsdm.game.daliandagunzifx.DaGunZiGame;
import com.tlcsdm.game.daliandagunzifx.ai.AIStrategy;
import com.tlcsdm.game.daliandagunzifx.ai.EasyAI;
import com.tlcsdm.game.daliandagunzifx.ai.HardAI;
import com.tlcsdm.game.daliandagunzifx.ai.MediumAI;
import com.tlcsdm.game.daliandagunzifx.config.AppConfig;
import com.tlcsdm.game.daliandagunzifx.engine.GameEngine;
import com.tlcsdm.game.daliandagunzifx.engine.GamePhase;
import com.tlcsdm.game.daliandagunzifx.engine.Player;
import com.tlcsdm.game.daliandagunzifx.engine.RoundResult;
import com.tlcsdm.game.daliandagunzifx.engine.TrumpInfo;
import com.tlcsdm.game.daliandagunzifx.model.Card;
import com.tlcsdm.game.daliandagunzifx.model.Rank;
import com.tlcsdm.game.daliandagunzifx.model.Suit;
import com.tlcsdm.game.daliandagunzifx.tracker.CardTracker;
import com.tlcsdm.game.daliandagunzifx.ui.Assets;
import com.tlcsdm.game.daliandagunzifx.ui.BaseStageScreen;
import com.tlcsdm.game.daliandagunzifx.ui.CardActor;
import com.tlcsdm.game.daliandagunzifx.ui.Dialogs;
import com.tlcsdm.game.daliandagunzifx.ui.UiUtils;

/**
 * 游戏主界面：复刻完整的大连打滚子对局流程（发牌、叫主、进贡、埋底、出牌、计分、升级），
 * 以 Scene2D 绝对定位渲染牌桌、手牌、出牌区、信息面板与记牌器。
 *
 * @author unknowIfGuestInDream
 */
public class GameScreen extends BaseStageScreen {

    private static final float CARD_W = CardActor.CARD_WIDTH;
    private static final float CARD_H = CardActor.CARD_HEIGHT;
    private static final float OVERLAP = 25f;
    private static final float AI_BIDDING_DELAY = 0.5f;

    private Player[] players;
    private CardTracker cardTracker;
    private AIStrategy aiStrategy;
    private GameEngine engine;
    private final AppConfig config = AppConfig.getInstance();

    // 顶层容器
    private final Group centerGroup = new Group();
    private final Group trickGroup = new Group();
    private final Group handGroup = new Group();

    // 文本标签
    private Label statusLabel;
    private Label roundLabel;
    private Label trumpLabel;
    private Label dealerLabel;
    private Label scoreLabel;
    private Label tributeLabel;
    private Label teamLevelLabel;
    private final Label[] playerNameLabels = new Label[4];

    private Table infoPanel;
    private Table trackerTable;
    private Table toolbar;
    private Table actionTable;
    private TextButton primaryBtn;

    // 交互状态
    private final List<Card> selectedKittyCards = new ArrayList<>();
    // 本局最初发到的 6 张底牌（埋底前的原始底牌），用于"查看底牌"规则
    private final List<Card> originalKitty = new ArrayList<>();
    private final List<Card> selectedPlayCards = new ArrayList<>();
    private boolean waitingForHumanPlay;
    private boolean waitingForKitty;
    private boolean waitingForTributeReturn;
    private int currentHighlightIdx = -1;

    // 进贡流程临时状态
    private final List<Integer> pendingTributeGivers = new ArrayList<>();
    private final List<Card> pendingTributeGiveCards = new ArrayList<>();
    private final List<Card> tributeDisplayCards = new ArrayList<>();
    private int tributeReturnCount;
    private StringBuilder tributeMessages;
    private Runnable tributeOnComplete;

    // 中央展示内容（用于窗口尺寸变化时重绘）
    private String centerTitle;
    private List<Card> centerCards;
    private boolean centerFaceUp;

    private float viewW;
    private float viewH;

    public GameScreen(DaGunZiGame game) {
        super(game);
        buildUi();
        startGame();
    }

    // ======================== 界面搭建 ========================

    private void buildUi() {
        stage.addActor(centerGroup);
        stage.addActor(trickGroup);
        stage.addActor(handGroup);

        statusLabel = new Label("", assets.getLabelStyle());
        statusLabel.setColor(Assets.STATUS);
        statusLabel.setAlignment(Align.center);
        stage.addActor(statusLabel);

        buildInfoPanel();
        buildTracker();
        buildToolbar();

        actionTable = new Table();
        stage.addActor(actionTable);

        for (int i = 0; i < 4; i++) {
            Label l = new Label("", assets.getLabelStyle());
            playerNameLabels[i] = l;
            stage.addActor(l);
        }
    }

    private void buildInfoPanel() {
        infoPanel = new Table();
        infoPanel.setBackground(assets.drawable(Assets.PANEL_BG));
        infoPanel.pad(10);
        infoPanel.defaults().left().padBottom(2);
        roundLabel = smallLabel();
        trumpLabel = smallLabel();
        dealerLabel = smallLabel();
        scoreLabel = smallLabel();
        tributeLabel = smallLabel();
        teamLevelLabel = smallLabel();
        infoPanel.add(roundLabel).row();
        infoPanel.add(trumpLabel).row();
        infoPanel.add(dealerLabel).row();
        infoPanel.add(scoreLabel).row();
        infoPanel.add(tributeLabel).row();
        infoPanel.add(teamLevelLabel).row();
        stage.addActor(infoPanel);
    }

    private void buildTracker() {
        trackerTable = new Table();
        trackerTable.setBackground(assets.drawable(Assets.PANEL_BG));
        trackerTable.pad(6);
        stage.addActor(trackerTable);
    }

    private void buildToolbar() {
        toolbar = new Table();
        toolbar.defaults().padRight(8);
        toolbar.add(UiUtils.button("规则", assets.getButtonStyle(),
            () -> game.setScreen(new RulesScreen(game, this)))).width(90).height(40);
        toolbar.add(UiUtils.button("看底牌", assets.getButtonStyle(), this::viewKittyByRule))
            .width(90).height(40);
        toolbar.add(UiUtils.button("重开本局", assets.getButtonStyle(),
            () -> Dialogs.showConfirm(stage, assets, "重开本局", "确定重新开始本局吗？", this::restartCurrentRound)))
            .width(110).height(40);
        toolbar.add(UiUtils.button("返回菜单", assets.getButtonStyle(),
            () -> Dialogs.showConfirm(stage, assets, "返回菜单", "确定返回主菜单吗？当前进度将丢失。",
                () -> game.switchScreen(new MenuScreen(game))))).width(110).height(40);
        stage.addActor(toolbar);
    }

    private Label smallLabel() {
        return new Label("", assets.getLabelStyleSmall());
    }

    // ======================== 布局 ========================

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        doLayout(width, height);
    }

    private void doLayout(float w, float h) {
        viewW = w;
        viewH = h;
        centerGroup.setBounds(0, 0, w, h);
        trickGroup.setBounds(0, 0, w, h);
        handGroup.setBounds(0, 0, w, h);

        statusLabel.setWidth(w);
        statusLabel.setPosition(0, h - 28);

        infoPanel.pack();
        infoPanel.setPosition(w - infoPanel.getWidth() - 12, h - infoPanel.getHeight() - 12);

        trackerTable.pack();
        trackerTable.setPosition(w - trackerTable.getWidth() - 12,
            infoPanel.getY() - trackerTable.getHeight() - 10);

        toolbar.pack();
        toolbar.setPosition(12, h - toolbar.getHeight() - 12);

        // 玩家名称位置：下(你)/左/上(搭档)/右；顶部搭档下移，避免与顶部状态栏重叠
        playerNameLabels[0].setPosition(20, 132);
        playerNameLabels[1].setPosition(20, h / 2f);
        playerNameLabels[2].setPosition(w / 2f - 40, h - 96);
        playerNameLabels[3].setPosition(w - 150, h / 2f);

        layoutActionTable();
        refreshDynamic();
    }

    private void layoutActionTable() {
        actionTable.pack();
        actionTable.setPosition((viewW - actionTable.getWidth()) / 2f, 134);
    }

    private void refreshDynamic() {
        updateHumanHand();
        renderTrick(-1);
        renderCenter();
        updateInfoPanel();
        updatePlayersPanel();
        updateTrackerPanel();
    }

    // ======================== 对局初始化 ========================

    private void startGame() {
        players = new Player[]{
            new Player(0, "你", true),
            new Player(1, "小明", false),
            new Player(2, "搭档小红", false),
            new Player(3, "小刚", false)
        };
        cardTracker = new CardTracker();
        initAIStrategy();
        engine = new GameEngine(players);
        engine.setLiveBang(config.isLiveBang());
        startNewRound();
    }

    private void initAIStrategy() {
        aiStrategy = switch (config.getAiLevel()) {
            case EASY -> new EasyAI();
            case MEDIUM -> new MediumAI(cardTracker);
            case HARD -> new HardAI(cardTracker);
        };
        aiStrategy.setAggressive(config.isAggressive());
    }

    private void restartCurrentRound() {
        Rank[] cur = engine.getTeamLevels();
        Rank l0 = cur[0];
        Rank l1 = cur[1];
        cardTracker.reset();
        initAIStrategy();
        engine = new GameEngine(players);
        engine.setLiveBang(config.isLiveBang());
        Rank[] nl = engine.getTeamLevels();
        nl[0] = l0;
        nl[1] = l1;
        startNewRound();
    }

    private void startNewRound() {
        cardTracker.reset();
        selectedKittyCards.clear();
        originalKitty.clear();
        selectedPlayCards.clear();
        waitingForHumanPlay = false;
        waitingForKitty = false;
        waitingForTributeReturn = false;
        currentHighlightIdx = -1;

        engine.startNewRound();

        clearCenter();
        trickGroup.clearChildren();
        actionTable.clearChildren();
        primaryBtn = null;
        updateInfoPanel();
        updatePlayersPanel();

        animateDealing(this::beginTrumpDeclaration);
    }

    // ======================== 发牌 ========================

    private void animateDealing(Runnable onComplete) {
        setStatus("发牌中...");
        showCenterBanner("发牌中", Assets.STATUS);
        float interval = engine.isFirstRound() ? 0.16f : 0.08f;
        updateHumanHand(interval);
        int n = players[0].getHand().size();
        schedule(interval * n + 0.3f, () -> {
            updateHumanHand();
            onComplete.run();
        });
    }

    // ======================== 进贡 / 回贡 ========================

    private void handleTributeFlow(Runnable onComplete) {
        tributeMessages = new StringBuilder();
        tributeOnComplete = onComplete;
        pendingTributeGivers.clear();
        pendingTributeGiveCards.clear();
        tributeDisplayCards.clear();

        int total = engine.getPreviousTributeCount();
        List<int[]> gives = new ArrayList<>();
        List<Card> giveCards = new ArrayList<>();
        for (int i = 0; i < total; i++) {
            int[] info = engine.findNextTributeGiverInfo();
            if (info == null) {
                break;
            }
            int giverIdx = info[0];
            int receiverIdx = info[1];
            Card tributeCard = engine.getTributeCard(giverIdx);
            if (tributeCard == null) {
                break;
            }
            engine.executeTributeGive(giverIdx, tributeCard, receiverIdx);
            gives.add(new int[]{giverIdx, receiverIdx});
            giveCards.add(tributeCard);
        }

        if (gives.isEmpty()) {
            finishTributeFlow();
            return;
        }

        tributeDisplayCards.addAll(giveCards);
        displayCenterCards("进贡", tributeDisplayCards, true);
        updateHumanHand();

        boolean humanReceives = gives.get(0)[1] == 0;
        if (humanReceives) {
            for (int[] g : gives) {
                pendingTributeGivers.add(g[0]);
            }
            pendingTributeGiveCards.addAll(giveCards);
            tributeReturnCount = gives.size();

            selectedPlayCards.clear();
            waitingForTributeReturn = true;
            updateHumanHand();

            StringBuilder giverInfo = new StringBuilder();
            for (int i = 0; i < gives.size(); i++) {
                if (giverInfo.length() > 0) {
                    giverInfo.append("、");
                }
                giverInfo.append(players[gives.get(i)[0]].getName())
                    .append(" 进贡 ").append(cardName(giveCards.get(i)));
            }
            setStatus(giverInfo + "，请选择 " + tributeReturnCount + " 张牌回贡");

            actionTable.clearChildren();
            primaryBtn = UiUtils.button("确认回贡", assets.getButtonStyle(), this::confirmTributeReturn);
            primaryBtn.setDisabled(true);
            actionTable.add(primaryBtn).width(140).height(46);
            layoutActionTable();
        } else {
            for (int i = 0; i < gives.size(); i++) {
                int giverIdx = gives.get(i)[0];
                int receiverIdx = gives.get(i)[1];
                Card tributeCard = giveCards.get(i);
                Card returnCard = engine.autoSelectReturnCard(receiverIdx);
                if (returnCard != null) {
                    engine.executeTributeReturn(receiverIdx, returnCard, giverIdx);
                    appendTributeMessage(giverIdx, tributeCard, receiverIdx, returnCard);
                }
            }
            updateHumanHand();
            finishTributeFlow();
        }
    }

    private void appendTributeMessage(int giverIdx, Card tributeCard, int receiverIdx, Card returnCard) {
        if (tributeMessages.length() > 0) {
            tributeMessages.append("；");
        }
        tributeMessages.append(players[giverIdx].getName()).append(" 进贡 ")
            .append(cardName(tributeCard)).append("，")
            .append(players[receiverIdx].getName()).append(" 还 ")
            .append(cardName(returnCard));
    }

    private void handleTributeReturnCardClick(Card card) {
        if (!waitingForTributeReturn) {
            return;
        }
        if (selectedPlayCards.contains(card)) {
            selectedPlayCards.remove(card);
        } else if (selectedPlayCards.size() < tributeReturnCount) {
            selectedPlayCards.add(card);
        }
        updateHumanHand();
        if (primaryBtn != null) {
            primaryBtn.setDisabled(selectedPlayCards.size() != tributeReturnCount);
        }
    }

    private void confirmTributeReturn() {
        if (!waitingForTributeReturn || selectedPlayCards.size() != tributeReturnCount) {
            return;
        }
        List<Card> returnCards = new ArrayList<>(selectedPlayCards);
        waitingForTributeReturn = false;
        selectedPlayCards.clear();
        actionTable.clearChildren();
        primaryBtn = null;

        int returnCount = Math.min(returnCards.size(),
            Math.min(pendingTributeGivers.size(), pendingTributeGiveCards.size()));
        for (int i = 0; i < returnCount; i++) {
            int giverIdx = pendingTributeGivers.get(i);
            Card tributeCard = pendingTributeGiveCards.get(i);
            Card returnCard = returnCards.get(i);
            engine.executeTributeReturn(0, returnCard, giverIdx);
            appendTributeMessage(giverIdx, tributeCard, 0, returnCard);
        }
        updateHumanHand();
        finishTributeFlow();
    }

    private void finishTributeFlow() {
        engine.finishTribute();
        updateHumanHand();
        if (tributeMessages != null && tributeMessages.length() > 0) {
            setStatus("进贡：" + tributeMessages);
            if (!tributeDisplayCards.isEmpty()) {
                displayCenterCards("进贡", tributeDisplayCards, true);
            }
        }
        tributeOnComplete.run();
    }

    // ======================== 叫主 ========================

    private void afterTrumpDeclared(int dealerIdx) {
        // 记录埋底前最初发到的 6 张底牌，供"查看底牌"规则在出牌阶段展示给没干锅的玩家
        originalKitty.clear();
        if (engine.getKitty() != null) {
            originalKitty.addAll(engine.getKitty());
        }
        displayCenterCards("底牌", engine.getKitty(), true);
        TrumpInfo trumpInfo = engine.getTrumpInfo();
        if (trumpInfo != null && trumpInfo.getTrumpSuit() != null) {
            showCenterBanner("亮主 " + trumpInfo.getTrumpSuit().getDisplayName()
                + trumpInfo.getTrumpRank().getDisplayName(), Assets.GOLD);
        }
        updateInfoPanel();
        updatePlayersPanel();
        if (engine.isTributeRequired()) {
            handleTributeFlow(() -> proceedToKittySelection(dealerIdx));
        } else {
            proceedToKittySelection(dealerIdx);
        }
    }

    private void proceedToKittySelection(int dealerIdx) {
        if (dealerIdx == 0) {
            beginKittySelection();
        } else {
            handleAIKitty(dealerIdx);
        }
    }

    private void beginTrumpDeclaration() {
        updateHumanHand();
        boolean isFirstRound = engine.isFirstRound();
        boolean humanHasBigJoker = engine.playerHasBigJoker(0);

        if (isFirstRound && !humanHasBigJoker) {
            setStatus("第一局需要亮大王定庄，你没有大王，电脑正在考虑...");
            actionTable.clearChildren();
            humanPassTrump();
            return;
        }

        if (isFirstRound) {
            setStatus("第一局：你有大王，可亮王定庄（主牌花色将随机分配），或选择不叫");
            actionTable.clearChildren();
            actionTable.add(UiUtils.button("亮王定庄", assets.getDangerButtonStyle(), () -> {
                actionTable.clearChildren();
                Suit randomSuit = engine.declareTrumpRandomSuit(0);
                setStatus("你亮王定庄，主牌：" + randomSuit.getDisplayName());
                updateInfoPanel();
                updateHumanHand();
                afterTrumpDeclared(engine.getDealerIndex());
            })).width(130).height(44).padRight(8);
            actionTable.add(UiUtils.button("不叫", assets.getButtonStyle(), this::humanPassTrump))
                .width(100).height(44);
            layoutActionTable();
            return;
        }

        Rank humanTrumpRank = engine.getTeamLevels()[players[0].getTeam()];
        int minRequired = engine.getDeclaredTrumpCount() == 0 ? 1 : engine.getDeclaredTrumpCount() + 1;
        Map<Suit, Integer> suitCounts = new EnumMap<>(Suit.class);
        for (Card card : players[0].getHand()) {
            if (card.getRank() == humanTrumpRank && card.getSuit() != null) {
                suitCounts.merge(card.getSuit(), 1, Integer::sum);
            }
        }
        boolean humanCanDeclare = suitCounts.values().stream().anyMatch(c -> c >= minRequired);

        if (humanCanDeclare) {
            String reqDesc = minRequired == 1 ? "1张即可叫主" : "需" + minRequired + "张同花色才能抢主";
            setStatus("你可以叫主（" + reqDesc + "），请选择主牌花色，或选择不叫");
            actionTable.clearChildren();
            for (Suit suit : Suit.values()) {
                int count = suitCounts.getOrDefault(suit, 0);
                final Suit s = suit;
                final int cardCount = count;
                TextButton btn = new TextButton("叫" + suit.getDisplayName(), assets.getButtonStyle());
                UiUtils.onClick(btn, () -> {
                    if (btn.isDisabled()) {
                        return;
                    }
                    humanDeclareTrump(s, cardCount);
                });
                btn.setDisabled(count < minRequired);
                actionTable.add(btn).width(110).height(44).padRight(6);
            }
            actionTable.add(UiUtils.button("不叫", assets.getButtonStyle(), () -> {
                actionTable.clearChildren();
                setStatus("你选择了不叫，电脑正在考虑...");
                aiTrumpBidding();
            })).width(90).height(44);
            layoutActionTable();
        } else {
            aiTrumpBidding();
        }
    }

    private void aiTrumpBidding() {
        int start = engine.getNextDealerIndex();
        if (start < 0) {
            start = 0;
        }
        actionTable.clearChildren();

        int step = 0;
        for (int k = 0; k < 4; k++) {
            final int idx = (start + k) % 4;
            if (idx == 0) {
                continue;
            }
            step++;
            schedule(AI_BIDDING_DELAY * step, () -> {
                int curCount = engine.getDeclaredTrumpCount();
                int minReq = curCount == 0 ? 1 : curCount + 1;
                Player ai = players[idx];
                Rank rank = engine.getTeamLevels()[ai.getTeam()];
                Suit chosenSuit = aiStrategy.chooseTrumpSuit(ai, rank, minReq);
                if (chosenSuit != null) {
                    int aiCount = countTrumpRankCards(ai, rank, chosenSuit);
                    engine.tentativeDeclareTrump(idx, chosenSuit, aiCount);
                    setStatus(ai.getName() + " 叫了主牌：" + chosenSuit.getDisplayName()
                        + "（" + aiCount + "张）");
                    updateInfoPanel();
                    updateHumanHand();
                } else {
                    setStatus(ai.getName() + " 不叫");
                }
            });
        }

        final int designatedDealer = start;
        schedule(AI_BIDDING_DELAY * (step + 1), () -> {
            if (engine.getDeclaredTrumpCount() > 0) {
                engine.finalizeTrumpDeclaration();
                afterTrumpDeclared(engine.getDealerIndex());
            } else {
                declareTrumpFromKittyForDealer(designatedDealer);
            }
        });
    }

    private int countTrumpRankCards(Player player, Rank rank, Suit suit) {
        int count = 0;
        for (Card c : player.getHand()) {
            if (c.getRank() == rank && c.getSuit() == suit) {
                count++;
            }
        }
        return count;
    }

    private void humanDeclareTrump(Suit suit, int count) {
        actionTable.clearChildren();
        engine.tentativeDeclareTrump(0, suit, count);
        setStatus("你叫了主牌：" + suit.getDisplayName() + "（" + count + "张）");
        updateInfoPanel();
        updateHumanHand();
        aiTrumpBidding();
    }

    private void humanPassTrump() {
        actionTable.clearChildren();
        setStatus("你选择了不叫，电脑正在考虑...");
        boolean[] declared = {false};

        for (int i = 1; i <= 3; i++) {
            final int idx = i;
            schedule(0.5f * i, () -> {
                if (declared[0]) {
                    return;
                }
                Player p = players[idx];
                if (!engine.playerHasBigJoker(idx)) {
                    setStatus(p.getName() + " 没有大王，不叫");
                    return;
                }
                declared[0] = true;
                Suit randomSuit = engine.declareTrumpRandomSuit(idx);
                setStatus(p.getName() + " 亮王定庄，随机主牌："
                    + randomSuit.getDisplayName());
                updateInfoPanel();
                updateHumanHand();
                afterTrumpDeclared(idx);
            });
        }

        schedule(2.0f, () -> {
            if (!declared[0]) {
                declareTrumpFromKittyWithDelay();
            }
        });
    }

    private void declareTrumpFromKittyWithDelay() {
        setStatus("无人叫牌，从底牌确定主牌花色...");
        schedule(1.0f, () -> {
            int newDealer = engine.declareTrumpFromKitty();
            TrumpInfo ti = engine.getTrumpInfo();
            setStatus("底牌确定主牌：" + ti.getTrumpSuit().getDisplayName()
                + "，庄家：" + players[newDealer].getName());
            updateInfoPanel();
            updateHumanHand();
            afterTrumpDeclared(newDealer);
        });
    }

    private void declareTrumpFromKittyForDealer(int dealerIdx) {
        setStatus("庄家无法叫主，从底牌确定主牌花色...");
        actionTable.clearChildren();
        schedule(1.0f, () -> {
            int newDealer = engine.declareTrumpFromKittyForDealer(dealerIdx);
            TrumpInfo ti = engine.getTrumpInfo();
            setStatus("底牌确定主牌：" + ti.getTrumpSuit().getDisplayName()
                + "，庄家：" + players[newDealer].getName());
            updateInfoPanel();
            updateHumanHand();
            afterTrumpDeclared(newDealer);
        });
    }

    // ======================== 埋底 / 扣王 ========================

    private void beginKittySelection() {
        waitingForKitty = true;
        selectedKittyCards.clear();
        setStatus("请选择6张牌作为底牌（已选：0/6）（扣王：大王=2血，小王=1血）");

        actionTable.clearChildren();
        primaryBtn = UiUtils.button("确认", assets.getButtonStyle(), this::confirmKitty);
        primaryBtn.setDisabled(true);
        actionTable.add(primaryBtn).width(130).height(46);
        layoutActionTable();

        updateHumanHand();
    }

    private void toggleKittyCard(Card card) {
        if (selectedKittyCards.contains(card)) {
            selectedKittyCards.remove(card);
        } else if (selectedKittyCards.size() < 6) {
            selectedKittyCards.add(card);
        }
        setStatus("请选择6张牌作为底牌（已选：" + selectedKittyCards.size()
            + "/6）（扣王：大王=2血，小王=1血）");
        if (primaryBtn != null) {
            primaryBtn.setDisabled(selectedKittyCards.size() != 6);
        }
        updateHumanHand();
    }

    private void confirmKitty() {
        if (!waitingForKitty || selectedKittyCards.size() != 6) {
            return;
        }
        waitingForKitty = false;
        actionTable.clearChildren();
        primaryBtn = null;
        engine.setKitty(new ArrayList<>(selectedKittyCards));
        selectedKittyCards.clear();
        setStatus("底牌已确认，游戏开始！");
        updateHumanHand();
        updateInfoPanel();
        displayBuriedKitty();
        schedule(0.5f, this::processCurrentPlayer);
    }

    private void handleAIKitty(int dealerIndex) {
        schedule(0.8f, () -> {
            Player dealer = players[dealerIndex];
            List<Card> kittyCards = aiStrategy.chooseKittyCards(dealer, engine.getKitty(), engine.getTrumpInfo());
            engine.setKitty(kittyCards);
            updateHumanHand();
            displayBuriedKitty();
            setStatus("游戏开始！");
            schedule(0.5f, this::processCurrentPlayer);
        });
    }

    // ======================== 出牌 ========================

    private void processCurrentPlayer() {
        if (engine.getPhase() != GamePhase.PLAYING) {
            return;
        }
        int currentIdx = engine.getCurrentPlayerIndex();
        Player currentPlayer = players[currentIdx];
        currentHighlightIdx = currentIdx;
        updatePlayersPanel();

        if (currentPlayer.isHuman()) {
            waitingForHumanPlay = true;
            selectedPlayCards.clear();
            showPlayButton();
            updateHumanHand();
            setStatus("轮到你出牌（选中牌后点击出牌按钮或右击出牌）");
        } else {
            waitingForHumanPlay = false;
            updateHumanHand();
            setStatus(currentPlayer.getName() + " 思考中...");
            schedule(0.5f, () -> {
                List<Card> cards = aiStrategy.chooseCards(currentPlayer, engine);
                executePlayMulti(currentIdx, cards);
            });
        }
    }

    private void handleHumanCardClick(Card card) {
        if (!waitingForHumanPlay) {
            return;
        }
        if (selectedPlayCards.contains(card)) {
            selectedPlayCards.remove(card);
        } else {
            selectedPlayCards.add(card);
        }
        updateHumanHand();
        updatePlayButtonState();
    }

    private void showPlayButton() {
        actionTable.clearChildren();
        primaryBtn = UiUtils.button("出牌", assets.getButtonStyle(), this::confirmPlay);
        primaryBtn.setDisabled(true);
        actionTable.add(primaryBtn).width(130).height(46);
        layoutActionTable();
    }

    private void updatePlayButtonState() {
        if (primaryBtn == null) {
            return;
        }
        boolean valid = !selectedPlayCards.isEmpty() && engine.isValidPlay(0, selectedPlayCards);
        primaryBtn.setDisabled(!valid);
    }

    private void confirmPlay() {
        if (!waitingForHumanPlay || selectedPlayCards.isEmpty()) {
            return;
        }
        if (!engine.isValidPlay(0, selectedPlayCards)) {
            return;
        }
        waitingForHumanPlay = false;
        actionTable.clearChildren();
        primaryBtn = null;
        List<Card> cardsToPlay = new ArrayList<>(selectedPlayCards);
        selectedPlayCards.clear();
        executePlayMulti(0, cardsToPlay);
    }

    private void executePlayMulti(int playerIndex, List<Card> cards) {
        if (engine.getTrickCardsPlayed() > 0) {
            Card leadCard = engine.getCurrentTrick()[engine.getCurrentTrickLeader()];
            Suit leadSuit = engine.getTrumpInfo().getEffectiveSuit(leadCard);
            for (Card card : cards) {
                Suit cardSuit = engine.getTrumpInfo().getEffectiveSuit(card);
                if (leadSuit != null && leadSuit != cardSuit) {
                    cardTracker.markVoidSuit(playerIndex, leadSuit);
                    break;
                }
            }
        }

        engine.playCards(playerIndex, cards);
        for (Card card : cards) {
            cardTracker.cardPlayed(card, playerIndex);
        }

        renderTrick(playerIndex);
        updateHumanHand();
        updateTrackerPanel();

        if (engine.getTrickCardsPlayed() == 4) {
            schedule(1.0f, this::completeTrick);
        } else {
            processCurrentPlayer();
        }
    }

    private void completeTrick() {
        int winner = engine.evaluateTrick();
        updateInfoPanel();
        updateTrackerPanel();

        if (engine.isRoundOver()) {
            showRoundResult();
        } else {
            setStatus(players[winner].getName() + " 赢得此墩");
            schedule(0.3f, () -> animateTrickCollection(() -> schedule(0.2f, this::processCurrentPlayer)));
        }
    }

    // ======================== 结算 ========================

    private void showRoundResult() {
        RoundResult result = engine.calculateRoundResult();
        clearTrick();

        String winner = (result.getWinningTeam() == players[0].getTeam()) ? "你的队伍" : "对方队伍";
        String resultText = winner + "获胜！升 " + result.getLevelChange() + " 级";

        boolean gameWon = false;
        if (result.getWinningTeam() >= 0 && result.getLevelChange() > 0) {
            Rank[] levels = engine.getTeamLevels();
            int team = result.getWinningTeam();
            int newVal = levels[team].getValue() + result.getLevelChange();
            if (newVal > Rank.TEN.getValue()) {
                gameWon = true;
                levels[team] = Rank.TEN;
            } else {
                for (Rank r : Rank.values()) {
                    if (r.getValue() == newVal && r != Rank.SMALL_JOKER && r != Rank.BIG_JOKER) {
                        levels[team] = r;
                        break;
                    }
                }
            }
        }

        StringBuilder statusMsg = new StringBuilder();
        if (gameWon) {
            String gameWinner = (result.getWinningTeam() == players[0].getTeam()) ? "你的队伍" : "对方队伍";
            statusMsg.append("游戏结束！").append(gameWinner).append("打碎10获胜！");
        } else {
            statusMsg.append("本局结束！").append(resultText);
        }
        statusMsg.append(" 防守方得分：").append(result.getDefenderPoints());
        if (result.getKittyBloods() > 0) {
            statusMsg.append(" 扣王血：").append(result.getKittyBloods());
        }
        if (result.getTributeCount() > 0) {
            statusMsg.append(" 下轮进贡：").append(result.getTributeCount()).append("个");
        }
        setStatus(statusMsg.toString());

        actionTable.clearChildren();
        primaryBtn = null;
        boolean finalGameWon = gameWon;
        actionTable.add(UiUtils.button(gameWon ? "新游戏" : "下一局", assets.getButtonStyle(), () -> {
            actionTable.clearChildren();
            if (finalGameWon) {
                Rank[] lvls = engine.getTeamLevels();
                lvls[0] = Rank.THREE;
                lvls[1] = Rank.THREE;
            }
            startNewRound();
        })).width(140).height(48).padRight(8);
        actionTable.add(UiUtils.button("显示底牌", assets.getButtonStyle(), this::showKittyCards))
            .width(120).height(48);
        layoutActionTable();

        updateInfoPanel();
        updatePlayersPanel();
    }

    /**
     * 按大连打滚子规则查看底牌（工具栏"看底牌"）：
     * <ul>
     *   <li>叫主/埋底前：展示最初发到的底牌；</li>
     *   <li>出牌中：干锅（人类是本局庄家）或本局扣王，可看扣好的底牌；否则只能看最初发的底牌，
     *       扣下的底牌要到本局结束才可见；</li>
     *   <li>本局结束：可看扣好的底牌。</li>
     * </ul>
     */
    private void viewKittyByRule() {
        if (engine.getTrumpInfo() == null
            || engine.getKitty() == null || engine.getKitty().isEmpty()) {
            setStatus("底牌尚未产生");
            return;
        }
        GamePhase phase = engine.getPhase();
        boolean buried = phase == GamePhase.PLAYING || phase == GamePhase.ROUND_END;
        boolean humanIsDealer = engine.getDealerIndex() == 0;   // 干锅：人类是本局庄家
        boolean kouWang = engine.getKittyBloods() > 0;          // 本局是否扣王
        boolean roundOver = phase == GamePhase.ROUND_END;
        boolean canSeeBuried = humanIsDealer || kouWang || roundOver;

        List<Card> show;
        String title;
        boolean buriedHidden = false;
        if (!buried) {
            // 埋底尚未完成：底牌即为最初发的底牌
            show = engine.getKitty();
            title = "最初底牌";
        } else if (canSeeBuried) {
            show = engine.getKitty();
            title = kouWang ? "扣王底牌" : "底牌";
        } else {
            // 没干锅没扣王且本局未结束：只能看最初发的底牌，看不到扣下的底牌
            if (originalKitty.isEmpty()) {
                setStatus("当前无法查看扣下的底牌，本局结束后可查看");
                return;
            }
            show = new ArrayList<>(originalKitty);
            title = "最初底牌";
            buriedHidden = true;
        }

        displayCenterCards(title, show, true);
        int pts = 0;
        StringBuilder sb = new StringBuilder(title).append("：");
        for (Card card : show) {
            sb.append(cardName(card)).append(" ");
            pts += card.getPoints();
        }
        sb.append("（分值：").append(pts).append("分）");
        if (buriedHidden) {
            sb.append("  ·扣下的底牌本局结束后可见");
        }
        setStatus(sb.toString());
    }

    private void showKittyCards() {
        List<Card> kittyCards = engine.getKitty();
        if (kittyCards == null || kittyCards.isEmpty()) {
            setStatus("底牌信息不可用");
            return;
        }
        displayCenterCards("底牌", kittyCards, true);
        int kittyPoints = 0;
        StringBuilder kittyText = new StringBuilder("底牌：");
        for (Card card : kittyCards) {
            kittyText.append(cardName(card)).append(" ");
            kittyPoints += card.getPoints();
        }
        kittyText.append("（分值：").append(kittyPoints).append("分）");
        setStatus(kittyText.toString());
    }

    // ======================== 渲染：手牌 ========================

    private void updateHumanHand() {
        updateHumanHand(-1f);
    }

    private void updateHumanHand(float dealInterval) {
        handGroup.clearChildren();
        List<Card> hand = sortForDisplay(players[0].getHand(), engine.getTrumpInfo());
        int n = hand.size();
        if (n == 0) {
            return;
        }
        float avail = viewW - 40f;
        float step = n > 1 ? Math.min(OVERLAP, (avail - CARD_W) / (n - 1)) : OVERLAP;
        float rowW = (n - 1) * step + CARD_W;
        float startX = (viewW - rowW) / 2f;
        float baseY = 14f;

        for (int i = 0; i < n; i++) {
            final Card c = hand.get(i);
            CardActor ca = new CardActor(assets, c, true);
            boolean selected = (waitingForKitty && selectedKittyCards.contains(c))
                || ((waitingForHumanPlay || waitingForTributeReturn) && selectedPlayCards.contains(c));
            ca.setSelected(selected);
            ca.setPosition(startX + i * step, baseY + (selected ? 20f : 0f));

            ClickListener cl = new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    onHandCardClicked(c, event.getButton());
                }
            };
            cl.setButton(-1);
            ca.addListener(cl);

            if (dealInterval > 0) {
                ca.getColor().a = 0f;
                ca.addAction(Actions.sequence(Actions.delay(dealInterval * i), Actions.fadeIn(0.2f)));
            }
            handGroup.addActor(ca);
        }
    }

    private void onHandCardClicked(Card card, int button) {
        if (waitingForTributeReturn) {
            handleTributeReturnCardClick(card);
            return;
        }
        if (waitingForHumanPlay) {
            if (button == Input.Buttons.RIGHT) {
                if (!selectedPlayCards.contains(card)) {
                    selectedPlayCards.add(card);
                }
                if (engine.isValidPlay(0, selectedPlayCards)) {
                    confirmPlay();
                } else {
                    updateHumanHand();
                    updatePlayButtonState();
                }
            } else {
                handleHumanCardClick(card);
            }
            return;
        }
        if (waitingForKitty) {
            toggleKittyCard(card);
        }
    }

    // ======================== 渲染：出牌区 ========================

    private void renderTrick(int animateIdx) {
        trickGroup.clearChildren();
        List<Card>[] trick = engine.getCurrentTrickCards();
        boolean anyPlayed = false;
        for (List<Card> played : trick) {
            if (played != null && !played.isEmpty()) {
                anyPlayed = true;
                break;
            }
        }
        if (anyPlayed) {
            clearCenter();
        }
        float cx = viewW / 2f;
        float cy = viewH / 2f + 10f;
        float[][] anchors = {
            {cx, cy - 120f},
            {cx - 170f, cy},
            {cx, cy + 80f},
            {cx + 170f, cy}
        };
        for (int i = 0; i < 4; i++) {
            List<Card> cards = trick[i];
            if (cards == null || cards.isEmpty()) {
                continue;
            }
            float advance = CARD_W - 20f;
            float groupW = (cards.size() - 1) * advance + CARD_W;
            float startX = anchors[i][0] - groupW / 2f;
            float yy = anchors[i][1] - CARD_H / 2f;
            for (int j = 0; j < cards.size(); j++) {
                CardActor ca = new CardActor(assets, cards.get(j), true);
                float tx = startX + j * advance;
                ca.setPosition(tx, yy);
                if (i == animateIdx) {
                    float dx = 0;
                    float dy = 0;
                    switch (i) {
                        case 0 -> dy = 60f;
                        case 1 -> dx = -60f;
                        case 2 -> dy = -60f;
                        case 3 -> dx = 60f;
                        default -> { }
                    }
                    ca.setPosition(tx + dx, yy + dy);
                    ca.getColor().a = 0f;
                    ca.addAction(Actions.parallel(Actions.moveTo(tx, yy, 0.25f), Actions.fadeIn(0.25f)));
                }
                trickGroup.addActor(ca);
            }
        }
    }

    private void clearTrick() {
        trickGroup.clearChildren();
    }

    private void animateTrickCollection(Runnable onComplete) {
        if (trickGroup.getChildren().size == 0) {
            if (onComplete != null) {
                onComplete.run();
            }
            return;
        }
        trickGroup.addAction(Actions.sequence(Actions.fadeOut(0.3f), Actions.run(() -> {
            trickGroup.clearChildren();
            trickGroup.getColor().a = 1f;
            if (onComplete != null) {
                onComplete.run();
            }
        })));
    }

    // ======================== 渲染：中央底牌 / 进贡 ========================

    private void displayCenterCards(String title, List<Card> cards, boolean faceUp) {
        centerTitle = title;
        centerCards = cards == null ? null : new ArrayList<>(cards);
        centerFaceUp = faceUp;
        renderCenter();
    }

    private void clearCenter() {
        centerTitle = null;
        centerCards = null;
        centerGroup.clearChildren();
    }

    private void renderCenter() {
        centerGroup.clearChildren();
        if (centerCards == null || centerCards.isEmpty()) {
            return;
        }
        float cx = viewW / 2f;
        float cy = viewH / 2f + 10f;

        Label title = new Label(centerTitle, assets.getLabelStyleSmall());
        title.setColor(Assets.STATUS);
        title.pack();
        title.setPosition(cx - title.getWidth() / 2f, cy + CARD_H / 2f + 6f);
        centerGroup.addActor(title);

        float advance = 20f;
        float groupW = (centerCards.size() - 1) * advance + CARD_W;
        float startX = cx - groupW / 2f;
        float yy = cy - CARD_H / 2f;
        for (int i = 0; i < centerCards.size(); i++) {
            CardActor ca = new CardActor(assets, centerCards.get(i), centerFaceUp);
            ca.setPosition(startX + i * advance, yy);
            centerGroup.addActor(ca);
        }
    }

    private void displayBuriedKitty() {
        if (engine.getKittyBloods() > 0) {
            displayCenterCards("扣王", engine.getKitty(), true);
            showCenterBanner("扣王！+" + engine.getKittyBloods() + " 血", Assets.GOLD);
        } else {
            displayCenterCards("底牌", engine.getKitty(), false);
            showCenterBanner("埋底完成", Assets.STATUS);
        }
    }

    /**
     * 在牌桌中央弹出一条带缩放+淡入淡出动画的提示横幅，用于发牌/亮主/扣牌等阶段的视觉强化。
     * 横幅作为一次性覆盖层加入舞台顶层，动画结束后自动移除，不影响中央底牌/出牌区的渲染。
     */
    private void showCenterBanner(String text, Color color) {
        if (viewW <= 0 || viewH <= 0) {
            return;
        }
        Label banner = new Label(text, assets.getTitleStyle());
        banner.setColor(color);
        banner.pack();
        banner.setOrigin(Align.center);
        banner.setPosition((viewW - banner.getWidth()) / 2f, viewH / 2f + CARD_H + 20f);
        banner.getColor().a = 0f;
        banner.setScale(0.6f);
        banner.addAction(Actions.sequence(
            Actions.parallel(Actions.fadeIn(0.25f), Actions.scaleTo(1.15f, 1.15f, 0.25f)),
            Actions.scaleTo(1f, 1f, 0.12f),
            Actions.delay(1.0f),
            Actions.parallel(Actions.fadeOut(0.3f), Actions.moveBy(0f, 24f, 0.3f)),
            Actions.removeActor()));
        stage.addActor(banner);
    }

    // ======================== 渲染：信息面板 / 玩家 / 记牌器 ========================

    private void updateInfoPanel() {
        roundLabel.setText("第 " + engine.getRoundNumber() + " 局");

        TrumpInfo ti = engine.getTrumpInfo();
        if (ti != null) {
            String suitName = ti.getTrumpSuit() != null
                ? ti.getTrumpSuit().getDisplayName() : "无";
            trumpLabel.setText("主牌：" + suitName + " " + ti.getTrumpRank().getDisplayName());
        } else {
            trumpLabel.setText("主牌：未定");
        }

        scoreLabel.setText("防守方得分：" + engine.getDefenderPoints());

        int tributeCount = engine.getPreviousTributeCount();
        if (engine.isTributeRequired() && tributeCount > 0) {
            tributeLabel.setText("本局进贡：" + tributeCount + " 个（上贡给庄家）");
            tributeLabel.setColor(Assets.GOLD);
        } else {
            tributeLabel.setText("本局进贡：无");
            tributeLabel.setColor(Color.WHITE);
        }

        if (ti != null) {
            dealerLabel.setText("庄家：" + players[engine.getDealerIndex()].getName());
            dealerLabel.setColor(Assets.GOLD);
        } else {
            dealerLabel.setText("庄家：未定");
            dealerLabel.setColor(Color.WHITE);
        }

        Rank[] levels = engine.getTeamLevels();
        teamLevelLabel.setText("你的队伍：" + levels[0].getDisplayName()
            + " | 对方队伍：" + levels[1].getDisplayName());

        infoPanel.pack();
        infoPanel.setPosition(viewW - infoPanel.getWidth() - 12, viewH - infoPanel.getHeight() - 12);
    }

    private void updatePlayersPanel() {
        int dealerIdx = engine.getTrumpInfo() != null ? engine.getDealerIndex() : -1;
        for (int i = 0; i < 4; i++) {
            Label label = playerNameLabels[i];
            if (label == null) {
                continue;
            }
            String name = players[i].getName() + "（" + players[i].getHand().size() + "）";
            if (i == dealerIdx) {
                name = name + " [庄]";
            }
            label.setText(name);
            if (i == currentHighlightIdx) {
                label.setColor(Assets.STATUS);
            } else if (i == dealerIdx) {
                label.setColor(Assets.GOLD);
            } else {
                label.setColor(Color.WHITE);
            }
            label.pack();
            // 顶部搭档名称按实际宽度水平居中，避免追加庄家标记后向右偏移
            if (i == 2 && viewW > 0) {
                label.setX((viewW - label.getWidth()) / 2f);
            }
        }
    }

    private void updateTrackerPanel() {
        if (!config.isTrackerEnabled()) {
            trackerTable.setVisible(false);
            return;
        }
        trackerTable.setVisible(true);
        trackerTable.clearChildren();
        trackerTable.add(trackerCell("牌面", true, Color.WHITE));
        for (Suit suit : Suit.values()) {
            Color c = suit.getColor().equals("red") ? new Color(1f, 0.4f, 0.4f, 1f) : Color.WHITE;
            trackerTable.add(trackerSuitCell(suit, c)).size(16, 16);
        }
        trackerTable.row();

        for (Rank rank : Rank.values()) {
            if (rank == Rank.SMALL_JOKER || rank == Rank.BIG_JOKER) {
                continue;
            }
            trackerTable.add(trackerCell(rank.getDisplayName(), false, Color.WHITE));
            for (Suit suit : Suit.values()) {
                int remaining = remainingBySuitAndRank(suit, rank);
                Color c = remaining == 0 ? new Color(0.4f, 0.4f, 0.4f, 1f) : Color.WHITE;
                trackerTable.add(trackerCell(String.valueOf(remaining), false, c));
            }
            trackerTable.row();
        }

        trackerTable.add(trackerCell("小王", false, Color.WHITE));
        trackerTable.add(trackerCell(String.valueOf(jokerRemaining(Rank.SMALL_JOKER)), false, Color.WHITE));
        trackerTable.row();
        trackerTable.add(trackerCell("大王", false, Color.WHITE));
        trackerTable.add(trackerCell(String.valueOf(jokerRemaining(Rank.BIG_JOKER)), false, Color.WHITE));

        trackerTable.pack();
        trackerTable.setPosition(viewW - trackerTable.getWidth() - 12,
            infoPanel.getY() - trackerTable.getHeight() - 10);
    }

    private Label trackerCell(String text, boolean bold, Color color) {
        Label label = new Label(text, assets.getLabelStyleSmall());
        label.setColor(color);
        return label;
    }

    /** 记牌器表头的花色格：用程序绘制的花色纹理，避免 ♠♥♦♣ 在系统字体缺字形时显示方块。 */
    private Image trackerSuitCell(Suit suit, Color color) {
        Image image = new Image(new TextureRegionDrawable(new TextureRegion(assets.getSuitTexture(suit))));
        image.setColor(color);
        return image;
    }

    /** 卡牌在状态文本中的中文名：花色用中文（黑桃A），避免 ♠♥♦♣ 符号在字体缺字形时显示方块。 */
    private static String cardName(Card card) {
        if (card.getSuit() == null) {
            return card.getRank().getDisplayName();
        }
        return card.getSuit().getDisplayName() + card.getRank().getDisplayName();
    }

    private int remainingBySuitAndRank(Suit suit, Rank rank) {
        int played = 0;
        for (Card c : cardTracker.getPlayedCards()) {
            if (c.getSuit() == suit && c.getRank() == rank) {
                played++;
            }
        }
        int inHand = 0;
        for (Card c : players[0].getHand()) {
            if (c.getSuit() == suit && c.getRank() == rank) {
                inHand++;
            }
        }
        return 3 - played - inHand;
    }

    private int jokerRemaining(Rank jokerRank) {
        int played = 0;
        for (Card c : cardTracker.getPlayedCards()) {
            if (c.getRank() == jokerRank) {
                played++;
            }
        }
        int inHand = 0;
        for (Card c : players[0].getHand()) {
            if (c.getRank() == jokerRank) {
                inHand++;
            }
        }
        return 3 - played - inHand;
    }

    // ======================== 工具 ========================

    private List<Card> sortForDisplay(List<Card> hand, TrumpInfo trumpInfo) {
        List<Card> sorted = new ArrayList<>(hand);
        if (trumpInfo == null) {
            sorted.sort(Comparator
                .comparingInt((Card c) -> {
                    if (c.getRank() == Rank.BIG_JOKER || c.getRank() == Rank.SMALL_JOKER) {
                        return -1;
                    }
                    return c.getSuit() == null ? 99 : suitDisplayOrder(c.getSuit());
                })
                .thenComparing(Comparator.comparingInt(
                    (Card c) -> TrumpInfo.effectiveRankStrength(c.getRank())).reversed()));
            return sorted;
        }
        Suit ts = trumpInfo.getTrumpSuit();
        sorted.sort((a, b) -> {
            int ga = displayGroup(a, trumpInfo);
            int gb = displayGroup(b, trumpInfo);
            if (ga != gb) {
                return Integer.compare(ga, gb);
            }
            if (ga == 0) {
                return Integer.compare(b.getRank().getValue(), a.getRank().getValue());
            }
            if (ga == 1 || ga == 2) {
                boolean aTS = ts != null && a.getSuit() == ts;
                boolean bTS = ts != null && b.getSuit() == ts;
                if (aTS != bTS) {
                    return aTS ? -1 : 1;
                }
                return Integer.compare(
                    a.getSuit() == null ? 99 : suitDisplayOrder(a.getSuit()),
                    b.getSuit() == null ? 99 : suitDisplayOrder(b.getSuit()));
            }
            if (ga == 3) {
                return Integer.compare(
                    TrumpInfo.effectiveRankStrength(b.getRank()),
                    TrumpInfo.effectiveRankStrength(a.getRank()));
            }
            int suitCmp = Integer.compare(
                a.getSuit() == null ? 99 : suitDisplayOrder(a.getSuit()),
                b.getSuit() == null ? 99 : suitDisplayOrder(b.getSuit()));
            if (suitCmp != 0) {
                return suitCmp;
            }
            return Integer.compare(
                TrumpInfo.effectiveRankStrength(b.getRank()),
                TrumpInfo.effectiveRankStrength(a.getRank()));
        });
        return sorted;
    }

    private int suitDisplayOrder(Suit suit) {
        return switch (suit) {
            case SPADE -> 0;
            case HEART -> 1;
            case CLUB -> 2;
            case DIAMOND -> 3;
        };
    }

    private int displayGroup(Card card, TrumpInfo trumpInfo) {
        Rank rank = card.getRank();
        if (rank == Rank.BIG_JOKER || rank == Rank.SMALL_JOKER) {
            return 0;
        }
        if (rank == trumpInfo.getTrumpRank()) {
            return 1;
        }
        if (rank == Rank.TWO) {
            return 2;
        }
        if (trumpInfo.getTrumpSuit() != null && card.getSuit() == trumpInfo.getTrumpSuit()) {
            return 3;
        }
        return 4;
    }

    private void setStatus(String text) {
        statusLabel.setText(text);
    }

    private void schedule(float delaySeconds, Runnable runnable) {
        stage.addAction(Actions.sequence(Actions.delay(delaySeconds), Actions.run(runnable)));
    }
}
