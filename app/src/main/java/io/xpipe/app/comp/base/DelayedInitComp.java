package io.xpipe.app.comp.base;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.util.GlobalTimer;

import javafx.application.Platform;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

import lombok.AllArgsConstructor;

import java.time.Duration;
import java.util.function.Supplier;

@AllArgsConstructor
public class DelayedInitComp extends SimpleComp {

    private final Comp<?> comp;
    private final Supplier<Boolean> condition;

    @Override
    protected Region createSimple() {
        var stack = new StackPane();
        GlobalTimer.scheduleUntil(Duration.ofMillis(10), () -> {
            if (!condition.get()) {
                return false;
            }

            Platform.runLater(() -> {
                var r = comp.createRegion();
                stack.getChildren().add(r);
            });
            return true;
        });
        return stack;
    }
}
