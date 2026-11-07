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

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Align;
import com.tlcsdm.game.daliandagunzifx.DaGunZiGame;
import com.tlcsdm.game.daliandagunzifx.ui.Assets;
import com.tlcsdm.game.daliandagunzifx.ui.BaseStageScreen;
import com.tlcsdm.game.daliandagunzifx.ui.RulesContent;
import com.tlcsdm.game.daliandagunzifx.ui.UiUtils;

/**
 * 游戏规则界面：以可滚动表格展示名词解释、牌型与游戏流程。
 *
 * @author unknowIfGuestInDream
 */
public class RulesScreen extends BaseStageScreen {

    private final Screen previous;

    public RulesScreen(DaGunZiGame game, Screen previous) {
        super(game);
        this.previous = previous;
        build();
    }

    private void build() {
        Table root = new Table();
        root.setFillParent(true);
        root.pad(16);
        stage.addActor(root);

        Label title = new Label("游戏规则", assets.getTitleStyle());
        root.add(title).padBottom(12).row();

        Table content = new Table();
        content.top();
        addSection(content, "名词解释", RulesContent.TERMINOLOGY);
        addSection(content, "牌型", RulesContent.CARD_TYPES);
        addSection(content, "游戏流程", RulesContent.GAME_FLOW);

        ScrollPane.ScrollPaneStyle spStyle = new ScrollPane.ScrollPaneStyle();
        ScrollPane scroll = new ScrollPane(content, spStyle);
        scroll.setScrollingDisabled(true, false);
        scroll.setFadeScrollBars(false);
        root.add(scroll).width(680).grow().row();

        TextButton back = UiUtils.button("返回", assets.getButtonStyle(), this::back);
        root.add(back).width(160).height(46).padTop(12).row();
    }

    private void addSection(Table content, String heading, String[][] rows) {
        Label h = new Label(heading, new Label.LabelStyle(assets.getFontMedium(), Assets.GOLD));
        content.add(h).left().colspan(2).padTop(14).padBottom(6).row();
        for (String[] row : rows) {
            Label term = new Label(row[0], assets.getLabelStyle());
            term.setAlignment(Align.topLeft);
            Label desc = new Label(row[1], assets.getLabelStyleSmall());
            desc.setWrap(true);
            content.add(term).width(110).top().left().pad(4);
            content.add(desc).width(540).top().left().pad(4).row();
        }
    }

    private void back() {
        game.setScreen(previous);
        dispose();
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        stage.clear();
        build();
    }
}
