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
package com.tlcsdm.game.daliandagunzifx.config;

import com.tlcsdm.game.daliandagunzifx.ai.AILevel;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 应用设置管理。
 * <p>
 * libgdx 重写后不再依赖 JavaFX / PreferencesFX，改用纯 {@link java.util.prefs.Preferences}
 * 持久化，设置项与原 {@code AppSettings} 保持一致，可被 UI 与引擎共同读取。
 *
 * @author unknowIfGuestInDream
 */
public final class AppConfig {

    private static final Logger log = LoggerFactory.getLogger(AppConfig.class);

    private static final String PREF_DARK_THEME = "darkTheme";
    private static final String PREF_TRACKER_ENABLED = "trackerEnabled";
    private static final String PREF_AI_LEVEL = "aiLevel";
    private static final String PREF_CHECK_UPDATE = "checkUpdateEnabled";
    private static final String PREF_LIVE_BANG = "liveBang";
    private static final String PREF_AGGRESSIVE = "aggressive";

    private static final AppConfig INSTANCE = new AppConfig();

    private final Preferences prefs;

    private boolean darkTheme;
    private boolean trackerEnabled;
    private AILevel aiLevel;
    private boolean checkUpdateEnabled;
    private boolean liveBang;
    private boolean aggressive;

    private AppConfig() {
        Preferences p;
        try {
            p = Preferences.userNodeForPackage(AppConfig.class);
        } catch (Exception e) {
            p = null;
        }
        this.prefs = p;
        this.darkTheme = getBoolean(PREF_DARK_THEME, false);
        this.trackerEnabled = getBoolean(PREF_TRACKER_ENABLED, false);
        this.checkUpdateEnabled = getBoolean(PREF_CHECK_UPDATE, true);
        this.liveBang = getBoolean(PREF_LIVE_BANG, true);
        this.aggressive = getBoolean(PREF_AGGRESSIVE, true);
        AILevel level = AILevel.MEDIUM;
        if (prefs != null) {
            try {
                level = AILevel.valueOf(prefs.get(PREF_AI_LEVEL, AILevel.MEDIUM.name()));
            } catch (IllegalArgumentException e) {
                level = AILevel.MEDIUM;
            }
        }
        this.aiLevel = level;
    }

    public static AppConfig getInstance() {
        return INSTANCE;
    }

    private boolean getBoolean(String key, boolean def) {
        return prefs != null ? prefs.getBoolean(key, def) : def;
    }

    private void flushQuietly() {
        if (prefs != null) {
            try {
                prefs.flush();
            } catch (BackingStoreException e) {
                log.warn("偏好设置保存失败", e);
            }
        }
    }

    public boolean isDarkTheme() {
        return darkTheme;
    }

    public void setDarkTheme(boolean value) {
        this.darkTheme = value;
        if (prefs != null) {
            prefs.putBoolean(PREF_DARK_THEME, value);
            flushQuietly();
        }
    }

    public boolean isTrackerEnabled() {
        return trackerEnabled;
    }

    public void setTrackerEnabled(boolean value) {
        this.trackerEnabled = value;
        if (prefs != null) {
            prefs.putBoolean(PREF_TRACKER_ENABLED, value);
            flushQuietly();
        }
    }

    public AILevel getAiLevel() {
        return aiLevel;
    }

    public void setAiLevel(AILevel value) {
        if (value == null) {
            return;
        }
        this.aiLevel = value;
        if (prefs != null) {
            prefs.put(PREF_AI_LEVEL, value.name());
            flushQuietly();
        }
    }

    public boolean isCheckUpdateEnabled() {
        return checkUpdateEnabled;
    }

    public void setCheckUpdateEnabled(boolean value) {
        this.checkUpdateEnabled = value;
        if (prefs != null) {
            prefs.putBoolean(PREF_CHECK_UPDATE, value);
            flushQuietly();
        }
    }

    public boolean isLiveBang() {
        return liveBang;
    }

    public void setLiveBang(boolean value) {
        this.liveBang = value;
        if (prefs != null) {
            prefs.putBoolean(PREF_LIVE_BANG, value);
            flushQuietly();
        }
    }

    public boolean isAggressive() {
        return aggressive;
    }

    public void setAggressive(boolean value) {
        this.aggressive = value;
        if (prefs != null) {
            prefs.putBoolean(PREF_AGGRESSIVE, value);
            flushQuietly();
        }
    }
}
