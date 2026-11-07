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

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.tlcsdm.game.daliandagunzifx.DaGunZiGame;
import com.tlcsdm.game.daliandagunzifx.ai.AILevel;
import com.tlcsdm.game.daliandagunzifx.config.AppConfig;
import com.tlcsdm.game.daliandagunzifx.ui.Assets;
import com.tlcsdm.game.daliandagunzifx.ui.BaseStageScreen;
import com.tlcsdm.game.daliandagunzifx.ui.Dialogs;
import com.tlcsdm.game.daliandagunzifx.ui.UiUtils;
import com.tlcsdm.game.daliandagunzifx.utils.UpdateChecker;

/**
 * 主菜单 / 欢迎界面：选择 AI 难度与各项设置后开始游戏。
 *
 * @author unknowIfGuestInDream
 */
public class MenuScreen extends BaseStageScreen {

    public MenuScreen(DaGunZiGame game) {
        super(game);
        build();
        if (AppConfig.getInstance().isCheckUpdateEnabled() && !UpdateChecker.hasCheckedToday()) {
            checkForUpdate(true);
        }
    }

    private void build() {
        stage.clear();
        AppConfig cfg = AppConfig.getInstance();

        Table root = new Table();
        root.setFillParent(true);
        stage.addActor(root);

        Label title = new Label("大连打滚子", assets.getTitleStyle());
        Label subtitle = new Label("Da Gunzi Card Game · v" + DaGunZiGame.APP_VERSION,
            assets.getLabelStyleSmall());

        TextButton difficultyBtn = new TextButton("", assets.getButtonStyle());
        difficultyBtn.setText("AI难度：" + cfg.getAiLevel().getDisplayName());
        UiUtils.onClick(difficultyBtn, () -> {
            AILevel[] levels = AILevel.values();
            AILevel next = levels[(cfg.getAiLevel().ordinal() + 1) % levels.length];
            cfg.setAiLevel(next);
            difficultyBtn.setText("AI难度：" + next.getDisplayName());
        });

        TextButton trackerBtn = toggleButton("记牌器", cfg.isTrackerEnabled(),
            cfg::setTrackerEnabled);
        TextButton themeBtn = toggleButton("深色主题", cfg.isDarkTheme(), cfg::setDarkTheme);
        TextButton liveBangBtn = toggleButton("活棒子", cfg.isLiveBang(), cfg::setLiveBang);
        TextButton aggressiveBtn = toggleButton("激进AI", cfg.isAggressive(), cfg::setAggressive);
        TextButton updateBtn = toggleButton("启动检查更新", cfg.isCheckUpdateEnabled(),
            cfg::setCheckUpdateEnabled);

        TextButton startBtn = UiUtils.button("开始游戏", assets.getButtonStyle(),
            () -> game.switchScreen(new GameScreen(game)));

        TextButton rulesBtn = UiUtils.button("游戏规则", assets.getButtonStyle(),
            () -> game.setScreen(new RulesScreen(game, this)));
        TextButton checkUpdateBtn = UiUtils.button("检查更新", assets.getButtonStyle(),
            () -> checkForUpdate(false));
        TextButton aboutBtn = UiUtils.button("关于", assets.getButtonStyle(), this::showAbout);
        TextButton exitBtn = UiUtils.button("退出", assets.getButtonStyle(),
            () -> Gdx.app.exit());

        root.add(title).padBottom(6).row();
        root.add(subtitle).padBottom(24).row();
        root.add(difficultyBtn).width(320).height(44).pad(5).row();
        root.add(trackerBtn).width(320).height(40).pad(4).row();
        root.add(themeBtn).width(320).height(40).pad(4).row();
        root.add(liveBangBtn).width(320).height(40).pad(4).row();
        root.add(aggressiveBtn).width(320).height(40).pad(4).row();
        root.add(updateBtn).width(320).height(40).pad(4).row();
        root.add(startBtn).width(320).height(54).padTop(16).padBottom(14).row();

        Table bottomRow = new Table();
        bottomRow.add(rulesBtn).width(120).height(40).pad(4);
        bottomRow.add(checkUpdateBtn).width(120).height(40).pad(4);
        bottomRow.add(aboutBtn).width(90).height(40).pad(4);
        bottomRow.add(exitBtn).width(90).height(40).pad(4);
        root.add(bottomRow).row();
    }

    private TextButton toggleButton(String name, boolean initial, java.util.function.Consumer<Boolean> setter) {
        TextButton btn = new TextButton(label(name, initial), assets.getButtonStyle());
        final boolean[] state = {initial};
        UiUtils.onClick(btn, () -> {
            state[0] = !state[0];
            setter.accept(state[0]);
            btn.setText(label(name, state[0]));
        });
        return btn;
    }

    private String label(String name, boolean on) {
        return name + "：" + (on ? "开" : "关");
    }

    private void showAbout() {
        String msg = "大连打滚子 v" + DaGunZiGame.APP_VERSION + "\n\n"
            + "一款 3 副牌、4 人 2 队的升级类扑克牌游戏。\n"
            + "2.0.0 使用 libgdx 重写界面与动画。\n\n"
            + "项目主页：github.com/unknowIfGuestInDream/dalian-dagunzi-fx";
        Dialogs.showMessage(stage, assets, "关于", msg);
    }

    /**
     * 异步检查更新：网络请求在后台线程执行，结果回到 GL 线程展示。
     *
     * @param silent 为 true 时启动自检，无更新不提示
     */
    private void checkForUpdate(boolean silent) {
        new Thread(() -> {
            UpdateChecker.UpdateInfo info = UpdateChecker.checkForUpdate(DaGunZiGame.APP_VERSION);
            UpdateChecker.markCheckedToday();
            Gdx.app.postRunnable(() -> {
                if (info != null) {
                    String notes = info.releaseNotes();
                    if (notes != null && notes.length() > 500) {
                        notes = notes.substring(0, 500) + "...";
                    }
                    Dialogs.showMessage(stage, assets, "发现新版本 " + info.latestVersion(),
                        (notes == null || notes.isBlank() ? "" : notes + "\n\n")
                            + "下载地址：" + UpdateChecker.getReleasesPageUrl());
                } else if (!silent) {
                    Dialogs.showMessage(stage, assets, "检查更新", "当前已是最新版本。");
                }
            });
        }, "update-checker").start();
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        build();
    }
}
