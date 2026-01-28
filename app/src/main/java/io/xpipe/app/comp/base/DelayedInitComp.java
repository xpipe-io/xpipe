package io.xpipe.app.comp.base;

import io.xpipe.app.comp.BaseRegionBuilder;
import io.xpipe.app.comp.SimpleRegionBuilder;
import io.xpipe.app.util.GlobalTimer;

import javafx.application.Platform;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

import lombok.AllArgsConstructor;

import java.time.Duration;
import java.util.function.Supplier;

@AllArgsConstructor
public class DelayedInitComp extends SimpleRegionBuilder {

    private final BaseRegionBuilder<?, ?> comp;
    private final Supplier<Boolean> condition;

    @Override
    protected Region createSimple() {
        var stack = new StackPane();
        GlobalTimer.scheduleUntil(Duration.ofMillis(10), true, () -> {
            if (!condition.get()) {
                return false;
            }

            Platform.runLater(() -> {
                var r = comp.build();
                stack.getChildren().add(r);
            });
            return true;
        });
        stack.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue && !stack.getChildren().isEmpty()) {
                stack.getChildren().getFirst().requestFocus();
            }
        });
        return stack;
    }
}
