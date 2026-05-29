package io.xpipe.app.comp.base;

import io.xpipe.app.comp.RegionBuilder;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.app.util.FailableSupplier;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.layout.Region;

import atlantafx.base.theme.Styles;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.concurrent.atomic.AtomicReference;

@Getter
@AllArgsConstructor
public class TestButtonComp extends RegionBuilder<Button> {

    private final FailableSupplier<Boolean> run;

    @Override
    public Button createSimple() {
        AtomicReference<Region> button = new AtomicReference<>();
        var testButton = new ButtonComp(AppI18n.observable("test"), new FontIcon("mdi2p-play"), () -> {
            ThreadHelper.runAsync(() -> {
                Platform.runLater(() -> {
                    button.get().getStyleClass().removeAll(Styles.SUCCESS, Styles.DANGER, Styles.ACCENT);
                    button.get().getStyleClass().add(Styles.ACCENT);
                    button.get().setDisable(true);
                });
                try {
                    boolean r;
                    try {
                        r = run.get();
                    } finally {
                        Platform.runLater(() -> {
                            button.get().setDisable(false);
                            button.get().getStyleClass().removeAll(Styles.SUCCESS, Styles.DANGER, Styles.ACCENT);
                        });
                    }
                    Platform.runLater(() -> {
                        if (r) {
                            button.get().getStyleClass().add(Styles.SUCCESS);
                        } else {
                            button.get().getStyleClass().add(Styles.DANGER);
                        }
                    });
                } catch (Throwable e) {
                    Platform.runLater(() -> {
                        button.get().getStyleClass().add(Styles.DANGER);
                    });
                    ErrorEventFactory.fromThrowable(e).expected().handle();
                }
            });
        });
        testButton.apply(struc -> button.set(struc));
        testButton.padding(new Insets(6, 10, 6, 6));
        return testButton.build();
    }
}
