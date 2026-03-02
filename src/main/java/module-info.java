module com.tlcsdm.game.daliandagunzifx {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.base;
    requires java.prefs;
    requires java.net.http;
    requires org.slf4j;
    requires com.dlsc.preferencesfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.ikonli.material;
    requires com.google.gson;

    opens com.tlcsdm.game.daliandagunzifx to javafx.fxml;
    exports com.tlcsdm.game.daliandagunzifx;
    exports com.tlcsdm.game.daliandagunzifx.model;
    exports com.tlcsdm.game.daliandagunzifx.engine;
    exports com.tlcsdm.game.daliandagunzifx.ai;
    exports com.tlcsdm.game.daliandagunzifx.tracker;
    exports com.tlcsdm.game.daliandagunzifx.utils;
}