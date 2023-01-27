package io.xpipe.app.comp.base;

import com.jfoenix.controls.JFXSpinner;
import io.xpipe.extension.fxcomps.Comp;
import io.xpipe.extension.fxcomps.CompStructure;
import io.xpipe.extension.fxcomps.SimpleCompStructure;
import io.xpipe.extension.fxcomps.util.PlatformThread;
import io.xpipe.extension.util.ThreadHelper;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.layout.StackPane;

public class LoadingOverlayComp extends Comp<CompStructure<StackPane>> {

    private final Comp<?> comp;
    private final ObservableValue<Boolean> showLoading;

    public LoadingOverlayComp(Comp<?> comp, ObservableValue<Boolean> loading) {
        this.comp = comp;
        this.showLoading = PlatformThread.sync(loading);
    }

    @Override
    public CompStructure<StackPane> createBase() {
        var compStruc = comp.createStructure();

        JFXSpinner loading = new JFXSpinner();
        loading.getStyleClass().add("spinner");
        var loadingBg = new StackPane(loading);
        loadingBg.getStyleClass().add("loading-comp");

        loadingBg.setVisible(showLoading.getValue());
        ;
        var listener = new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean busy) {
                if (!busy) {
                    // Reduce flickering for consecutive loads
                    Thread t = new Thread(() -> {
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException ignored) {
                        }

                        if (!showLoading.getValue()) {
                            Platform.runLater(() -> loadingBg.setVisible(false));
                        }
                    });
                    t.setDaemon(true);
                    t.setName("loading delay");
                    t.start();
                } else {
                    ThreadHelper.runAsync(() -> {
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException ignored) {
                        }

                        if (showLoading.getValue()) {
                            Platform.runLater(() -> loadingBg.setVisible(true));
                        }
                    });
                }
            }
        };
        showLoading.addListener(listener);

        var stack = new StackPane(compStruc.get(), loadingBg);
        return new SimpleCompStructure<>(stack);
    }
}
