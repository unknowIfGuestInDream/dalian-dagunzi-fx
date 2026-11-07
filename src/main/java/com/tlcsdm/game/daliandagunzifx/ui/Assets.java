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

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Disposable;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 全局资源管理：字体、纯色纹理、Scene2D 样式与颜色常量。
 * <p>
 * 中文 UI 依赖 TrueType 字体，运行时按以下顺序解析字体文件：
 * <ol>
 *     <li>内置字体 {@code fonts/ui.ttf}（存在时优先，便于离线分发）；</li>
 *     <li>扫描常见系统中文字体路径（Windows / macOS / Linux）；</li>
 *     <li>以上均不可用时回退 libgdx 默认 {@link BitmapFont}（仅 ASCII，中文将无法显示）。</li>
 * </ol>
 *
 * @author unknowIfGuestInDream
 */
public final class Assets implements Disposable {

    private static final Logger log = LoggerFactory.getLogger(Assets.class);

    private FreeTypeFontGenerator generator;

    private BitmapFont fontSmall;
    private BitmapFont fontMedium;
    private BitmapFont fontLarge;
    private BitmapFont fontTitle;
    private BitmapFont fontCardCorner;
    private BitmapFont fontCardCenter;

    private Texture whitePixel;

    private Label.LabelStyle labelStyle;
    private Label.LabelStyle labelStyleSmall;
    private Label.LabelStyle titleStyle;
    private TextButton.TextButtonStyle buttonStyle;
    private TextButton.TextButtonStyle dangerButtonStyle;
    private Window.WindowStyle windowStyle;
    private TextField.TextFieldStyle textFieldStyle;

    // ======================== 颜色常量（对应原 JavaFX 版本）========================
    public static final Color TABLE_LIGHT = rgb(0x2d8a4e);
    public static final Color TABLE_DARK = rgb(0x1a1a2e);
    public static final Color SPADE_COLOR = rgb(0x1a1a1a);
    public static final Color HEART_COLOR = rgb(0xcc0000);
    public static final Color DIAMOND_COLOR = rgb(0x0066bb);
    public static final Color CLUB_COLOR = rgb(0x228833);
    public static final Color JOKER_SMALL_COLOR = rgb(0x333333);
    public static final Color GOLD = rgb(0xffd700);
    public static final Color GOLD_BTN = rgb(0xd4af37);
    public static final Color CARD_BACK = rgb(0x2255aa);
    public static final Color STATUS = rgb(0xffdd57);
    public static final Color PANEL_BG = new Color(0f, 0f, 0f, 0.3f);

    public void load() {
        whitePixel = createWhitePixel();

        FileHandle fontFile = resolveFontFile();
        if (fontFile != null) {
            try {
                generator = new FreeTypeFontGenerator(fontFile);
                fontSmall = genFont(14);
                fontMedium = genFont(18);
                fontLarge = genFont(28);
                fontTitle = genFont(48);
                fontCardCorner = genFont(15);
                fontCardCenter = genFont(30);
                log.info("已加载中文字体：{}", fontFile.path());
            } catch (Exception e) {
                log.warn("TrueType 字体加载失败，回退默认位图字体（中文可能无法显示）", e);
            }
        } else {
            log.warn("未找到可用的中文 TrueType 字体，回退默认位图字体（中文可能无法显示）");
        }

        if (fontSmall == null) {
            fontSmall = new BitmapFont();
            fontMedium = new BitmapFont();
            fontLarge = new BitmapFont();
            fontTitle = new BitmapFont();
            fontCardCorner = new BitmapFont();
            fontCardCenter = new BitmapFont();
            fontLarge.getData().setScale(1.6f);
            fontTitle.getData().setScale(2.4f);
            fontCardCenter.getData().setScale(1.6f);
        }

        buildStyles();
    }

    private BitmapFont genFont(int size) {
        FreeTypeFontGenerator.FreeTypeFontParameter param = new FreeTypeFontGenerator.FreeTypeFontParameter();
        param.size = size;
        // 增量模式：仅预生成 ASCII，其余字符（全部中文与 ♠♥♦♣ 等花色/符号）在首次渲染时按需生成字形，
        // 避免使用固定字符集时因遗漏字符而显示为方块（豆腐块）。
        param.incremental = true;
        param.characters = FreeTypeFontGenerator.DEFAULT_CHARS;
        param.minFilter = Texture.TextureFilter.Linear;
        param.magFilter = Texture.TextureFilter.Linear;
        return generator.generateFont(param);
    }

