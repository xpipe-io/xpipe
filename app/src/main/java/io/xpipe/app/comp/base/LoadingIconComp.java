package io.xpipe.app.comp.base;

import atlantafx.base.controls.RingProgressIndicator;
import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.CompStructure;
import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.comp.SimpleCompStructure;
import io.xpipe.app.core.AppFontSizes;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.util.PlatformThread;
import io.xpipe.app.util.ThreadHelper;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

import java.util.function.Consumer;
import java.util.function.Function;

public class LoadingIconComp extends SimpleComp {

    private static final char[] chars = new char[] {'⠋', '⠙', '⠹', '⠸', '⠼', '⠴', '⠦', '⠧', '⠇', '⠏'};

    private final ObservableValue<Boolean> show;
    private final Consumer<Node> fontSize;

    public LoadingIconComp(ObservableValue<Boolean> show, Consumer<Node> fontSize) {this.show = show;
        this.fontSize = fontSize;
    }

    @Override
    protected Region createSimple() {
        var label = new Label();
        fontSize.accept(label);
        label.setAlignment(Pos.CENTER);
        label.setText(Character.toString(chars[0]));
        label.getStyleClass().add("loading-icon-comp");

        label.setPrefWidth(16);
        label.setPrefHeight(16);

        var timer = new AnimationTimer() {

            long init = 0;
            int index = 0;

            @Override
            public void handle(long now) {
                if (init == 0) {
                    init = now;
                }

                var nowMs = now;
                if ((nowMs - init) > 250 * 1_000_000L) {
                    label.setText(Character.toString(chars[index]));
                    init = nowMs;
                    index = (index + 1) % chars.length;
                }
            }
        };

        show.subscribe(val -> {
            PlatformThread.runLaterIfNeeded(() -> {
                label.setVisible(val);
                if (val) {
                    timer.start();
                } else {
                    timer.stop();
                }
            });
        });

        return label;
    }
}
