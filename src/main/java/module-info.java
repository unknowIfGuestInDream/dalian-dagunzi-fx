module com.tlcsdm.game.daliandagunzifx {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.almasb.fxgl.core;
    requires com.almasb.fxgl.entity;
    requires com.almasb.fxgl.io;
    requires com.almasb.fxgl.gameplay;
    requires com.almasb.fxgl.scene;

    opens com.tlcsdm.game.daliandagunzifx to javafx.fxml;
    exports com.tlcsdm.game.daliandagunzifx;
}