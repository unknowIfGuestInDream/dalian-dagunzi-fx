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

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

/**
 * Utility class for displaying game rules in dialog windows.
 *
 * @author unknowIfGuestInDream
 */
public final class RulesDialog {

    private static final int FIRST_COL_WIDTH = 80;
    private static final String CELL_STYLE = "-fx-background-color: white; -fx-font-size: 13px;";
    private static final String CELL_BOLD_STYLE = "-fx-background-color: white; -fx-font-size: 13px; -fx-font-weight: bold;";

    private RulesDialog() {
    }

    /**
     * Shows the terminology (名词解释) rules dialog.
     */
    public static void showTerminology(Window owner) {
        String[][] data = {
            {"棒子", "一对相同花色的相同牌，如2张红桃5"},
            {"滚子", "3张相同花色的相同牌，如3张红桃5"},
            {"庄家", "收底玩家，第一局谁叫到主就谁庄，之后在赢方互换，赢方若输，则庄家为上局庄的下家。"},
            {"反庄", "抓分方得到超过120分，则下一局反庄"},
            {"扣王", "把王放进底牌，即为扣王，一张大王升2级并且吃2个贡，一张小王升1级并且吃一个贡，扣王则可以看到底牌，用王换掉一张最小的牌"},
            {"升级", "跑分方让抓分方得到低于120分，则升1级，再加上扣王的级，1张大王升2级，1张小王升1级；抓分方抓到超过120分，并且抠底，则升一级，再加上扣王的级，若超过120分，但是没抠底，则不升级，只换庄。"},
            {"喝血", "即为进贡，扣王失败（抓分方扣王但没抓到120分，或者跑分方扣王让抓分方抓到超过120分，即算扣王失败）1扣一张大王要进2张贡，1张小王进1张贡，下一局由扣王玩家进给庄家。另外抓分方得到超过150分，每多10分就多吃一张贡，若抓分方抓到低于80分，则少抓10分多进一张贡出去。"},
        };
        showTableDialog(owner, "名词解释", new String[]{"名词", "解释说明"}, data, 500);
    }

    /**
     * Shows the card types (牌型) rules dialog.
     */
    public static void showCardTypes(Window owner) {
        String[][] data = {
            {"单张", "任意一张牌"},
            {"棒子（对子）", "两张相同花色且相同点数的牌，如两张红桃5"},
            {"滚子（三条）", "三张相同花色且相同点数的牌，如三张红桃5"},
            {"连棒（拖拉机）", "两对或以上相邻点数的棒子，如红桃55+红桃66。主牌中跳过非主牌点数。"},
            {"连滚", "两组或以上相邻点数的滚子，如红桃555+红桃666"},
            {"甩牌", "同花色中最大的散牌组合，如果被其他玩家管住则无效"},
        };
        showTableDialog(owner, "牌型", new String[]{"牌型", "说明"}, data, 500);
    }

    /**
     * Shows the game flow (流程) rules dialog.
     */
    public static void showGameFlow(Window owner) {
        String[][] data = {
            {"发牌", "3副牌共162张，每人38张，10张底牌"},
            {"叫主", "第一局需要有大王才能叫主；之后每局需要有2张或以上当前级别的主牌才能叫主"},
            {"埋底", "庄家从底牌中选10张牌埋入底牌，可以扣王（大王=2血，小王=1血）"},
            {"出牌", "从庄家开始，按顺序出牌。必须跟领出的花色，没有则可以出任意牌。主牌最大者赢得此墩。"},
            {"计分", "5分=5分，10分=10分，K=10分，每局共300分。抓分方得分<120分庄家升级，≥120分抓分方升级。"},
            {"升级", "从3开始，打碎10获胜。升级数取决于得分差距和扣王情况。"},
            {"进贡", "得分<80分时，每少10分多进一个贡；得分>160分时，每多10分多吃一个血。"},
        };
        showTableDialog(owner, "游戏流程", new String[]{"阶段", "说明"}, data, 500);
    }

    private static void showTableDialog(Window owner, String title, String[] headers, String[][] data, int width) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) {
            dialog.initOwner(owner);
        }
        dialog.setTitle(title);

        GridPane grid = new GridPane();
        grid.setHgap(1);
        grid.setVgap(1);
        grid.setPadding(new Insets(10));
        grid.setStyle("-fx-background-color: #cccccc;");

        // Header row
        int contentColWidth = width - FIRST_COL_WIDTH - 20;
        for (int c = 0; c < headers.length; c++) {
            Label header = new Label(headers[c]);
            header.setWrapText(true);
            header.setPadding(new Insets(8));
            header.setMaxWidth(Double.MAX_VALUE);
            header.setStyle("-fx-background-color: #e0e0e0; -fx-font-weight: bold; -fx-font-size: 14px;");
            if (c == 0) {
                header.setMinWidth(FIRST_COL_WIDTH);
                header.setPrefWidth(FIRST_COL_WIDTH);
            } else {
                header.setMinWidth(contentColWidth);
                header.setPrefWidth(contentColWidth);
            }
            grid.add(header, c, 0);
        }

        // Data rows
        for (int r = 0; r < data.length; r++) {
            for (int c = 0; c < data[r].length; c++) {
                Label cell = new Label(data[r][c]);
                cell.setWrapText(true);
                cell.setPadding(new Insets(8));
                cell.setMaxWidth(Double.MAX_VALUE);
                if (c == 0) {
                    cell.setMinWidth(FIRST_COL_WIDTH);
                    cell.setPrefWidth(FIRST_COL_WIDTH);
                    cell.setStyle(CELL_BOLD_STYLE);
                } else {
                    cell.setMinWidth(contentColWidth);
                    cell.setPrefWidth(contentColWidth);
                    cell.setStyle(CELL_STYLE);
                }
                grid.add(cell, c, r + 1);
            }
        }

        ScrollPane scrollPane = new ScrollPane(grid);
        scrollPane.setFitToWidth(true);

        VBox root = new VBox(scrollPane);
        root.setPadding(new Insets(5));

        Scene scene = new Scene(root, width, 450);
        dialog.setScene(scene);
        dialog.setResizable(true);
        dialog.showAndWait();
    }
}
