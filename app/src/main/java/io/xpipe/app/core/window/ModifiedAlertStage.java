package io.xpipe.app.core.window;

import javafx.scene.control.Alert;
import javafx.scene.control.Dialog;
import javafx.stage.Stage;
import javafx.stage.Window;
import lombok.SneakyThrows;

public class ModifiedAlertStage {

    @SneakyThrows
    public static void setForAlert(Alert alert) {
        var dialogClass = Dialog.class;
        var dialogField = dialogClass.getDeclaredField("dialog");
        dialogField.setAccessible(true);
        var dialog = (Dialog<?>) dialogField.get(alert);

        var c = Class.forName("javafx.scene.control.HeavyweightDialog");
        var positionStageMethod = c.getDeclaredMethod("positionStage");
        positionStageMethod.setAccessible(true);

        var stageField = c.getDeclaredField("stage");
        stageField.setAccessible(true);

        var m = new Stage()  {
            @SneakyThrows
            @Override public void centerOnScreen() {
                Window owner = getOwner();
                if (owner != null) {
                    positionStageMethod.invoke(dialog);
                } else {
                    if (getWidth() > 0 && getHeight() > 0) {
                        super.centerOnScreen();
                    }
                }
            }
        };

        stageField.set(alert,m);
    }
}
