package io.xpipe.app.core;

import io.xpipe.app.Main;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class AppSplashScreen {

    public static void show() {
        var stage = new Stage();
        stage.setWidth(500);
        stage.setHeight(500);
        stage.setResizable(false);
        stage.initStyle(StageStyle.TRANSPARENT);

        var content = new ImageView(Main.class.getResource("resources/img/loading.gif").toString());
        var aa = Platform.isSupported(ConditionalFeature.SCENE3D)
                ? SceneAntialiasing.BALANCED
                : SceneAntialiasing.DISABLED;
        var scene = new Scene(new Pane(content), -1, -1, false, aa);
        stage.setScene(scene);
        stage.show();
    }
}
