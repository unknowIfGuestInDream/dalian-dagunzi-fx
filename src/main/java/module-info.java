module com.tlcsdm.game.daliandagunzifx {
    requires javafx.controls;
    requires javafx.fxml;

    requires com.almasb.fxgl.all;

    opens com.tlcsdm.game.daliandagunzifx to javafx.fxml;
    exports com.tlcsdm.game.daliandagunzifx;
}