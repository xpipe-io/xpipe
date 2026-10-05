package io.xpipe.app.core;

import io.xpipe.app.core.window.AppMainWindow;

import io.xpipe.app.platform.GlobalBooleanProperty;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableBooleanValue;

public class AppSizeBreakpoints {

    private static final BooleanProperty compactMode = new GlobalBooleanProperty();
    private static final BooleanProperty portraitMode = new GlobalBooleanProperty();

    public static ObservableBooleanValue compactMode() {
        return compactMode;
    }

    public static ObservableBooleanValue portraitMode() {
        return portraitMode;
    }

    public static void init() {
        compactMode.bind(Bindings.createBooleanBinding(
                () -> {
                    return AppMainWindow.get().getStage().getWidth() <= 1000;
                },
                AppMainWindow.get().getStage().widthProperty()));
        portraitMode.bind(Bindings.createBooleanBinding(
                () -> {
                    return AppMainWindow.get().getStage().getWidth() <= 700;
                },
                AppMainWindow.get().getStage().widthProperty()));
    }
}
