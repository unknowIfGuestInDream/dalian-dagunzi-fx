module com.tlcsdm.game.daliandagunzifx {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.base;
    requires com.almasb.fxgl.core;
    requires com.almasb.fxgl.entity;
    requires com.almasb.fxgl.io;
    requires com.almasb.fxgl.gameplay;
    requires com.almasb.fxgl.scene;

    opens com.tlcsdm.game.daliandagunzifx to javafx.fxml;
    exports com.tlcsdm.game.daliandagunzifx;
    exports com.tlcsdm.game.daliandagunzifx.model;
    exports com.tlcsdm.game.daliandagunzifx.engine;
    exports com.tlcsdm.game.daliandagunzifx.ai;
    exports com.tlcsdm.game.daliandagunzifx.tracker;
}