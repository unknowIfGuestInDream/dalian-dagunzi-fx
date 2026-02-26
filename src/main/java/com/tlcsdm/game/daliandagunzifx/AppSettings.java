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
import java.util.prefs.Preferences;

/**
 * Application settings management using PreferencesFX.
 *
 * @author unknowIfGuestInDream
 */
public final class AppSettings {

    private static final AppSettings INSTANCE = new AppSettings();

    private static final Preferences PREFS = Preferences.userNodeForPackage(AppSettings.class);
    private static final String PREF_DARK_THEME = "darkTheme";
    private static final String PREF_TRACKER_ENABLED = "trackerEnabled";
    private static final String PREF_AI_LEVEL = "aiLevel";

    private final BooleanProperty darkThemeProperty;
    private final BooleanProperty trackerEnabledProperty;
    private final ObjectProperty<AILevel> aiLevelProperty;

    private PreferencesFx preferencesFx;

    private AppSettings() {
        darkThemeProperty = new SimpleBooleanProperty(PREFS.getBoolean(PREF_DARK_THEME, false));
        trackerEnabledProperty = new SimpleBooleanProperty(PREFS.getBoolean(PREF_TRACKER_ENABLED, false));
        String savedLevel = PREFS.get(PREF_AI_LEVEL, AILevel.MEDIUM.name());
        AILevel level;
        try {
            level = AILevel.valueOf(savedLevel);
        } catch (IllegalArgumentException e) {
            level = AILevel.MEDIUM;
        }
        aiLevelProperty = new SimpleObjectProperty<>(level);

        darkThemeProperty.addListener((obs, oldVal, newVal) ->
            PREFS.putBoolean(PREF_DARK_THEME, newVal));
        trackerEnabledProperty.addListener((obs, oldVal, newVal) ->
            PREFS.putBoolean(PREF_TRACKER_ENABLED, newVal));
        aiLevelProperty.addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                PREFS.put(PREF_AI_LEVEL, newVal.name());
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

    /**
     * Get the PreferencesFx instance. Creates it on first call.
     */
    public PreferencesFx getPreferencesFx() {
        if (preferencesFx == null) {
            buildPreferences();
        }
        return preferencesFx;
    }

    private void buildPreferences() {
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
                )
            )
        ).persistWindowState(true)
         .saveSettings(true)
         .debugHistoryMode(false)
         .buttonsVisibility(false)
         .instantPersistent(true);
    }
}
