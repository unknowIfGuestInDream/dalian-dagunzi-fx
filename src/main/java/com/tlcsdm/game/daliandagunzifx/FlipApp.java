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

import javafx.animation.RotateTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Application;
import javafx.scene.ParallelCamera;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * @author unknowIfGuestInDream
 */
public class FlipApp extends Application {
    private boolean dir;
    private boolean flipping;

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage stage) {
        Pane frontPane = new Pane();
        frontPane.setVisible(false);
        frontPane.setStyle("-fx-background-color: #ccccff;");
        Pane backPane = new Pane();
        backPane.setStyle("-fx-background-color: #ffff99;");
        StackPane sp = new StackPane(frontPane, backPane);
        sp.setMaxSize(66, 86);
        sp.setOnMouseClicked(e -> {
            dir = e.getX() <= sp.getWidth() / 2;
            if (backPane.isVisible()) {
                flipAnim(frontPane, backPane, dir);
            } else {
                flipAnim(backPane, frontPane, dir);
            }
        });

        Scene scene = new Scene(new BorderPane(sp), 500, 500);
        scene.setCamera(new ParallelCamera());
        stage.setScene(scene);
        stage.setTitle("GunZi");
        stage.show();
    }

    private void flipAnim(Pane showPane, Pane hidePane, boolean dir) {
        if (flipping) {
            return;
        }
        flipping = true;
        Duration duration = Duration.millis(500);
        RotateTransition hideRt = new RotateTransition(duration, hidePane);
        hideRt.setAxis(Rotate.Y_AXIS);
        hideRt.setFromAngle(0);
        hideRt.setToAngle(dir ? 90 : -90);
        hideRt.setOnFinished(e -> {
            hidePane.setVisible(false);
            showPane.setVisible(true);
        });

        RotateTransition showRt = new RotateTransition(duration, showPane);
        showRt.setAxis(Rotate.Y_AXIS);
        showRt.setFromAngle(dir ? -90 : 90);
        showRt.setToAngle(0);

        SequentialTransition st = new SequentialTransition(hideRt, showRt);
        st.setOnFinished(e -> flipping = false);
        st.play();
    }
}
