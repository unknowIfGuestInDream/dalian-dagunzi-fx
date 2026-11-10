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
package com.tlcsdm.game.daliandagunzifx;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Screen;
import com.tlcsdm.game.daliandagunzifx.config.AppConfig;
import com.tlcsdm.game.daliandagunzifx.screen.MenuScreen;
import com.tlcsdm.game.daliandagunzifx.ui.Assets;

/**
 * libgdx 主游戏类，负责加载全局资源并在各界面之间切换。
 * <p>
 * 2.0.0 起使用 libgdx（Scene2D）重写原 JavaFX 界面，核心引擎/AI/模型逻辑完全复用。
 *
 * @author unknowIfGuestInDream
 */
public class DaGunZiGame extends Game {

    /** 应用版本号，与构建版本保持一致。 */
    public static final String APP_VERSION = "2.0.0";

    private Assets assets;

    @Override
    public void create() {
        assets = new Assets();
        assets.load();
        setScreen(new MenuScreen(this));
    }

    public Assets getAssets() {
        return assets;
    }

    public AppConfig getConfig() {
        return AppConfig.getInstance();
    }

    /**
     * 切换界面并释放上一个界面的资源，避免内存泄漏。
     */
    public void switchScreen(Screen next) {
        Screen current = getScreen();
        setScreen(next);
        if (current != null) {
            current.dispose();
        }
    }

    @Override
    public void dispose() {
        if (getScreen() != null) {
            getScreen().dispose();
        }
        if (assets != null) {
            assets.dispose();
        }
    }
}
