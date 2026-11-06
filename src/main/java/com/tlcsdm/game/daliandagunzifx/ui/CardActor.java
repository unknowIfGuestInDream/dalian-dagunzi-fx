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

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.tlcsdm.game.daliandagunzifx.model.Card;
import com.tlcsdm.game.daliandagunzifx.model.Rank;

/**
 * 单张扑克牌的 Scene2D 渲染组件：以纯文字方式绘制牌面（花色符号 + 点数），
 * 对应原 JavaFX 版本 {@code createCardFace}/{@code createCardBack} 的渲染效果。
 *
 * @author unknowIfGuestInDream
 */
public class CardActor extends Actor {

    public static final float CARD_WIDTH = 80f;
    public static final float CARD_HEIGHT = 106f;

    private static final Color BORDER = new Color(0.6f, 0.6f, 0.6f, 1f);
    private static final GlyphLayout LAYOUT = new GlyphLayout();

    private final Assets assets;
    private final Card card;
    private boolean faceUp;
    private boolean selected;

    public CardActor(Assets assets, Card card, boolean faceUp) {
        this.assets = assets;
        this.card = card;
        this.faceUp = faceUp;
        setSize(CARD_WIDTH, CARD_HEIGHT);
    }

    public Card getCard() {
        return card;
    }

    public boolean isFaceUp() {
        return faceUp;
    }

    public void setFaceUp(boolean faceUp) {
        this.faceUp = faceUp;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        float x = getX();
        float y = getY();
        float w = getWidth();
        float h = getHeight();
        Color prev = batch.getColor().cpy();
        float a = prev.a * parentAlpha * getColor().a;

        // 边框（选中时金色高亮）
        if (selected) {
            fill(batch, x - 2, y - 2, w + 4, h + 4, Assets.GOLD, a);
        } else {
            fill(batch, x - 1, y - 1, w + 2, h + 2, BORDER, a);
        }

        if (!faceUp || card == null) {
            fill(batch, x, y, w, h, Assets.CARD_BACK, a);
            fill(batch, x + 4, y + 4, w - 8, h - 8, new Color(0.08f, 0.26f, 0.6f, 1f), a);
            batch.setColor(prev);
            return;
        }

        // 牌面白底
        fill(batch, x, y, w, h, Color.WHITE, a);

        Color textColor = colorOf(card);
        // 左上角：花色符号 + 点数（两行）
        BitmapFont corner = assets.getFontCardCorner();
        corner.setColor(textColor.r, textColor.g, textColor.b, a);
        corner.draw(batch, cornerText(card), x + 5, y + h - 5);

        // 中央大符号
        BitmapFont center = assets.getFontCardCenter();
        String centerText = centerText(card);
        center.setColor(textColor.r, textColor.g, textColor.b, a);
        LAYOUT.setText(center, centerText);
        float cx = x + (w - LAYOUT.width) / 2f;
        float cy = y + (h + LAYOUT.height) / 2f;
        center.draw(batch, centerText, cx, cy);

        batch.setColor(prev);
    }

    private void fill(Batch batch, float x, float y, float w, float h, Color color, float alpha) {
        batch.setColor(color.r, color.g, color.b, alpha);
        batch.draw(assets.getWhitePixel(), x, y, w, h);
    }

    private static String cornerText(Card card) {
        if (card.getRank() == Rank.BIG_JOKER) {
            return "大\n王";
        }
        if (card.getRank() == Rank.SMALL_JOKER) {
            return "小\n王";
        }
        String suit = card.getSuit() != null ? card.getSuit().getSymbol() : "";
        return suit + "\n" + card.getRank().getDisplayName();
    }

    private static String centerText(Card card) {
        if (card.getSuit() != null) {
            return card.getSuit().getSymbol();
        }
        return card.getRank() == Rank.BIG_JOKER ? "★" : "☆";
    }

    /** 牌面文字颜色，对应原版本 {@code getCardColor}。 */
    public static Color colorOf(Card card) {
        if (card.getRank() == Rank.BIG_JOKER) {
            return Assets.HEART_COLOR;
        }
        if (card.getRank() == Rank.SMALL_JOKER || card.getSuit() == null) {
            return Assets.JOKER_SMALL_COLOR;
        }
        return switch (card.getSuit()) {
            case SPADE -> Assets.SPADE_COLOR;
            case HEART -> Assets.HEART_COLOR;
            case DIAMOND -> Assets.DIAMOND_COLOR;
            case CLUB -> Assets.CLUB_COLOR;
        };
    }
}
