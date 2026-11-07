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

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link UiUtils} 的单元测试，重点验证点击回调为 {@code null} 时不会在点击时抛出空指针。
 *
 * @author unknowIfGuestInDream
 */
class UiUtilsTest {

    /**
     * 回归测试：第二局叫主阶段曾用 {@code button(text, style, null)} 预创建按钮，
     * 点击时 {@code action.run()} 因 action 为 null 抛 NPE 导致程序退出。
     * 现要求 {@link UiUtils#onClick(Actor, Runnable)} 在 action 为 null 时直接跳过绑定，
     * 不注册任何会崩溃的监听器。
     */
    @Test
    void testOnClickWithNullActionRegistersNoListener() {
        Actor actor = new Actor();
        assertDoesNotThrow(() -> UiUtils.onClick(actor, null),
            "action 为 null 时绑定点击回调不应抛异常");
        assertEquals(0, actor.getListeners().size,
            "action 为 null 时不应注册任何点击监听器，避免点击时空指针崩溃");
    }

    /**
     * action 非 null 时应正常注册监听器，且触发点击能执行回调。
     */
    @Test
    void testOnClickWithActionRunsCallback() {
        Actor actor = new Actor();
        boolean[] fired = {false};
        UiUtils.onClick(actor, () -> fired[0] = true);

        assertEquals(1, actor.getListeners().size, "action 非 null 时应注册一个点击监听器");
        assertInstanceOf(ClickListener.class, actor.getListeners().get(0));

        ClickListener listener = (ClickListener) actor.getListeners().get(0);
        listener.clicked(null, 0f, 0f);
        assertTrue(fired[0], "触发点击应执行传入的回调");
    }
}
