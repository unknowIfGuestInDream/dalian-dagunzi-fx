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

package com.tlcsdm.game.daliandagunzifx;

import com.tlcsdm.game.daliandagunzifx.ai.AILevel;
import com.tlcsdm.game.daliandagunzifx.ai.AIStrategy;
import com.tlcsdm.game.daliandagunzifx.ai.EasyAI;
import com.tlcsdm.game.daliandagunzifx.ai.HardAI;
import com.tlcsdm.game.daliandagunzifx.ai.MediumAI;
import com.tlcsdm.game.daliandagunzifx.engine.GameEngine;
import com.tlcsdm.game.daliandagunzifx.engine.GamePhase;
import com.tlcsdm.game.daliandagunzifx.engine.Player;
import com.tlcsdm.game.daliandagunzifx.engine.RoundResult;
import com.tlcsdm.game.daliandagunzifx.engine.TrumpInfo;
import com.tlcsdm.game.daliandagunzifx.model.Card;
import com.tlcsdm.game.daliandagunzifx.model.Rank;
import com.tlcsdm.game.daliandagunzifx.model.Suit;
import com.tlcsdm.game.daliandagunzifx.tracker.CardTracker;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Separator;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.StringConverter;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.material.Material;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Main JavaFX application for the 大连打滚子 (Da Gunzi) card game.
 * Builds the entire UI programmatically without FXML.
 *
 * @author unknowIfGuestInDream
 */
public class DaGunZiApp extends Application {

    private static final int WINDOW_WIDTH = 1200;
    private static final int WINDOW_HEIGHT = 800;
    private static final int CARD_WIDTH = 66;
    private static final int CARD_HEIGHT = 86;
    private static final int CARD_OVERLAP = 25;
    private static final String TABLE_COLOR = "#2d8a4e";
    private static final String DARK_TABLE_COLOR = "#1a1a2e";
    private static final String APP_VERSION = "1.0.0";

    // Game state
    private GameEngine engine;
    private AIStrategy aiStrategy;
    private CardTracker cardTracker;
    private Player[] players;

    // UI interaction state
    private boolean waitingForHumanPlay;
    private boolean waitingForKitty;
    private final List<Card> selectedKittyCards = new ArrayList<>();

    // UI components
    private Stage primaryStage;
    private StackPane rootPane;
    private Pane humanHandPane;
    private GridPane trickArea;
    private VBox infoPanel;
    private GridPane trackerGrid;
    private VBox trackerSection;
    private Label statusLabel;
    private HBox actionPane;
    private Label trumpLabel;
    private Label scoreLabel;
    private Label teamLevelLabel;
    private Label roundLabel;
    private Label dealerLabel;
    private final StackPane[] trickCardNodes = new StackPane[4];
    private final Label[] playerNameLabels = new Label[4];
    private final Label[] playerCountLabels = new Label[4];

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        rootPane = new StackPane();
        showWelcomeScreen();

