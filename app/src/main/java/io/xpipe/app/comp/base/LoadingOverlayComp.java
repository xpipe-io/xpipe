package io.xpipe.app.comp.base;

import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.CompStructure;
import io.xpipe.app.fxcomps.SimpleCompStructure;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.util.Indicator;
import io.xpipe.app.util.ThreadHelper;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;
import org.reactfx.EventStream;
import org.reactfx.EventStreams;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class LoadingOverlayComp extends Comp<CompStructure<StackPane>> {

    public static LoadingOverlayComp noProgress(Comp<?> comp, ObservableValue<Boolean> loading) {
        return new LoadingOverlayComp(comp, loading, new SimpleDoubleProperty(50));
    }

    private final Comp<?> comp;
    private final ObservableValue<Boolean> showLoading;
    private final ObservableValue<Number> progress;

    public LoadingOverlayComp(Comp<?> comp, ObservableValue<Boolean> loading, ObservableValue<Number> progress) {
        this.comp = comp;
        this.showLoading = PlatformThread.sync(loading);
        this.progress = PlatformThread.sync(progress);
    }

    private static final double FPS = 30.0;
    private static final double cycleDurationSeconds = 4.0;

    @Override
    public CompStructure<StackPane> createBase() {
        var compStruc = comp.createStructure();
        var r = compStruc.get();

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        EventStream<?> ticks = EventStreams.ticks(Duration.ofMillis((long) (1000 / FPS)), scheduler, Platform::runLater);


        var pane = new StackPane();
        Parent node = new Indicator(ticks, (int) (FPS * cycleDurationSeconds)).getNode();
        pane.getChildren().add(node);
        pane.setAlignment(Pos.CENTER);

        var loadingOverlay = new StackPane(pane);
        loadingOverlay.getStyleClass().add("loading-comp");
        loadingOverlay.setVisible(showLoading.getValue());

        var listener = new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean busy) {
                if (!busy) {
                    // Reduce flickering for consecutive loads
                    ThreadHelper.runAsync(() -> {
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException ignored) {
                        }

                        if (!showLoading.getValue()) {
                            Platform.runLater(() -> loadingOverlay.setVisible(false));
                        }
                    });
                } else {
                    ThreadHelper.runAsync(() -> {
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException ignored) {
                        }

                        if (showLoading.getValue()) {
                            Platform.runLater(() -> loadingOverlay.setVisible(true));
                        }
                    });
                }
            }
        };
        showLoading.addListener(listener);

        var stack = new StackPane(r, loadingOverlay);

        pane.prefWidthProperty()
                .bind(Bindings.createDoubleBinding(
                        () -> {
                            return Math.min(r.getHeight() - 20, 50);
                        },
                        r.heightProperty()));
        pane.prefHeightProperty().bind(pane.prefWidthProperty());

        return new SimpleCompStructure<>(stack);
    }
}
