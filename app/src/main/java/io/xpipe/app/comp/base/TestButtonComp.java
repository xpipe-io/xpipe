package io.xpipe.app.comp.base;

import io.xpipe.app.comp.RegionBuilder;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.FailableSupplier;

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
                try {
                    var r = run.get();
                    Platform.runLater(() -> {
                        button.get().getStyleClass().removeAll(Styles.SUCCESS, Styles.DANGER);
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
                }
            });
        });
        testButton.apply(struc -> button.set(struc));
        testButton.padding(new Insets(6, 10, 6, 6));
        return testButton.build();
    }
}