    /**
     * 解析可用字体文件：优先内置 {@code fonts/ui.ttf}，否则扫描系统中文字体。
     */
    private FileHandle resolveFontFile() {
        FileHandle bundled = Gdx.files.internal("fonts/ui.ttf");
        if (bundled.exists()) {
            return bundled;
        }
        String[] candidates = {
            // Windows
            "C:/Windows/Fonts/msyh.ttc", "C:/Windows/Fonts/msyh.ttf",
            "C:/Windows/Fonts/simhei.ttf", "C:/Windows/Fonts/simsun.ttc",
            // macOS
            "/System/Library/Fonts/PingFang.ttc", "/System/Library/Fonts/STHeiti Light.ttc",
            "/Library/Fonts/Arial Unicode.ttf",
            // Linux（文泉驿 / Noto / 思源黑体）
            "/usr/share/fonts/truetype/wqy/wqy-microhei.ttc",
            "/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc",
            "/usr/share/fonts/wenquanyi/wqy-microhei/wqy-microhei.ttc",
            "/usr/share/fonts/wenquanyi/wqy-zenhei/wqy-zenhei.ttc",
            "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc",
            "/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc",
            "/usr/share/fonts/truetype/arphic/uming.ttc",
            "/usr/share/fonts/truetype/droid/DroidSansFallbackFull.ttf"
        };
        for (String path : candidates) {
            File f = new File(path);
            if (f.exists()) {
                return Gdx.files.absolute(path);
            }
        }
        return null;
    }

    private void buildStyles() {
        labelStyle = new Label.LabelStyle(fontMedium, Color.WHITE);
        labelStyleSmall = new Label.LabelStyle(fontSmall, Color.WHITE);
        titleStyle = new Label.LabelStyle(fontTitle, GOLD);

        buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.font = fontMedium;
        buttonStyle.fontColor = Color.BLACK;
        buttonStyle.up = drawable(GOLD_BTN);
        buttonStyle.down = drawable(rgb(0xb8962d));
        buttonStyle.over = drawable(rgb(0xe5c040));
        buttonStyle.disabled = drawable(rgb(0x888888));
        buttonStyle.disabledFontColor = rgb(0xdddddd);

        dangerButtonStyle = new TextButton.TextButtonStyle();
        dangerButtonStyle.font = fontMedium;
        dangerButtonStyle.fontColor = Color.WHITE;
        dangerButtonStyle.up = drawable(rgb(0xcc0000));
        dangerButtonStyle.down = drawable(rgb(0x990000));
        dangerButtonStyle.over = drawable(rgb(0xdd2222));
        dangerButtonStyle.disabled = drawable(rgb(0x888888));
        dangerButtonStyle.disabledFontColor = rgb(0xdddddd);

        windowStyle = new Window.WindowStyle();
        windowStyle.titleFont = fontMedium;
        windowStyle.titleFontColor = GOLD;
        windowStyle.background = drawable(new Color(0.12f, 0.12f, 0.18f, 0.96f));

        textFieldStyle = new TextField.TextFieldStyle();
        textFieldStyle.font = fontMedium;
        textFieldStyle.fontColor = Color.WHITE;
        textFieldStyle.background = drawable(new Color(1f, 1f, 1f, 0.12f));
        textFieldStyle.cursor = drawable(Color.WHITE);
        textFieldStyle.selection = drawable(new Color(0.3f, 0.5f, 0.9f, 0.5f));
    }

    private Texture createWhitePixel() {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return texture;
    }

    /** 以指定颜色着色的 1x1 纹理 Drawable，用于填充矩形背景。 */
    public Drawable drawable(Color color) {
        TextureRegionDrawable d = new TextureRegionDrawable(whitePixel);
        return d.tint(color);
    }

    public Texture getWhitePixel() {
        return whitePixel;
    }

    public BitmapFont getFontSmall() {
        return fontSmall;
    }

    public BitmapFont getFontMedium() {
        return fontMedium;
    }

    public BitmapFont getFontLarge() {
        return fontLarge;
    }

    public BitmapFont getFontTitle() {
        return fontTitle;
    }

    public BitmapFont getFontCardCorner() {
        return fontCardCorner;
    }

    public BitmapFont getFontCardCenter() {
        return fontCardCenter;
    }

    public Label.LabelStyle getLabelStyle() {
        return labelStyle;
    }

    public Label.LabelStyle getLabelStyleSmall() {
        return labelStyleSmall;
    }

    public Label.LabelStyle getTitleStyle() {
        return titleStyle;
    }

    public TextButton.TextButtonStyle getButtonStyle() {
        return buttonStyle;
    }

    public TextButton.TextButtonStyle getDangerButtonStyle() {
        return dangerButtonStyle;
    }

    public Window.WindowStyle getWindowStyle() {
        return windowStyle;
    }

    public TextField.TextFieldStyle getTextFieldStyle() {
        return textFieldStyle;
    }

    private static Color rgb(int hex) {
        return new Color(
            ((hex >> 16) & 0xff) / 255f,
            ((hex >> 8) & 0xff) / 255f,
            (hex & 0xff) / 255f,
            1f);
    }

    @Override
    public void dispose() {
        // 增量字体在渲染期间持有 generator 的惰性引用，须先释放字体（含其 PixmapPacker 纹理），再释放 generator。
        disposeFont(fontSmall);
        disposeFont(fontMedium);
        disposeFont(fontLarge);
        disposeFont(fontTitle);
        disposeFont(fontCardCorner);
        disposeFont(fontCardCenter);
        if (generator != null) {
            generator.dispose();
        }
        if (whitePixel != null) {
            whitePixel.dispose();
        }
    }

    private void disposeFont(BitmapFont font) {
        if (font != null) {
            font.dispose();
        }
    }
}
