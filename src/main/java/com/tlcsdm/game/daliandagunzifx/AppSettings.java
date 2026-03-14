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

import com.dlsc.preferencesfx.PreferencesFx;
import com.dlsc.preferencesfx.model.Category;
import com.dlsc.preferencesfx.model.Group;
import com.dlsc.preferencesfx.model.Setting;
import com.tlcsdm.game.daliandagunzifx.ai.AILevel;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;

import java.util.Arrays;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Application settings management using PreferencesFX.
 *
 * @author unknowIfGuestInDream
 */
public final class AppSettings {

    private static final Logger log = LoggerFactory.getLogger(AppSettings.class);

    private static final Preferences PREFS = initPreferences();
    private static final String PREF_DARK_THEME = "darkTheme";
    private static final String PREF_TRACKER_ENABLED = "trackerEnabled";
    private static final String PREF_AI_LEVEL = "aiLevel";
    private static final String PREF_CHECK_UPDATE = "checkUpdateEnabled";

    private static final AppSettings INSTANCE = new AppSettings();

    private final BooleanProperty darkThemeProperty;
    private final BooleanProperty trackerEnabledProperty;
    private final ObjectProperty<AILevel> aiLevelProperty;
    private final BooleanProperty checkUpdateEnabledProperty;

    private PreferencesFx preferencesFx;

    private static Preferences initPreferences() {
        try {
            return Preferences.userNodeForPackage(AppSettings.class);
        } catch (Exception e) {
            return null;
        }
    }

    private AppSettings() {
        darkThemeProperty = new SimpleBooleanProperty(
            PREFS != null ? PREFS.getBoolean(PREF_DARK_THEME, false) : false);
        trackerEnabledProperty = new SimpleBooleanProperty(
            PREFS != null ? PREFS.getBoolean(PREF_TRACKER_ENABLED, false) : false);
        String savedLevel = PREFS != null ? PREFS.get(PREF_AI_LEVEL, AILevel.MEDIUM.name()) : AILevel.MEDIUM.name();
        AILevel level;
        try {
            level = AILevel.valueOf(savedLevel);
        } catch (IllegalArgumentException e) {
            level = AILevel.MEDIUM;
        }
        aiLevelProperty = new SimpleObjectProperty<>(level);
        checkUpdateEnabledProperty = new SimpleBooleanProperty(
            PREFS != null ? PREFS.getBoolean(PREF_CHECK_UPDATE, true) : true);

        darkThemeProperty.addListener((obs, oldVal, newVal) -> {
            if (PREFS != null) {
                PREFS.putBoolean(PREF_DARK_THEME, newVal);
                flushQuietly();
            }
        });
        trackerEnabledProperty.addListener((obs, oldVal, newVal) -> {
            if (PREFS != null) {
                PREFS.putBoolean(PREF_TRACKER_ENABLED, newVal);
                flushQuietly();
            }
        });
        aiLevelProperty.addListener((obs, oldVal, newVal) -> {
            if (newVal != null && PREFS != null) {
                PREFS.put(PREF_AI_LEVEL, newVal.name());
                flushQuietly();
            }
        });
        checkUpdateEnabledProperty.addListener((obs, oldVal, newVal) -> {
            if (PREFS != null) {
                PREFS.putBoolean(PREF_CHECK_UPDATE, newVal);
                flushQuietly();
            }
        });
    }

    public static AppSettings getInstance() {
        return INSTANCE;
    }

    public BooleanProperty darkThemeProperty() {
        return darkThemeProperty;
    }

    public boolean isDarkTheme() {
        return darkThemeProperty.get();
    }

    public BooleanProperty trackerEnabledProperty() {
        return trackerEnabledProperty;
    }

    public boolean isTrackerEnabled() {
        return trackerEnabledProperty.get();
    }

    public ObjectProperty<AILevel> aiLevelProperty() {
        return aiLevelProperty;
    }

    public AILevel getAiLevel() {
        return aiLevelProperty.get();
    }

    public BooleanProperty checkUpdateEnabledProperty() {
        return checkUpdateEnabledProperty;
    }

    public boolean isCheckUpdateEnabled() {
        return checkUpdateEnabledProperty.get();
    }

    /**
     * Get the PreferencesFx instance. Creates it on first call.
     */
    public PreferencesFx getPreferencesFx() {
        if (preferencesFx == null) {
            buildPreferences();
        }
        return preferencesFx;
    }

    private static void flushQuietly() {
        if (PREFS != null) {
            try {
                PREFS.flush();
            } catch (BackingStoreException e) {
                log.warn("偏好设置保存失败", e);
            }
        }
    }

    private void buildPreferences() {
        // 保存当前属性值，防止 PreferencesFx 初始化时用内部空存储覆盖已从 PREFS 加载的属性值。
        // 注意：如果新增属性，必须在此处同步添加保存和恢复逻辑，否则新属性可能在打开偏好设置后被重置。
        boolean savedDark = darkThemeProperty.get();
        boolean savedTracker = trackerEnabledProperty.get();
        AILevel savedLevel = aiLevelProperty.get();
        boolean savedUpdate = checkUpdateEnabledProperty.get();

        preferencesFx = PreferencesFx.of(AppSettings.class,
            Category.of("游戏设置",
                Group.of("显示",
                    Setting.of("深色主题", darkThemeProperty)
                ),
                Group.of("游戏",
                    Setting.of("开启记牌器", trackerEnabledProperty),
                    Setting.of("AI难度",
                        FXCollections.observableArrayList(Arrays.asList(AILevel.values())),
                        aiLevelProperty)
                ),
                Group.of("更新",
                    Setting.of("启动时检查更新", checkUpdateEnabledProperty)
                )
            )
        ).persistWindowState(false)
         // 禁用 PreferencesFx 自动保存，使用手动 Preferences API 作为唯一持久化机制，避免设置被覆盖
         .saveSettings(false)
         .debugHistoryMode(false)
         .buttonsVisibility(false)
         .instantPersistent(true)
         .dialogIcon(new javafx.scene.image.Image(
             getClass().getResourceAsStream("logo.png")));

        // 恢复属性值：PreferencesFx 的 instantPersistent(true) + saveSettings(false) 会在创建时
        // 从内部空存储加载默认值并覆盖属性，此处恢复确保手动 Preferences 中的值不会丢失
        darkThemeProperty.set(savedDark);
        trackerEnabledProperty.set(savedTracker);
        aiLevelProperty.set(savedLevel);
        checkUpdateEnabledProperty.set(savedUpdate);
    }
}
