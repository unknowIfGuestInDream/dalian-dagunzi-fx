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
package com.tlcsdm.game.daliandagunzifx.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.util.prefs.Preferences;

/**
 * 检查更新工具类，通过 GitHub Release API 获取最新版本信息。
 * 支持每日自动检查（避免频繁请求）和手动触发检查。
 *
 * @author unknowIfGuestInDream
 */
public final class UpdateChecker {

    private static final Logger log = LoggerFactory.getLogger(UpdateChecker.class);

    private static final String GITHUB_API_URL =
        "https://api.github.com/repos/unknowIfGuestInDream/dalian-dagunzi-fx/releases/latest";
    private static final String RELEASES_PAGE_URL =
        "https://github.com/unknowIfGuestInDream/dalian-dagunzi-fx/releases";
    private static final String PREF_LAST_CHECK_DATE = "lastUpdateCheckDate";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private UpdateChecker() {
    }

    /**
     * 更新信息记录。
     *
     * @param latestVersion 最新版本号
     * @param releaseNotes  发布说明
     * @param downloadUrl   下载页面 URL
     */
    public record UpdateInfo(String latestVersion, String releaseNotes, String downloadUrl) {
    }

    /**
     * 判断今天是否已经检查过更新。
     */
    public static boolean hasCheckedToday() {
        Preferences prefs = getPreferences();
        if (prefs == null) {
            return false;
        }
        String lastCheck = prefs.get(PREF_LAST_CHECK_DATE, "");
        return LocalDate.now().toString().equals(lastCheck);
    }

    /**
     * 记录今天已检查过更新。
     */
    public static void markCheckedToday() {
        Preferences prefs = getPreferences();
        if (prefs != null) {
            prefs.put(PREF_LAST_CHECK_DATE, LocalDate.now().toString());
        }
    }

    /**
     * 查询 GitHub 最新 Release 版本信息。
     *
     * @param currentVersion 当前应用版本
     * @return 若有新版本，返回 UpdateInfo；否则返回 null
     */
    public static UpdateInfo checkForUpdate(String currentVersion) {
        try {
            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(REQUEST_TIMEOUT)
                .build();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GITHUB_API_URL))
                .header("Accept", "application/vnd.github.v3+json")
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.warn("检查更新失败，HTTP 状态码：{}", response.statusCode());
                return null;
            }

            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            String tagName = json.get("tag_name").getAsString();
            String latestVersion = tagName.startsWith("v") ? tagName.substring(1) : tagName;
            String body = json.has("body") && !json.get("body").isJsonNull()
                ? json.get("body").getAsString() : "";
            String htmlUrl = json.has("html_url") && !json.get("html_url").isJsonNull()
                ? json.get("html_url").getAsString() : RELEASES_PAGE_URL;

            if (isNewerVersion(latestVersion, currentVersion)) {
                return new UpdateInfo(latestVersion, body, htmlUrl);
            }
            return null;
        } catch (IOException | InterruptedException e) {
            log.warn("检查更新时发生异常", e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return null;
        }
    }

    /**
     * 获取 GitHub Releases 页面地址。
     */
    public static String getReleasesPageUrl() {
        return RELEASES_PAGE_URL;
    }

    /**
     * 比较两个版本号，判断 newer 是否比 current 更新。
     * 支持语义化版本格式（如 1.0.0、1.2.3）。
     */
    static boolean isNewerVersion(String newer, String current) {
        if (newer == null || current == null) {
            return false;
        }
        String[] newerParts = newer.split("\\.");
        String[] currentParts = current.split("\\.");
        int length = Math.max(newerParts.length, currentParts.length);
        for (int i = 0; i < length; i++) {
            int n = i < newerParts.length ? parseIntSafe(newerParts[i]) : 0;
            int c = i < currentParts.length ? parseIntSafe(currentParts[i]) : 0;
            if (n > c) {
                return true;
            }
            if (n < c) {
                return false;
            }
        }
        return false;
    }

    private static int parseIntSafe(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static Preferences getPreferences() {
        try {
            return Preferences.userNodeForPackage(UpdateChecker.class);
        } catch (Exception e) {
            return null;
        }
    }
}