        Scene scene = new Scene(rootPane, WINDOW_WIDTH, WINDOW_HEIGHT);
        stage.setScene(scene);
        stage.setTitle("大连打滚子");
        stage.setResizable(false);
        stage.getIcons().add(createAppIcon());
        stage.show();
    }

    /**
     * Loads the application icon from resources.
     */
    private javafx.scene.image.Image createAppIcon() {
        return new javafx.scene.image.Image(
            getClass().getResourceAsStream("logo.png"));
    }

    // ======================== Menu Bar ========================

    private MenuBar createMenuBar(boolean inGame) {
        MenuBar menuBar = new MenuBar();

        // --- Game menu (only during gameplay) ---
        if (inGame) {
            Menu gameMenu = new Menu("游戏");
            gameMenu.setGraphic(new FontIcon(Material.VIDEOGAME_ASSET));

            MenuItem restartItem = new MenuItem("重新开始");
            restartItem.setGraphic(new FontIcon(Material.REPLAY));
            restartItem.setOnAction(e -> confirmRestartRound());

            MenuItem returnItem = new MenuItem("返回主界面");
            returnItem.setGraphic(new FontIcon(Material.HOME));
            returnItem.setOnAction(e -> confirmReturnToWelcome());

            gameMenu.getItems().addAll(restartItem, returnItem);
            menuBar.getMenus().add(gameMenu);
        }

        // --- Settings menu ---
        Menu settingsMenu = new Menu("设置");
        settingsMenu.setGraphic(new FontIcon(Material.SETTINGS));

        MenuItem preferencesItem = new MenuItem("偏好设置");
        preferencesItem.setGraphic(new FontIcon(Material.SETTINGS));
        preferencesItem.setOnAction(e ->
            AppSettings.getInstance().getPreferencesFx().show(true));

        settingsMenu.getItems().add(preferencesItem);

        if (inGame) {
            CheckMenuItem trackerItem = new CheckMenuItem("记牌器");
            trackerItem.setGraphic(new FontIcon(Material.VISIBILITY));
            trackerItem.setSelected(AppSettings.getInstance().isTrackerEnabled());
            trackerItem.setOnAction(e -> {
                AppSettings.getInstance().trackerEnabledProperty().set(trackerItem.isSelected());
                toggleTrackerDisplay(trackerItem.isSelected());
            });
            settingsMenu.getItems().add(trackerItem);
        }

        // --- Rules menu ---
        Menu rulesMenu = new Menu("规则");
        rulesMenu.setGraphic(new FontIcon(Material.LIBRARY_BOOKS));

        MenuItem rulesItem = new MenuItem("规则说明");
        rulesItem.setGraphic(new FontIcon(Material.DESCRIPTION));
        rulesItem.setOnAction(e -> RulesDialog.showAll(primaryStage));

        rulesMenu.getItems().add(rulesItem);

        // --- About menu ---
        Menu helpMenu = new Menu("帮助");
        helpMenu.setGraphic(new FontIcon(Material.HELP_OUTLINE));

        MenuItem aboutItem = new MenuItem("关于");
        aboutItem.setGraphic(new FontIcon(Material.INFO_OUTLINE));
        aboutItem.setOnAction(e -> showAboutDialog());

        helpMenu.getItems().add(aboutItem);

        menuBar.getMenus().addAll(settingsMenu, rulesMenu, helpMenu);
        return menuBar;
    }

    private void showAboutDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("关于");
        alert.setHeaderText("大连打滚子 Da Gunzi");

        javafx.scene.image.ImageView logoView = new javafx.scene.image.ImageView(createAppIcon());
        logoView.setFitWidth(64);
        logoView.setFitHeight(64);
        alert.setGraphic(logoView);

        Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
        alertStage.getIcons().add(createAppIcon());

        VBox content = new VBox(8);
        content.setPadding(new Insets(10, 0, 0, 0));
        Label versionLabel = new Label("版本：" + APP_VERSION);
        Label descLabel = new Label("大连打滚子单机版 - 基于 JavaFX 的单机卡牌游戏");
        descLabel.setWrapText(true);
        content.getChildren().addAll(versionLabel, descLabel);
        alert.getDialogPane().setContent(content);
        alert.getDialogPane().setPrefWidth(400);
        alert.showAndWait();
    }

    private void confirmRestartRound() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认");
        alert.setHeaderText("重新开始");
        alert.setContentText("确定要重新开始吗？本局游戏将丢失。");

        Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
        alertStage.getIcons().add(createAppIcon());

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                restartCurrentRound();
            }
        });
    }

    private void confirmReturnToWelcome() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("确认");
        alert.setHeaderText("返回主界面");
        alert.setContentText("确定要返回主界面吗？本局游戏及进度将丢失。");

        Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
        alertStage.getIcons().add(createAppIcon());

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                showWelcomeScreen();
            }
        });
    }

    private void restartCurrentRound() {
        if (engine == null) return;
        Rank[] oldLevels = engine.getTeamLevels().clone();

        players = new Player[]{
            new Player(0, "你", true),
            new Player(1, "电脑1", false),
            new Player(2, "电脑2(搭档)", false),
            new Player(3, "电脑3", false)
        };
        cardTracker = new CardTracker();
        aiStrategy = switch (AppSettings.getInstance().getAiLevel()) {
            case EASY -> new EasyAI();
            case MEDIUM -> new MediumAI();
            case HARD -> new HardAI(cardTracker);
        };
        engine = new GameEngine(players);
        engine.getTeamLevels()[0] = oldLevels[0];
        engine.getTeamLevels()[1] = oldLevels[1];

        initGameBoard();
        startNewRound();
    }

    private void toggleTrackerDisplay(boolean enabled) {
        if (trackerSection != null) {
            trackerSection.setVisible(enabled);
            trackerSection.setManaged(enabled);
            if (enabled) {
                updateTrackerPanel();
            }
        }
    }

    // ======================== Welcome Screen ========================

    private void showWelcomeScreen() {
        rootPane.getChildren().clear();

        AppSettings settings = AppSettings.getInstance();
        String bgColor = settings.isDarkTheme() ? DARK_TABLE_COLOR : TABLE_COLOR;

        VBox welcomeBox = new VBox(20);
        welcomeBox.setAlignment(Pos.CENTER);
        welcomeBox.setStyle("-fx-background-color: " + bgColor + ";");
        VBox.setVgrow(welcomeBox, Priority.ALWAYS);

        Label title = new Label("大连打滚子");
        title.setStyle("-fx-font-size: 48px; -fx-text-fill: gold; -fx-font-weight: bold;");

        Label subtitle = new Label("Da Gunzi Card Game");
        subtitle.setStyle("-fx-font-size: 18px; -fx-text-fill: #cccccc;");

        // AI difficulty selector
        HBox diffBox = new HBox(10);
        diffBox.setAlignment(Pos.CENTER);
        Label diffLabel = new Label("AI难度：");
        diffLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px;");
        ComboBox<AILevel> diffCombo = new ComboBox<>();
        diffCombo.getItems().addAll(AILevel.values());
        diffCombo.setValue(settings.getAiLevel());
        diffCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(AILevel level) {
                return level == null ? "" : level.getDisplayName();
            }

            @Override
            public AILevel fromString(String s) {
                return null;
            }
        });
        diffCombo.setOnAction(e -> settings.aiLevelProperty().set(diffCombo.getValue()));
        diffBox.getChildren().addAll(diffLabel, diffCombo);

        // Card tracker toggle
        CheckBox trackerCheck = new CheckBox("开启记牌器");
        trackerCheck.setStyle("-fx-text-fill: white; -fx-font-size: 16px;");
        trackerCheck.setSelected(settings.isTrackerEnabled());
        trackerCheck.setOnAction(e -> settings.trackerEnabledProperty().set(trackerCheck.isSelected()));

        // Start button
        Button startBtn = new Button("开始游戏");
        startBtn.setStyle("-fx-font-size: 20px; -fx-padding: 10 30; "
            + "-fx-background-color: #d4af37; -fx-text-fill: black; -fx-font-weight: bold; -fx-cursor: hand;");
        startBtn.setOnAction(e -> startGame());

        Region spacer1 = new Region();
        spacer1.setPrefHeight(20);
        Region spacer2 = new Region();
        spacer2.setPrefHeight(10);

        welcomeBox.getChildren().addAll(title, subtitle, spacer1, diffBox, trackerCheck, spacer2, startBtn);

        VBox wrapper = new VBox();
        wrapper.getChildren().addAll(createMenuBar(false), welcomeBox);
        rootPane.getChildren().add(wrapper);
    }

    // ======================== Game Initialization ========================

    private void startGame() {
        players = new Player[]{
            new Player(0, "你", true),
            new Player(1, "电脑1", false),
            new Player(2, "电脑2(搭档)", false),
            new Player(3, "电脑3", false)
        };

        cardTracker = new CardTracker();
        aiStrategy = switch (AppSettings.getInstance().getAiLevel()) {
            case EASY -> new EasyAI();
            case MEDIUM -> new MediumAI();
            case HARD -> new HardAI(cardTracker);
        };
        engine = new GameEngine(players);

        initGameBoard();
        startNewRound();
    }

    private void initGameBoard() {
        rootPane.getChildren().clear();

        String bgColor = AppSettings.getInstance().isDarkTheme() ? DARK_TABLE_COLOR : TABLE_COLOR;

        BorderPane gameBoard = new BorderPane();
        gameBoard.setStyle("-fx-background-color: " + bgColor + ";");

        statusLabel = new Label();
        statusLabel.setStyle("-fx-text-fill: #ffdd57; -fx-font-size: 16px; -fx-font-weight: bold;");

        actionPane = new HBox(10);
        actionPane.setAlignment(Pos.CENTER);
        actionPane.setPadding(new Insets(5));

        // Top: menu bar + Player 2 (partner)
        VBox topPlayerPane = createAIPlayerPane("电脑2(搭档)", 2);
        topPlayerPane.setPadding(new Insets(8));

        VBox topArea = new VBox();
        topArea.getChildren().addAll(createMenuBar(true), topPlayerPane);

        // Left: Player 1
        VBox leftPlayerPane = createAIPlayerPane("电脑1", 1);
        leftPlayerPane.setMinWidth(120);
        leftPlayerPane.setPrefWidth(120);

        // Right: Player 3 + info panel
        VBox rightPlayerPane = createAIPlayerPane("电脑3", 3);
        buildInfoPanel();

        VBox rightSide = new VBox(10);
        rightSide.setPadding(new Insets(5));
        rightSide.getChildren().addAll(rightPlayerPane, infoPanel);
        rightSide.setMinWidth(220);
        rightSide.setPrefWidth(220);

        // Center: trick area
        buildTrickArea();
        VBox centerBox = new VBox(10);
        centerBox.setAlignment(Pos.CENTER);
        centerBox.getChildren().addAll(statusLabel, trickArea, actionPane);

        // Bottom: human player
        VBox bottomArea = buildHumanArea();

        gameBoard.setTop(topArea);
        gameBoard.setLeft(leftPlayerPane);
        gameBoard.setRight(rightSide);
        gameBoard.setCenter(centerBox);
        gameBoard.setBottom(bottomArea);

        BorderPane.setAlignment(topArea, Pos.CENTER);
        BorderPane.setAlignment(leftPlayerPane, Pos.CENTER_LEFT);
        BorderPane.setMargin(leftPlayerPane, new Insets(0, 0, 0, 10));

        rootPane.getChildren().add(gameBoard);
    }

    private VBox createAIPlayerPane(String name, int playerIndex) {
        VBox pane = new VBox(5);
        pane.setAlignment(Pos.CENTER);
        pane.setPadding(new Insets(10));

        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        playerNameLabels[playerIndex] = nameLabel;

        Label countLabel = new Label("牌数：38");
        countLabel.setStyle("-fx-text-fill: #cccccc; -fx-font-size: 12px;");
        playerCountLabels[playerIndex] = countLabel;

        pane.getChildren().addAll(nameLabel, countLabel);
        return pane;
    }

    private void buildInfoPanel() {
        infoPanel = new VBox(8);
        infoPanel.setPadding(new Insets(10));
        infoPanel.setStyle("-fx-background-color: rgba(0,0,0,0.3); -fx-background-radius: 8;");
        infoPanel.setMinWidth(200);

        roundLabel = createInfoLabel("第 0 局");
        trumpLabel = createInfoLabel("主牌：未定");
        dealerLabel = createInfoLabel("庄家：未定");
        scoreLabel = createInfoLabel("防守方得分：0");
        teamLevelLabel = createInfoLabel("队伍级别：3 / 3");

        infoPanel.getChildren().addAll(roundLabel, trumpLabel, dealerLabel, scoreLabel, teamLevelLabel);

        trackerGrid = new GridPane();
        trackerGrid.setHgap(3);
        trackerGrid.setVgap(2);
        trackerGrid.setPadding(new Insets(5));
        Label trackerTitle = new Label("记牌器");
        trackerTitle.setStyle("-fx-text-fill: #ffdd57; -fx-font-size: 14px; -fx-font-weight: bold;");

        trackerSection = new VBox(5);
        trackerSection.getChildren().addAll(new Separator(), trackerTitle, trackerGrid);

        boolean trackerEnabled = AppSettings.getInstance().isTrackerEnabled();
        trackerSection.setVisible(trackerEnabled);
        trackerSection.setManaged(trackerEnabled);

        infoPanel.getChildren().add(trackerSection);

        if (trackerEnabled) {
            updateTrackerPanel();
        }
    }

    private void buildTrickArea() {
        trickArea = new GridPane();
        trickArea.setAlignment(Pos.CENTER);
        trickArea.setHgap(20);
        trickArea.setVgap(10);

        for (int i = 0; i < 4; i++) {
            trickCardNodes[i] = new StackPane();
            trickCardNodes[i].setMinSize(CARD_WIDTH, CARD_HEIGHT);
            trickCardNodes[i].setPrefSize(CARD_WIDTH, CARD_HEIGHT);
        }
        // Player 2 (partner) top-center
        trickArea.add(trickCardNodes[2], 1, 0);
        // Player 1 left
        trickArea.add(trickCardNodes[1], 0, 1);
        // Player 3 right
        trickArea.add(trickCardNodes[3], 2, 1);
        // Player 0 (human) bottom-center
        trickArea.add(trickCardNodes[0], 1, 2);
    }

    private VBox buildHumanArea() {
        Label humanNameLabel = new Label("你");
        humanNameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold;");
        playerNameLabels[0] = humanNameLabel;

        Label humanCountLabel = new Label("牌数：38");
        humanCountLabel.setStyle("-fx-text-fill: #cccccc; -fx-font-size: 12px;");
        playerCountLabels[0] = humanCountLabel;

        HBox humanInfoBar = new HBox(10);
        humanInfoBar.setAlignment(Pos.CENTER);
        humanInfoBar.getChildren().addAll(humanNameLabel, humanCountLabel);

        humanHandPane = new Pane();
        humanHandPane.setMinHeight(120);
        humanHandPane.setPrefHeight(120);

        VBox bottomArea = new VBox(5);
        bottomArea.setAlignment(Pos.CENTER);
        bottomArea.setPadding(new Insets(5, 10, 8, 10));
        bottomArea.getChildren().addAll(humanInfoBar, humanHandPane);
        return bottomArea;
    }

    private Label createInfoLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: white; -fx-font-size: 13px;");
        label.setWrapText(true);
        return label;
    }

    // ======================== Round Management ========================

    private void startNewRound() {
        cardTracker.reset();
        selectedKittyCards.clear();
        waitingForHumanPlay = false;
        waitingForKitty = false;

        engine.startNewRound();

        // Handle tribute if applicable
        if (engine.isTributeRequired()) {
            String tributeMsg = engine.performAutoTribute();
            if (tributeMsg != null) {
                statusLabel.setText("进贡：" + tributeMsg);
            }
        }

        updateInfoPanel();
        updateAIPlayerPanes();
        clearTrickArea();

        // Dealing animation: show cards appearing one by one in the human hand
        animateDealing(() -> beginTrumpDeclaration());
    }

    private void animateDealing(Runnable onComplete) {
        statusLabel.setText("发牌中...");
        humanHandPane.getChildren().clear();

        List<Card> hand = sortForDisplay(players[0].getHand(), engine.getTrumpInfo());
        Timeline dealTimeline = new Timeline();
        double dealInterval = engine.isFirstRound() ? 80.0 : 40.0;

        for (int i = 0; i < hand.size(); i++) {
            final int index = i;
            final Card card = hand.get(i);
            KeyFrame kf = new KeyFrame(Duration.millis(dealInterval * i), e -> {
                StackPane cardNode = createCardFace(card);
                cardNode.setLayoutX(10 + index * CARD_OVERLAP);
                cardNode.setLayoutY(25);
                cardNode.setOpacity(0);
                cardNode.setScaleX(0.3);
                cardNode.setScaleY(0.3);
                humanHandPane.getChildren().add(cardNode);

                // Card fade+scale in animation
                FadeTransition fade = new FadeTransition(Duration.millis(200), cardNode);
                fade.setFromValue(0);
                fade.setToValue(1);
                ScaleTransition scale = new ScaleTransition(Duration.millis(200), cardNode);
                scale.setFromX(0.3);
                scale.setFromY(0.3);
                scale.setToX(1.0);
                scale.setToY(1.0);
                scale.setInterpolator(Interpolator.EASE_OUT);
                ParallelTransition pt = new ParallelTransition(fade, scale);
                pt.play();
            });
            dealTimeline.getKeyFrames().add(kf);
        }

        // After all cards dealt, refresh the hand properly and proceed
        KeyFrame finalKf = new KeyFrame(Duration.millis(dealInterval * hand.size() + 300), e -> {
            updateHumanHand();
            playerCountLabels[0].setText("牌数：" + players[0].getHand().size());
            onComplete.run();
        });
        dealTimeline.getKeyFrames().add(finalKf);
        dealTimeline.play();
    }

    // ======================== Trump Declaration ========================

    private void beginTrumpDeclaration() {
        updateHumanHand();

        boolean isFirstRound = engine.isFirstRound();
        boolean humanHasBigJoker = engine.playerHasBigJoker(0);

        if (isFirstRound && !humanHasBigJoker) {
            // First round: human has no Big Joker, skip to AI declaration
            statusLabel.setText("第一局需要亮大王定庄，你没有大王，电脑正在考虑...");
            actionPane.getChildren().clear();
            humanPassTrump();
            return;
        }

        if (isFirstRound) {
            // First round: show "亮王" button with random suit selection
            statusLabel.setText("第一局：你有大王，点击亮王按钮要主（花色随机）");
            actionPane.getChildren().clear();

            Button declareBtn = new Button("亮王");
            declareBtn.setStyle("-fx-font-size: 16px; -fx-padding: 8 20; "
                + "-fx-background-color: #d4af37; -fx-text-fill: black; -fx-font-weight: bold;");
            declareBtn.setOnAction(e -> {
                actionPane.getChildren().clear();
                Suit randomSuit = engine.declareTrumpRandomSuit(0);
                statusLabel.setText("你亮王定庄，随机主牌：" + randomSuit.getSymbol() + randomSuit.getDisplayName());
                updateInfoPanel();
                beginKittySelection();
            });
            actionPane.getChildren().add(declareBtn);

            Button passBtn = new Button("不叫");
            passBtn.setStyle("-fx-font-size: 14px; -fx-padding: 8 16; "
                + "-fx-background-color: #888888; -fx-text-fill: white;");
            passBtn.setOnAction(e -> humanPassTrump());
            actionPane.getChildren().add(passBtn);
            return;
        }

        statusLabel.setText("请选择主牌花色或不叫");

        Rank humanTrumpRank = engine.getTeamLevels()[players[0].getTeam()];
        Map<Suit, Integer> suitCounts = new EnumMap<>(Suit.class);
        for (Card card : players[0].getHand()) {
            if (card.getRank() == humanTrumpRank && card.getSuit() != null) {
                suitCounts.merge(card.getSuit(), 1, Integer::sum);
            }
        }

        actionPane.getChildren().clear();
        for (Suit suit : Suit.values()) {
            int count = suitCounts.getOrDefault(suit, 0);
            Button btn = new Button("叫" + suit.getSymbol() + suit.getDisplayName());
            String suitColor = suit.getColor().equals("red") ? "#cc0000" : "#333333";
            btn.setStyle("-fx-font-size: 14px; -fx-padding: 8 16; "
                + "-fx-background-color: white; -fx-text-fill: " + suitColor + "; -fx-font-weight: bold;");
            btn.setDisable(count < 2);
            final Suit s = suit;
            btn.setOnAction(e -> humanDeclareTrump(s));
            actionPane.getChildren().add(btn);
        }

        Button passBtn = new Button("不叫");
        passBtn.setStyle("-fx-font-size: 14px; -fx-padding: 8 16; "
            + "-fx-background-color: #888888; -fx-text-fill: white;");
        passBtn.setOnAction(e -> humanPassTrump());
        actionPane.getChildren().add(passBtn);
    }

    private void humanDeclareTrump(Suit suit) {
        actionPane.getChildren().clear();
        engine.declareTrump(0, suit);
        statusLabel.setText("你叫了主牌：" + suit.getSymbol() + suit.getDisplayName());
        updateInfoPanel();
        beginKittySelection();
    }

    private void humanPassTrump() {
        actionPane.getChildren().clear();
        statusLabel.setText("你选择了不叫，电脑正在考虑...");
        boolean isFirstRound = engine.isFirstRound();

        Timeline timeline = new Timeline();
        boolean[] declared = {false};

        for (int i = 1; i <= 3; i++) {
            final int idx = i;
            KeyFrame kf = new KeyFrame(Duration.millis(500.0 * i), e -> {
                if (declared[0]) return;
                Player p = players[idx];

                if (isFirstRound) {
                    // First round: only players with Big Joker can declare with random suit
                    if (!engine.playerHasBigJoker(idx)) {
                        statusLabel.setText(p.getName() + " 没有大王，不叫");
                        return;
                    }
                    // Declare with random suit
                    declared[0] = true;
                    Suit randomSuit = engine.declareTrumpRandomSuit(idx);
                    statusLabel.setText(p.getName() + " 亮王定庄，随机主牌："
                        + randomSuit.getSymbol() + randomSuit.getDisplayName());
                    updateInfoPanel();
                    updateHumanHand();
                    handleAIKitty(idx);
                    return;
                }

                Rank trumpRank = engine.getTeamLevels()[p.getTeam()];
                Suit chosenSuit = aiStrategy.chooseTrumpSuit(p, trumpRank);
                if (chosenSuit != null) {
                    declared[0] = true;
                    engine.declareTrump(idx, chosenSuit);
                    statusLabel.setText(p.getName() + " 叫了主牌："
                        + chosenSuit.getSymbol() + chosenSuit.getDisplayName());
                    updateInfoPanel();
                    updateHumanHand();
                    handleAIKitty(idx);
                } else {
                    statusLabel.setText(p.getName() + " 不叫");
                }
            });
            timeline.getKeyFrames().add(kf);
        }

        // If nobody declared after all AIs tried
        KeyFrame finalKf = new KeyFrame(Duration.millis(2000), e -> {
            if (!declared[0]) {
                if (isFirstRound) {
                    // First round fallback: determine trump from kitty
                    statusLabel.setText("无人叫牌，从底牌确定主牌花色...");
                    Timeline kittyDelay = new Timeline(new KeyFrame(Duration.millis(1000), ev -> {
                        int newDealer = engine.declareTrumpFromKitty();
                        TrumpInfo ti = engine.getTrumpInfo();
                        statusLabel.setText("底牌确定主牌：" + ti.getTrumpSuit().getSymbol()
                            + ti.getTrumpSuit().getDisplayName() + "，庄家：" + players[newDealer].getName());
                        updateInfoPanel();
                        updateHumanHand();
                        if (newDealer == 0) {
                            beginKittySelection();
                        } else {
                            handleAIKitty(newDealer);
                        }
                    }));
                    kittyDelay.play();
                } else {
                    statusLabel.setText("无人叫牌，重新发牌...");
                    Timeline redeal = new Timeline(new KeyFrame(Duration.millis(1500), ev -> startNewRound()));
                    redeal.play();
                }
            }
        });
        timeline.getKeyFrames().add(finalKf);
        timeline.play();
    }

    // ======================== Kitty Management ========================

    private void beginKittySelection() {
        waitingForKitty = true;
        selectedKittyCards.clear();
        statusLabel.setText("请选择10张牌作为底牌（已选：0/10）（扣王：大王=2血，小王=1血）");

        Button confirmBtn = new Button("确认");
        confirmBtn.setStyle("-fx-font-size: 16px; -fx-padding: 8 20; "
            + "-fx-background-color: #d4af37; -fx-text-fill: black; -fx-font-weight: bold;");
        confirmBtn.setDisable(true);
        confirmBtn.setOnAction(e -> confirmKitty());

        actionPane.getChildren().clear();
        actionPane.getChildren().add(confirmBtn);

        updateHumanHand();
    }

    private void toggleKittyCard(Card card) {
        if (selectedKittyCards.contains(card)) {
            selectedKittyCards.remove(card);
        } else if (selectedKittyCards.size() < 10) {
            selectedKittyCards.add(card);
        }
        statusLabel.setText("请选择10张牌作为底牌（已选：" + selectedKittyCards.size() + "/10）（扣王：大王=2血，小王=1血）");

        if (!actionPane.getChildren().isEmpty()
            && actionPane.getChildren().get(0) instanceof Button btn) {
            btn.setDisable(selectedKittyCards.size() != 10);
        }
        updateHumanHand();
    }

    private void confirmKitty() {
        waitingForKitty = false;
        actionPane.getChildren().clear();
        engine.setKitty(new ArrayList<>(selectedKittyCards));
        selectedKittyCards.clear();
        statusLabel.setText("底牌已确认，游戏开始！");
        updateHumanHand();
        updateAIPlayerPanes();
        updateInfoPanel();

        Timeline delay = new Timeline(new KeyFrame(Duration.millis(500), e -> processCurrentPlayer()));
        delay.play();
    }

    private void handleAIKitty(int dealerIndex) {
        Timeline delay = new Timeline(new KeyFrame(Duration.millis(800), e -> {
            Player dealer = players[dealerIndex];
            List<Card> kittyCards = aiStrategy.chooseKittyCards(dealer, engine.getKitty(), engine.getTrumpInfo());
            engine.setKitty(kittyCards);
            updateHumanHand();
            updateAIPlayerPanes();
            statusLabel.setText("游戏开始！");

            Timeline startDelay = new Timeline(new KeyFrame(Duration.millis(500), ev -> processCurrentPlayer()));
            startDelay.play();
        }));
        delay.play();
    }

    // ======================== Playing Phase ========================

    private void processCurrentPlayer() {
        if (engine.getPhase() != GamePhase.PLAYING) return;

        int currentIdx = engine.getCurrentPlayerIndex();
        Player currentPlayer = players[currentIdx];
        updateCurrentPlayerHighlight(currentIdx);

        if (currentPlayer.isHuman()) {
            waitingForHumanPlay = true;
            updateHumanHand();
            statusLabel.setText("轮到你出牌");
        } else {
            waitingForHumanPlay = false;
            updateHumanHand();
            statusLabel.setText(currentPlayer.getName() + " 思考中...");
            Timeline timeline = new Timeline(new KeyFrame(Duration.millis(500), e -> {
                Card card = aiStrategy.chooseCard(currentPlayer, engine);
                executePlay(currentIdx, card);
            }));
            timeline.play();
        }
    }

    private void handleHumanCardClick(Card card) {
        if (!waitingForHumanPlay) return;
        if (!engine.isValidPlay(0, card)) return;

        waitingForHumanPlay = false;
        executePlay(0, card);
    }

    private void executePlay(int playerIndex, Card card) {
        // Track void suit before the engine updates state
        if (engine.getTrickCardsPlayed() > 0) {
            Card leadCard = engine.getCurrentTrick()[engine.getCurrentTrickLeader()];
            Suit leadSuit = engine.getTrumpInfo().getEffectiveSuit(leadCard);
            Suit cardSuit = engine.getTrumpInfo().getEffectiveSuit(card);
            if (leadSuit != null && leadSuit != cardSuit) {
                cardTracker.markVoidSuit(playerIndex, leadSuit);
            }
        }

        engine.playCard(playerIndex, card);
        cardTracker.cardPlayed(card, playerIndex);

        updateTrickArea();
        updateHumanHand();
        updateAIPlayerPanes();

        if (engine.getTrickCardsPlayed() == 4) {
            // Trick complete — pause to let player see the cards, then evaluate
            Timeline pause = new Timeline(new KeyFrame(Duration.millis(1000), e -> completeTrick()));
            pause.play();
        } else {
            processCurrentPlayer();
        }
    }

    private void completeTrick() {
        int winner = engine.evaluateTrick();
        updateInfoPanel();

        if (trackerSection != null && trackerSection.isVisible()) {
            updateTrackerPanel();
        }

        if (engine.isRoundOver()) {
            showRoundResult();
        } else {
            statusLabel.setText(players[winner].getName() + " 赢得此墩");
            // Animate trick collection, then proceed to next trick
            Timeline pause = new Timeline(new KeyFrame(Duration.millis(300), e ->
                animateTrickCollection(() -> {
                    Timeline next = new Timeline(new KeyFrame(Duration.millis(200), ev -> processCurrentPlayer()));
                    next.play();
                })
            ));
            pause.play();
        }
    }

    // ======================== Round Result ========================

    private void showRoundResult() {
        RoundResult result = engine.calculateRoundResult();
        clearTrickArea();

        String resultText;
        String winner = (result.getWinningTeam() == players[0].getTeam()) ? "你的队伍" : "对方队伍";
        resultText = winner + "获胜！升 " + result.getLevelChange() + " 级";

        // Advance team levels and check win condition
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
        statusLabel.setText(statusMsg.toString());

        actionPane.getChildren().clear();
        boolean finalGameWon = gameWon;
        Button nextBtn = new Button(gameWon ? "新游戏" : "下一局");
        nextBtn.setStyle("-fx-font-size: 18px; -fx-padding: 10 30; "
            + "-fx-background-color: #d4af37; -fx-text-fill: black; -fx-font-weight: bold; -fx-cursor: hand;");
        nextBtn.setOnAction(e -> {
            actionPane.getChildren().clear();
            if (finalGameWon) {
                Rank[] lvls = engine.getTeamLevels();
                lvls[0] = Rank.THREE;
                lvls[1] = Rank.THREE;
            }
            startNewRound();
        });
        actionPane.getChildren().add(nextBtn);

        updateInfoPanel();
    }

    // ======================== Card Rendering ========================

    private StackPane createCardFace(Card card) {
        StackPane cardPane = new StackPane();
        cardPane.setMinSize(CARD_WIDTH, CARD_HEIGHT);
        cardPane.setPrefSize(CARD_WIDTH, CARD_HEIGHT);
        cardPane.setMaxSize(CARD_WIDTH, CARD_HEIGHT);

        String color = getCardColor(card);
        cardPane.setStyle("-fx-background-color: white; -fx-background-radius: 6; "
            + "-fx-border-color: #999; -fx-border-radius: 6; -fx-border-width: 1; "
            + "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 3, 0, 1, 1);");

        // Top-left rank + suit
        String topText;
        if (card.getRank() == Rank.BIG_JOKER) {
            topText = "大\n王";
        } else if (card.getRank() == Rank.SMALL_JOKER) {
            topText = "小\n王";
        } else {
            topText = card.getSuit().getSymbol() + "\n" + card.getRank().getDisplayName();
        }
        Label topLeft = new Label(topText);
        topLeft.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 12px; "
            + "-fx-font-weight: bold; -fx-line-spacing: -3;");
        StackPane.setAlignment(topLeft, Pos.TOP_LEFT);
        StackPane.setMargin(topLeft, new Insets(3));
        cardPane.getChildren().add(topLeft);

        // Center suit symbol
        if (card.getSuit() != null) {
            Label centerSymbol = new Label(card.getSuit().getSymbol());
            centerSymbol.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 22px;");
            StackPane.setAlignment(centerSymbol, Pos.CENTER);
            cardPane.getChildren().add(centerSymbol);
        } else {
            // Joker center text
            String jokerText = card.getRank() == Rank.BIG_JOKER ? "★" : "☆";
            Label centerSymbol = new Label(jokerText);
            centerSymbol.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 24px;");
            StackPane.setAlignment(centerSymbol, Pos.CENTER);
            cardPane.getChildren().add(centerSymbol);
        }

        return cardPane;
    }

    private StackPane createCardBack() {
        StackPane cardPane = new StackPane();
        cardPane.setMinSize(CARD_WIDTH, CARD_HEIGHT);
        cardPane.setPrefSize(CARD_WIDTH, CARD_HEIGHT);
        cardPane.setMaxSize(CARD_WIDTH, CARD_HEIGHT);
        cardPane.setStyle("-fx-background-color: #2255aa; -fx-background-radius: 6; "
            + "-fx-border-color: white; -fx-border-radius: 6; -fx-border-width: 1;");
        return cardPane;
    }

    private String getCardColor(Card card) {
        if (card.getRank() == Rank.BIG_JOKER) return "#cc0000";
        if (card.getRank() == Rank.SMALL_JOKER) return "#333333";
        if (card.getSuit() == null) return "#333333";
        return card.getSuit().getColor().equals("red") ? "#cc0000" : "#333333";
    }

    // ======================== UI Updates ========================

    private void updateHumanHand() {
        humanHandPane.getChildren().clear();
        List<Card> hand = sortForDisplay(players[0].getHand(), engine.getTrumpInfo());

        double startX = 10;
        for (int i = 0; i < hand.size(); i++) {
            Card card = hand.get(i);
            StackPane cardNode = createCardFace(card);

            boolean isSelected = selectedKittyCards.contains(card);
            boolean isValid = waitingForHumanPlay && engine.isValidPlay(0, card);
            boolean isInvalid = waitingForHumanPlay && !engine.isValidPlay(0, card);

            double baseY = isSelected ? 5.0 : 25.0;
            if (isInvalid) {
                cardNode.setOpacity(0.5);
            }

            cardNode.setLayoutX(startX + i * CARD_OVERLAP);
            cardNode.setLayoutY(baseY);

            final Card c = card;
            final StackPane node = cardNode;
            cardNode.setOnMouseEntered(e -> {
                if ((waitingForHumanPlay && engine.isValidPlay(0, c)) || waitingForKitty) {
                    node.setTranslateY(-10);
                    node.setCursor(Cursor.HAND);
                }
            });
            cardNode.setOnMouseExited(e -> {
                node.setTranslateY(0);
                node.setCursor(Cursor.DEFAULT);
            });
            cardNode.setOnMouseClicked(e -> {
                if (waitingForHumanPlay) {
                    handleHumanCardClick(c);
                } else if (waitingForKitty) {
                    toggleKittyCard(c);
                }
            });

            humanHandPane.getChildren().add(cardNode);
        }

        humanHandPane.setPrefWidth(startX + hand.size() * CARD_OVERLAP + CARD_WIDTH);
        playerCountLabels[0].setText("牌数：" + players[0].getHand().size());
    }

    private void updateTrickArea() {
        Card[] trick = engine.getCurrentTrick();
        for (int i = 0; i < 4; i++) {
            if (trick[i] != null && trickCardNodes[i].getChildren().isEmpty()) {
                // Only add card if the slot is empty (newly played card)
                StackPane cardFace = createCardFace(trick[i]);
                trickCardNodes[i].getChildren().add(cardFace);
                animateCardPlay(cardFace, i);
            } else if (trick[i] == null) {
                trickCardNodes[i].getChildren().clear();
            }
        }
    }

    private void animateCardPlay(Node cardNode, int playerIndex) {
        // Card appears with a scale-up + fade-in + slide from the player direction
        cardNode.setOpacity(0);
        cardNode.setScaleX(0.5);
        cardNode.setScaleY(0.5);

        // Slide direction based on player position
        double startTranslateX = 0;
        double startTranslateY = 0;
        switch (playerIndex) {
            case 0 -> startTranslateY = 60;   // human (bottom) slides up
            case 1 -> startTranslateX = -60;  // left player slides right
            case 2 -> startTranslateY = -60;  // top player slides down
            case 3 -> startTranslateX = 60;   // right player slides left
        }
        cardNode.setTranslateX(startTranslateX);
        cardNode.setTranslateY(startTranslateY);

        FadeTransition fade = new FadeTransition(Duration.millis(250), cardNode);
        fade.setFromValue(0);
        fade.setToValue(1);

        ScaleTransition scale = new ScaleTransition(Duration.millis(250), cardNode);
        scale.setFromX(0.5);
        scale.setFromY(0.5);
        scale.setToX(1.0);
        scale.setToY(1.0);
        scale.setInterpolator(Interpolator.EASE_OUT);

        TranslateTransition slide = new TranslateTransition(Duration.millis(250), cardNode);
        slide.setFromX(startTranslateX);
        slide.setFromY(startTranslateY);
        slide.setToX(0);
        slide.setToY(0);
        slide.setInterpolator(Interpolator.EASE_OUT);

        ParallelTransition pt = new ParallelTransition(fade, scale, slide);
        pt.play();
    }

    private void clearTrickArea() {
        animateTrickCollection(null);
    }

    private void animateTrickCollection(Runnable onComplete) {
        List<Node> cardsToRemove = new ArrayList<>();
        boolean hasCards = false;
        for (int i = 0; i < 4; i++) {
            if (!trickCardNodes[i].getChildren().isEmpty()) {
                hasCards = true;
                cardsToRemove.addAll(trickCardNodes[i].getChildren());
            }
        }
        if (!hasCards) {
            if (onComplete != null) onComplete.run();
            return;
        }

        ParallelTransition allFade = new ParallelTransition();
        for (Node card : cardsToRemove) {
            FadeTransition fade = new FadeTransition(Duration.millis(300), card);
            fade.setFromValue(1);
            fade.setToValue(0);

            ScaleTransition shrink = new ScaleTransition(Duration.millis(300), card);
            shrink.setToX(0.3);
            shrink.setToY(0.3);
            shrink.setInterpolator(Interpolator.EASE_IN);

            allFade.getChildren().add(new ParallelTransition(fade, shrink));
        }
        allFade.setOnFinished(e -> {
            for (int i = 0; i < 4; i++) {
                trickCardNodes[i].getChildren().clear();
            }
            if (onComplete != null) onComplete.run();
        });
        allFade.play();
    }

    private void updateAIPlayerPanes() {
        for (int i = 1; i <= 3; i++) {
            playerCountLabels[i].setText("牌数：" + players[i].getHand().size());
        }
    }

    private void updateInfoPanel() {
        roundLabel.setText("第 " + engine.getRoundNumber() + " 局");

        TrumpInfo ti = engine.getTrumpInfo();
        if (ti != null) {
            String suitName = ti.getTrumpSuit() != null
                ? ti.getTrumpSuit().getSymbol() + ti.getTrumpSuit().getDisplayName()
                : "无";
            trumpLabel.setText("主牌：" + suitName + " " + ti.getTrumpRank().getDisplayName());
        } else {
            trumpLabel.setText("主牌：未定");
        }

        scoreLabel.setText("防守方得分：" + engine.getDefenderPoints());

        // Update dealer label and player name labels with dealer indicator
        if (engine.getTrumpInfo() != null) {
            int di = engine.getDealerIndex();
            dealerLabel.setText("庄家：" + players[di].getName());
            updateDealerIndicator(di);
        } else {
            dealerLabel.setText("庄家：未定");
        }

        Rank[] levels = engine.getTeamLevels();
        teamLevelLabel.setText("你的队伍：" + levels[0].getDisplayName()
            + " | 对方队伍：" + levels[1].getDisplayName());
    }

    private void updateCurrentPlayerHighlight(int currentIdx) {
        for (int i = 0; i < 4; i++) {
            if (playerNameLabels[i] == null) continue;
            if (i == currentIdx) {
                playerNameLabels[i].setStyle("-fx-text-fill: #ffdd57; -fx-font-size: 14px; "
                    + "-fx-font-weight: bold; -fx-underline: true;");
            } else {
                playerNameLabels[i].setStyle("-fx-text-fill: white; -fx-font-size: 14px; "
                    + "-fx-font-weight: bold;");
            }
        }
    }

    private void updateDealerIndicator(int dealerIdx) {
        for (int i = 0; i < 4; i++) {
            if (playerNameLabels[i] == null) continue;
            String baseName = players[i].getName();
            playerNameLabels[i].setText(i == dealerIdx ? baseName + " [庄]" : baseName);
        }
    }

    private void updateTrackerPanel() {
        if (trackerGrid == null) return;
        trackerGrid.getChildren().clear();

        // Header row
        trackerGrid.add(createTrackerCell("牌面", true), 0, 0);
        int col = 1;
        for (Suit suit : Suit.values()) {
            Label header = createTrackerCell(suit.getSymbol(), true);
            String c = suit.getColor().equals("red") ? "#ff6666" : "white";
            header.setStyle(header.getStyle().replace("-fx-text-fill: white", "-fx-text-fill: " + c));
            trackerGrid.add(header, col++, 0);
        }

        // One row per non-joker rank
        int row = 1;
        for (Rank rank : Rank.values()) {
            if (rank == Rank.SMALL_JOKER || rank == Rank.BIG_JOKER) continue;
            trackerGrid.add(createTrackerCell(rank.getDisplayName(), false), 0, row);
            col = 1;
            for (Suit suit : Suit.values()) {
                int remaining = remainingBySuitAndRank(suit, rank);
                Label cell = createTrackerCell(String.valueOf(remaining), false);
                if (remaining == 0) {
                    cell.setStyle(cell.getStyle().replace("-fx-text-fill: white", "-fx-text-fill: #666"));
                }
                trackerGrid.add(cell, col++, row);
            }
            row++;
        }

        // Jokers
        int smallRemaining = jokerRemaining(Rank.SMALL_JOKER);
        trackerGrid.add(createTrackerCell("小王", false), 0, row);
        trackerGrid.add(createTrackerCell(String.valueOf(smallRemaining), false), 1, row);
        row++;

        int bigRemaining = jokerRemaining(Rank.BIG_JOKER);
        trackerGrid.add(createTrackerCell("大王", false), 0, row);
        trackerGrid.add(createTrackerCell(String.valueOf(bigRemaining), false), 1, row);
        row++;

        // Total
        trackerGrid.add(createTrackerCell("总计", true), 0, row);
        trackerGrid.add(createTrackerCell(String.valueOf(cardTracker.getRemainingCardCount()), true), 1, row);
    }

    private Label createTrackerCell(String text, boolean bold) {
        Label label = new Label(text);
        String style = "-fx-text-fill: white; -fx-font-size: 11px; -fx-padding: 1 4;";
        if (bold) style += " -fx-font-weight: bold;";
        label.setStyle(style);
        return label;
    }

    // ======================== Utility ========================

    private List<Card> sortForDisplay(List<Card> hand, TrumpInfo trumpInfo) {
        List<Card> sorted = new ArrayList<>(hand);
        if (trumpInfo == null) {
            sorted.sort(Comparator
                .comparingInt((Card c) -> c.getSuit() == null ? 99 : c.getSuit().ordinal())
                .thenComparing(Comparator.comparingInt((Card c) -> c.getRank().getValue()).reversed()));
            return sorted;
        }
        sorted.sort((a, b) -> {
            boolean aT = trumpInfo.isTrump(a);
            boolean bT = trumpInfo.isTrump(b);
            if (aT != bT) return aT ? -1 : 1;
            if (aT) {
                return Integer.compare(trumpInfo.getCardStrength(b), trumpInfo.getCardStrength(a));
            }
            int suitCmp = Integer.compare(
                a.getSuit() == null ? 99 : a.getSuit().ordinal(),
                b.getSuit() == null ? 99 : b.getSuit().ordinal());
            if (suitCmp != 0) return suitCmp;
            return Integer.compare(b.getRank().getValue(), a.getRank().getValue());
        });
        return sorted;
    }

    private int remainingBySuitAndRank(Suit suit, Rank rank) {
        int played = 0;
        for (Card c : cardTracker.getPlayedCards()) {
            if (c.getSuit() == suit && c.getRank() == rank) {
                played++;
            }
        }
        return 2 - played;
    }

    private int jokerRemaining(Rank jokerRank) {
        int played = 0;
        for (Card c : cardTracker.getPlayedCards()) {
            if (c.getRank() == jokerRank) {
                played++;
            }
        }
        return 2 - played;
    }
}
