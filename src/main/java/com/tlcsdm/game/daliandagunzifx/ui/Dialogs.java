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
package com.tlcsdm.game.daliandagunzifx.ui;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window;

/**
 * 轻量模态对话框工具：在 Scene2D 舞台上居中显示信息或确认框。
 *
 * @author unknowIfGuestInDream
 */
public final class Dialogs {

    private Dialogs() {
    }

    /** 显示仅含“确定”按钮的信息对话框。 */
    public static void showMessage(Stage stage, Assets assets, String title, String message) {
        Window window = baseWindow(stage, assets, title, message);
        TextButton ok = UiUtils.button("确定", assets.getButtonStyle(), window::remove);
        window.add(ok).width(120).pad(10);
        finish(stage, window);
    }

    /** 显示“确定/取消”确认对话框，确认时执行回调。 */
    public static void showConfirm(Stage stage, Assets assets, String title, String message,
            Runnable onConfirm) {
        Window window = baseWindow(stage, assets, title, message);
        TextButton ok = UiUtils.button("确定", assets.getButtonStyle(), () -> {
            window.remove();
            onConfirm.run();
        });
        TextButton cancel = UiUtils.button("取消", assets.getButtonStyle(), window::remove);
        window.add(ok).width(110).pad(10);
        window.add(cancel).width(110).pad(10);
        finish(stage, window);
    }

    private static Window baseWindow(Stage stage, Assets assets, String title, String message) {
        Window window = new Window(title, assets.getWindowStyle());
        window.setModal(true);
        window.setMovable(false);
        window.getTitleLabel().setAlignment(com.badlogic.gdx.utils.Align.center);
        window.padTop(44).padBottom(16).padLeft(20).padRight(20);
        Label label = new Label(message, assets.getLabelStyle());
        label.setWrap(true);
        window.add(label).width(420).pad(16).colspan(2).row();
        return window;
    }

    private static void finish(Stage stage, Window window) {
        window.pack();
        if (window.getWidth() < 320) {
            window.setWidth(320);
        }
        window.setPosition(
            Math.round((stage.getWidth() - window.getWidth()) / 2f),
            Math.round((stage.getHeight() - window.getHeight()) / 2f));
        stage.addActor(window);
    }
}
